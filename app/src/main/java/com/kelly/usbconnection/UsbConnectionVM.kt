package com.kelly.usbconnection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class UsbConnectionVM(val usbConnectionManager: UsbConnectionManager) : ViewModel() {


    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        usbConnectionManager.run {
            start()

            incomingData.onEach { response ->
                val parts = response.split(",")

                val command = parts.getOrNull(0)
                val event = parts.getOrNull(1)
                val param1 = parts.getOrNull(2)
                val param2 = parts.getOrNull(3)
                val param3 = parts.getOrNull(4)
                when (event) {
                    "STATUS" -> {
                        if (param1.orEmpty() == "VEND") {
//                            c,STATUS,VEND,<price>,<item_number>
                            _uiState.update { it.copy(amount = null) }
                            Log.d(
                                "UsbConnectionVM",
                                "Amount: ${param2.orEmpty()}, Item Number: ${param3.orEmpty()}"
                            )
                            _uiState.update { it.copy(amount = param2) }
                        } else _uiState.update { it.copy(status = param1) }
                    }

                    "SET" -> {}
                }
            }.launchIn(viewModelScope)
        }
    }

    fun onAction(action: UsbConnectionScreenAction) {
        when (action) {
            UsbConnectionScreenAction.OnEnableDevice -> writeData("C,1")
            UsbConnectionScreenAction.OnDisableDevice -> {
                writeData("C,0")
                _uiState.value = UiState()
            }

            UsbConnectionScreenAction.OnSetAlwaysIdle -> writeData("C,SETCONF,mdb-always-idle=1")
            UsbConnectionScreenAction.OnSetCurrency -> writeData("C,SETCONF,mdb-currency-code=0x1566")
            UsbConnectionScreenAction.StopVending -> {
                writeData("C,STOP")
                resetAmount()
            }

            UsbConnectionScreenAction.OnDisplayMessage -> writeData("C,DISPLAY,Hello!")
            UsbConnectionScreenAction.OnStartVending -> writeData("C,START,1")
            UsbConnectionScreenAction.OnApproveVending -> {
                writeData("C,VEND,${uiState.value.amount.orEmpty()}")
                resetAmount()
            }

            UsbConnectionScreenAction.OnDenyVending -> {
                writeData("C,VEND,-1")
                resetAmount()
            }
        }
    }

    private fun resetAmount() {
        _uiState.update { it.copy(amount = null) }
    }

    private fun writeData(data: String) {
        usbConnectionManager.write(data)
    }

    override fun onCleared() {
        usbConnectionManager.stop()
    }

    data class UiState(
        val status: String? = null,
        val amount: String? = null
    )
}