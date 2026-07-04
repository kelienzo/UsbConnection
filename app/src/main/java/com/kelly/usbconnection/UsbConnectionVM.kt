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
                val (command, event, param1, param2, param3) = response.split(",")
                when (event) {
                    "STATUS" -> {
                        if (param1 == "VEND") {
//                            c,STATUS,VEND,<price>,<item_number>
                            Log.d("UsbConnectionVM", "Amount: $param2, Item Number: $param3")
                            _uiState.update { it.copy(amount = param2) }
                        } else _uiState.update { it.copy(status = param1) }
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    fun onAction(action: UsbConnectionScreenAction) {
        when (action) {
            UsbConnectionScreenAction.OnEnableDevice -> writeData("C,1")
            UsbConnectionScreenAction.DisplayMessage -> writeData("C,DISPLAY,Hello!")
            UsbConnectionScreenAction.StartVending -> writeData("C,START,200")
        }
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