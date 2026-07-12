package com.clockin.app.ui.navigation

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clockin.app.data.local.PreferencesManager
import com.clockin.app.data.repository.ScanRepository
import com.clockin.app.nfc.NfcManager
import com.clockin.app.ui.screens.admin.AdminScreen
import com.clockin.app.ui.screens.admin.ExportScreen
import com.clockin.app.ui.screens.admin.PinDialog
import com.clockin.app.ui.screens.admin.WriteTagScreen
import com.clockin.app.ui.screens.database.DatabaseScreen
import com.clockin.app.ui.screens.home.HomeScreen
import com.clockin.app.ui.screens.scan.ScanScreen
import com.clockin.app.util.Constants
import com.clockin.app.util.DateTimeUtils
import com.clockin.app.util.ScanClock
import com.clockin.app.util.ScanTimeResult
import com.clockin.app.ui.theme.ScanFailRed
import androidx.compose.ui.unit.dp

@Composable
fun AppNavigation(
    nfcManager: NfcManager,
    repository: ScanRepository,
    preferencesManager: PreferencesManager,
    scanClock: ScanClock,
    activity: Activity,
    nfcLaunchRequest: Int = 0
) {
    val navController = rememberNavController()
    val isDarkMode by preferencesManager.isDarkMode.collectAsState()

    // Admin authentication state - persists across admin sub-screens
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showPinError by remember { mutableStateOf(false) }
    var showManualDateDialog by remember { mutableStateOf(false) }
    var manualDateInput by remember { mutableStateOf(scanClock.getManualDisplayDate() ?: "") }
    var manualDateError by remember { mutableStateOf<String?>(null) }
    var activeScanDate by remember { mutableStateOf(resolveCurrentScanDate(scanClock)) }
    var nfcEnabled by remember { mutableStateOf(nfcManager.isNfcEnabled()) }

    // Refresh "today" and the NFC adapter state whenever the app resumes,
    // so they don't go stale after midnight or after toggling NFC in settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activeScanDate = resolveCurrentScanDate(scanClock)
                nfcEnabled = nfcManager.isNfcEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (scanClock.needsManualDate()) {
            showManualDateDialog = true
        }
    }

    LaunchedEffect(nfcLaunchRequest) {
        if (nfcLaunchRequest > 0) {
            navController.navigate(Screen.Scan.route) {
                launchSingleTop = true
            }
        }
    }

    // PIN Dialog shown when trying to access admin
    if (showPinDialog) {
        PinDialog(
            showError = showPinError,
            onDismiss = {
                showPinDialog = false
                showPinError = false
            },
            onPinConfirmed = { pin ->
                if (pin == Constants.ADMIN_PIN) {
                    isAdminAuthenticated = true
                    showPinDialog = false
                    showPinError = false
                    navController.navigate(Screen.Admin.route)
                } else {
                    showPinError = true
                }
            }
        )
    }

    if (showManualDateDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Check date") },
            text = {
                Column {
                    Text(
                        text = "The phone's date looks unreliable. Enter today's date.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
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
                        Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
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
                            manualDateError = "Use dd/mm/yyyy, e.g. 29/06/2026"
                            return@TextButton
                        }
                        scanClock.setManualDate(storageDate)
                        activeScanDate = storageDate
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

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                nfcEnabled = nfcEnabled,
                isDarkMode = isDarkMode,
                repository = repository,
                todayDate = activeScanDate,
                onToggleTheme = { preferencesManager.setDarkMode(!isDarkMode) },
                onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                onNavigateToDatabase = { navController.navigate(Screen.Database.route) },
                onNavigateToAdmin = {
                    if (isAdminAuthenticated) {
                        navController.navigate(Screen.Admin.route)
                    } else {
                        showPinDialog = true
                    }
                }
            )
        }

        composable(Screen.Scan.route) {
            ScanScreen(
                nfcManager = nfcManager,
                repository = repository,
                preferencesManager = preferencesManager,
                scanClock = scanClock,
                activity = activity,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Database.route) {
            DatabaseScreen(
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Admin.route) {
            AdminScreen(
                preferencesManager = preferencesManager,
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWriteTag = { navController.navigate(Screen.WriteTag.route) },
                onNavigateToExport = { navController.navigate(Screen.ExportDatabase.route) }
            )
        }

        composable(Screen.WriteTag.route) {
            WriteTagScreen(
                nfcManager = nfcManager,
                activity = activity,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExportDatabase.route) {
            ExportScreen(
                repository = repository,
                activity = activity,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun resolveCurrentScanDate(scanClock: ScanClock): String {
    return when (val result = scanClock.getScanTime()) {
        is ScanTimeResult.Valid -> result.scanTime.scanDate
        is ScanTimeResult.Invalid -> DateTimeUtils.getCurrentDate()
    }
}
