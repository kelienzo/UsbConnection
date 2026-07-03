package com.kelly.usbconnection

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
                val (command, event, result) = response.split(",")
                when (event) {
                    "STATUS" -> _uiState.update { it.copy(status = result) }
                    "VEND" -> {}
                    "DISPLAY" -> _uiState.update { it.copy(displayMessage = result) }
                }
            }.launchIn(viewModelScope)
        }
    }

    fun onAction(action: UsbConnectionScreenAction) {
        when (action) {
            UsbConnectionScreenAction.OnEnableDevice -> writeData("C,1")
            UsbConnectionScreenAction.DisplayMessage -> writeData("C,DISPLAY,Hello!")
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
        val displayMessage: String? = null,
    )
}