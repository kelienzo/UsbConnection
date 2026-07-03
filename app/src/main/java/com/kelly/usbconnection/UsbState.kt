package com.kelly.usbconnection

sealed interface UsbState {

    data object Disconnected : UsbState

    data object Connecting : UsbState

    data object Connected : UsbState

    data object PermissionDenied : UsbState

    data class Error(val message: String) : UsbState
}