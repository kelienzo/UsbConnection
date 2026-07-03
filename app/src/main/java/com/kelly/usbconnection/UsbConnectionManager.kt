package com.kelly.usbconnection

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.charset.StandardCharsets

class UsbConnectionManager(private val context: Context) {

    companion object {

        private const val TAG = "UsbConnectionManager"

        private const val BAUD_RATE = 115200

        private const val DATA_BITS = 8

        private const val STOP_BITS = UsbSerialPort.STOPBITS_1

        private const val PARITY = UsbSerialPort.PARITY_NONE

        private const val WRITE_TIMEOUT = 3000

        const val ACTION_USB_PERMISSION = "com.kelly.usbconnection.USB_PERMISSION"

    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var usbConnection: UsbDeviceConnection? = null

    private var usbDriver: UsbSerialDriver? = null

    private var usbPort: UsbSerialPort? = null

    private var ioManager: SerialInputOutputManager? = null

    private val _connectionState = MutableStateFlow<UsbState>(UsbState.Disconnected)
    val connectionState: StateFlow<UsbState> = _connectionState

    private val _incomingData = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val incomingData: SharedFlow<ByteArray> = _incomingData

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> intent.usbDevice()?.let {
                    Log.d(TAG, "USB Attached")
                    handleUsbAttached(it)
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> intent.usbDevice()?.let {
                    Log.d(TAG, "USB Detached")
                    disconnect()
                }

                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted) {
                        intent.usbDevice()?.let {
                            Log.d(TAG, "USB Permission Granted")
                            connect(it)
                        }
                    } else {
                        Log.d(TAG, "USB Permission Denied")
                        _connectionState.value = UsbState.PermissionDenied
                    }
                }
            }
        }
    }

    fun start() {
        registerReceiver()
        scanConnectedDevices()
    }

    fun stop() {
        unregisterReceiver()
        disconnect()
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "BroadcastReceiver Registered")
    }

    private fun unregisterReceiver() {
        runCatching { context.unregisterReceiver(receiver) }
        Log.d(TAG, "BroadcastReceiver Unregistered")
    }

    private fun scanConnectedDevices() {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (drivers.isEmpty()) {
            Log.d(TAG, "No USB Devices Found")
            _connectionState.value = UsbState.Disconnected
            return
        }
        Log.d(TAG, "Found ${drivers.size} USB Driver(s)")
        handleUsbAttached(drivers.first().device)
    }

    private fun handleUsbAttached(device: UsbDevice) {
        _connectionState.value = UsbState.Connecting
        Log.d(TAG, "VendorId=${device.vendorId}")
        Log.d(TAG, "ProductId=${device.productId}")
        Log.d(TAG, "Device=${device.deviceName}")

        if (usbManager.hasPermission(device)) {
            Log.d(TAG, "Already Have Permission")
            connect(device)
            return
        }
        Log.d(TAG, "Requesting USB Permission")
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun connect(device: UsbDevice) {
        try {
            Log.d(TAG, "Opening USB Connection...")
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            usbDriver = drivers.firstOrNull { it.device.deviceId == device.deviceId }
            if (usbDriver == null) {
                Log.e(TAG, "No compatible serial driver found")
                _connectionState.value = UsbState.Error("No USB Serial Driver")
                return
            }
            usbConnection = usbManager.openDevice(device)
            if (usbConnection == null) {
                Log.e(TAG, "Unable to open UsbDeviceConnection")
                _connectionState.value = UsbState.Error("Unable to open USB Device")
                return
            }
            usbPort = usbDriver!!.ports.first()
            usbPort!!.open(usbConnection)
            Log.d(TAG, "USB Port Opened")
            usbPort?.apply {
                setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)
                dtr = true
                rts = true
            }
            Log.d(TAG, "Serial Port Configured")
            startReading()
            _connectionState.value = UsbState.Connected
            Log.d(TAG, "USB Connected Successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Connection Failed", e)
            disconnect()
            _connectionState.value = UsbState.Error(e.message ?: "Unknown Error")
        }
    }

    private fun startReading() {
        ioManager?.stop()
        ioManager = SerialInputOutputManager(usbPort, serialInputOutputListener)
        ioManager?.start()
        Log.d(TAG, "SerialInputOutputManager Started")
    }

    private val serialInputOutputListener = object : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray) {
            Log.d(TAG, "Received: ${String(data)}")
            Log.d(TAG, "Received US ASCII: ${String(data, StandardCharsets.US_ASCII)}")
            _incomingData.tryEmit(data)
        }

        override fun onRunError(e: Exception) {
            Log.e(TAG, "Read Thread Stopped", e)
            disconnect()
        }
    }

    fun write(command: String) {
//        usbConnectionManager.write("C,1\r\n")
//        usbConnectionManager.write("C,VEND,1.20\r\n")
        try {
            val bytes = "$command\r\n".toByteArray()
//            val bytes = command.toByteArray()
//            val bytes = command.toByteArray(StandardCharsets.US_ASCII)
            usbPort?.write(bytes, WRITE_TIMEOUT)
            Log.d(TAG, "TX -> $command")
        } catch (e: Exception) {
            Log.e(TAG, "Write Failed", e)
        }
    }

    fun write(bytes: ByteArray) {
        try {
            usbPort?.write(bytes, WRITE_TIMEOUT)
        } catch (e: Exception) {
            Log.e(TAG, "Write Failed", e)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting USB Device")
        _connectionState.value = UsbState.Disconnected
        try {
            ioManager?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed stopping IO Manager", e)
        }
        ioManager = null
        try {
            usbPort?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed closing USB Port", e)
        }
        usbPort = null
        try {
            usbConnection?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed closing USB Connection", e)
        }
        usbConnection = null
        usbDriver = null
    }

    fun isConnected() = usbPort != null && usbConnection != null && ioManager != null

    fun currentDevice() = usbDriver?.device

    fun reconnect() {
        currentDevice()?.let {
            disconnect()
            connect(it)
        }
    }

    fun purge() {
        try {
            usbPort?.purgeHwBuffers(true, true)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun Intent.usbDevice(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}