package com.kelly.usbconnection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kelly.usbconnection.ui.theme.UsbConnectionTheme

@Composable
fun UsbConnectionScreen(
    modifier: Modifier = Modifier,
    usbState: UsbState,
    uiState: UsbConnectionVM.UiState,
    onAction: (UsbConnectionScreenAction) -> Unit
) {
    Scaffold(modifier = modifier) { p ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (usbState) {
                UsbState.Connected -> {
                    uiState.run {
                        Text("STATUS: ${status.orEmpty()}")
                        amount?.let { Text("Amount: $it") }
                    }

                    Button(onClick = { onAction(UsbConnectionScreenAction.OnEnableDevice) }) {
                        Text("Enable Device")
                    }

                    Button(onClick = { onAction(UsbConnectionScreenAction.OnDisableDevice) }) {
                        Text("Disable Device")
                    }

                    Button(onClick = { onAction(UsbConnectionScreenAction.OnSetAlwaysIdle) }) {
                        Text("Set Always Idle")
                    }

                    Button(onClick = { onAction(UsbConnectionScreenAction.OnSetCurrency) }) {
                        Text("Set Currency")
                    }

                    Button(
                        enabled = uiState.run { status != null },
                        onClick = { onAction(UsbConnectionScreenAction.StopVending) }) {
                        Text("Stop vending session")
                    }

                    Button(
                        enabled = uiState.run { status != null },
                        onClick = { onAction(UsbConnectionScreenAction.OnDisplayMessage) }) {
                        Text("Display message")
                    }

                    Button(
                        enabled = uiState.run { status != null },
                        onClick = { onAction(UsbConnectionScreenAction.OnStartVending) }) {
                        Text("Start vending request")
                    }

                    Button(
                        enabled = uiState.run { status != null },
                        onClick = { onAction(UsbConnectionScreenAction.OnApproveVending) }) {
                        Text("Approve vending request")
                    }

                    Button(
                        enabled = uiState.run { status != null },
                        onClick = { onAction(UsbConnectionScreenAction.OnDenyVending) }) {
                        Text("Deny vending request")
                    }
                }

                UsbState.Connecting -> Text("Connecting...")
                UsbState.Disconnected -> Text("Disconnected")
                is UsbState.Error -> Text(usbState.message)
                UsbState.PermissionDenied -> Text("Permission Denied")
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
private fun UsbConnectionScreenPrev() = UsbConnectionTheme {
    UsbConnectionScreen(
        usbState = UsbState.Connected,
        uiState = UsbConnectionVM.UiState(),
        onAction = {}
    )
}

sealed interface UsbConnectionScreenAction {
    data object OnEnableDevice : UsbConnectionScreenAction
    data object OnDisableDevice : UsbConnectionScreenAction
    data object OnSetAlwaysIdle : UsbConnectionScreenAction
    data object OnSetCurrency : UsbConnectionScreenAction
    data object OnDisplayMessage : UsbConnectionScreenAction
    data object OnStartVending : UsbConnectionScreenAction
    data object OnApproveVending : UsbConnectionScreenAction
    data object OnDenyVending : UsbConnectionScreenAction
    data object StopVending : UsbConnectionScreenAction
}