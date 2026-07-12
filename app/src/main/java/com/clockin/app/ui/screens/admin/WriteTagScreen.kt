package com.clockin.app.ui.screens.admin

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.clockin.app.nfc.NfcManager
import com.clockin.app.ui.theme.GreenAccent
import com.clockin.app.ui.theme.ScanFailRed
import com.clockin.app.ui.theme.ScanWaitingGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteTagScreen(
    nfcManager: NfcManager,
    activity: Activity,
    onNavigateBack: () -> Unit
) {
    val viewModel: WriteTagViewModel = viewModel(
        factory = viewModelFactory {
            initializer { WriteTagViewModel(nfcManager) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    // NFC Write Mode lifecycle
    DisposableEffect(activity) {
        nfcManager.enableWriteMode(activity) { tag ->
            activity.runOnUiThread {
                viewModel.onTagDiscovered(tag)
            }
        }
        onDispose {
            nfcManager.disableReaderMode(activity)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Write Badge") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Badge number input with fixed "Badge" prefix
            OutlinedTextField(
                value = uiState.cardNumberInput,
                onValueChange = {
                    viewModel.updateCardNumber(it)
                    if (uiState.writeState != WriteState.IDLE) {
                        viewModel.resetState()
                    }
                },
                label = { Text("Number") },
                prefix = { Text("Badge ") },
                isError = uiState.inputError != null,
                supportingText = uiState.inputError?.let { error ->
                    { Text(error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Overwrite confirmation card
            if (uiState.writeState == WriteState.CONFIRM_OVERWRITE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ScanFailRed.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = ScanFailRed,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tag already contains data!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ScanFailRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Current content:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\"${uiState.existingData}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = { viewModel.cancelOverwrite() }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { viewModel.confirmOverwrite() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ScanFailRed
                                )
                            ) {
                                Text("Overwrite")
                            }
                        }
                    }
                }
            } else {
                // Status icon
                Icon(
                    imageVector = when (uiState.writeState) {
                        WriteState.IDLE, WriteState.WRITING -> Icons.Default.Nfc
                        WriteState.SUCCESS -> Icons.Default.Check
                        WriteState.ERROR -> Icons.Default.Close
                        else -> Icons.Default.Nfc
                    },
                    contentDescription = null,
                    tint = when (uiState.writeState) {
                        WriteState.IDLE -> ScanWaitingGray
                        WriteState.WRITING -> MaterialTheme.colorScheme.primary
                        WriteState.SUCCESS -> GreenAccent
                        WriteState.ERROR -> ScanFailRed
                        else -> ScanWaitingGray
                    },
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (uiState.writeState) {
                        WriteState.SUCCESS -> GreenAccent
                        WriteState.ERROR -> ScanFailRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
