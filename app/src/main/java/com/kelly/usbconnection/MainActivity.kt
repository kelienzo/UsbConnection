package com.kelly.usbconnection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kelly.usbconnection.ui.theme.UsbConnectionTheme
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UsbConnectionTheme {
                Scaffold { innerPadding ->
                    val viewModel = koinViewModel<UsbConnectionVM>()
                    val connectionState by viewModel.usbConnectionManager
                        .connectionState.collectAsStateWithLifecycle()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    UsbConnectionScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding),
                        usbState = connectionState,
                        uiState = uiState,
                        onAction = viewModel::onAction
                    )
                }
            }
        }
    }
}