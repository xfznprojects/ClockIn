package com.clockin.app.ui.screens.scan

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockin.app.data.local.PreferencesManager
import com.clockin.app.data.repository.ScanRepository
import com.clockin.app.nfc.NfcManager
import com.clockin.app.ui.components.ScanIndicator
import com.clockin.app.ui.components.ScanState
import com.clockin.app.ui.theme.GreenAccent
import com.clockin.app.ui.theme.ScanFailRed
import com.clockin.app.util.DateTimeUtils
import com.clockin.app.util.ScanClock
import com.clockin.app.util.SoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    nfcManager: NfcManager,
    repository: ScanRepository,
    preferencesManager: PreferencesManager,
    scanClock: ScanClock,
    activity: Activity,
    onNavigateBack: () -> Unit
) {
    val viewModel: ScanViewModel = viewModel(
        factory = ScanViewModel.Factory(repository, preferencesManager, scanClock)
    )
    val uiState by viewModel.uiState.collectAsState()
    val soundManager = remember { SoundManager() }
    var showManualDateDialog by remember { mutableStateOf(false) }
    var manualDateInput by remember { mutableStateOf(scanClock.getManualDisplayDate() ?: "") }
    var manualDateError by remember { mutableStateOf<String?>(null) }
    val failureLocked = uiState.failureAcknowledgementRequired

    LaunchedEffect(Unit) {
        if (scanClock.needsManualDate()) {
            showManualDateDialog = true
        }
    }

    if (showManualDateDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Check date") },
            text = {
                Column {
                    Text(
                        text = "The phone's date does not look reliable. Enter today's date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualDateInput,
                        onValueChange = {
                            manualDateInput = it
                            manualDateError = null
                        },
                        label = { Text("Today (dd/mm/yyyy)") },
                        singleLine = true,
                        isError = manualDateError != null
                    )
                    if (manualDateError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = manualDateError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ScanFailRed
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val storageDate = DateTimeUtils.parseDateInputToStorage(manualDateInput)
                        if (storageDate == null) {
                            manualDateError = "Use dd/mm/yyyy, for example 29/06/2026"
                            return@TextButton
                        }
                        scanClock.setManualDate(storageDate)
                        manualDateInput = DateTimeUtils.formatDisplayDate(storageDate)
                        manualDateError = null
                        showManualDateDialog = false
                    }
                ) {
                    Text("Save")
                }
            }
        )
    }

    if (failureLocked) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Scan failed") },
            text = {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledgeScanFailure() }) {
                    Text("Scan again")
                }
            },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
            textContentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    // Sound feedback on success only
    LaunchedEffect(uiState.scanState, uiState.sessionCount) {
        if (uiState.scanState == ScanState.SUCCESS) {
            soundManager.playSuccessBeep()
        }
    }

    // Keep reader mode active while the scan screen is visible so Android's own tag UI never takes over.
    // The view model ignores scans while the failure popup is waiting for acknowledgement.
    DisposableEffect(activity) {
        nfcManager.enableReaderMode(
            activity = activity,
            onTagRead = { cardNumber ->
                activity.runOnUiThread {
                    if (scanClock.needsManualDate()) {
                        manualDateInput = scanClock.getManualDisplayDate() ?: ""
                        manualDateError = null
                        showManualDateDialog = true
                    } else {
                        viewModel.onTagScanned(cardNumber)
                    }
                }
            },
            onError = { error ->
                activity.runOnUiThread {
                    viewModel.onScanError(error)
                }
            }
        )
        onDispose {
            nfcManager.disableReaderMode(activity)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (failureLocked) ScanFailRed else MaterialTheme.colorScheme.background,
                    titleContentColor = if (failureLocked) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = if (failureLocked) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = if (failureLocked) ScanFailRed else MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large scan indicator with animations
            ScanIndicator(state = uiState.scanState)

            Spacer(modifier = Modifier.height(32.dp))

            // Status message
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.titleLarge,
                color = when (uiState.scanState) {
                    ScanState.SUCCESS -> GreenAccent
                    ScanState.ERROR -> if (failureLocked) MaterialTheme.colorScheme.onError else ScanFailRed
                    ScanState.WAITING -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Session counter
            Text(
                text = "Scans this session: ${uiState.sessionCount}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (failureLocked) {
                    MaterialTheme.colorScheme.onError.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
