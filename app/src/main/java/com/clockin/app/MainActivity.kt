package com.clockin.app

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.clockin.app.data.repository.ScanRepository
import com.clockin.app.nfc.NfcManager
import com.clockin.app.ui.navigation.AppNavigation
import com.clockin.app.ui.theme.ClockInTheme

class MainActivity : ComponentActivity() {

    private lateinit var nfcManager: NfcManager
    private lateinit var repository: ScanRepository
    private val nfcLaunchRequest = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only handle the launch intent on a fresh start; on recreation (e.g. rotation)
        // the intent was already processed and re-handling would re-navigate to Scan.
        if (savedInstanceState == null) {
            updateNfcLaunchRequest(intent)
        }

        nfcManager = NfcManager(this)
        val app = application as ClockInApp
        val database = app.database
        repository = ScanRepository(database.scanRecordDao())
        val preferencesManager = app.preferencesManager
        val scanClock = app.scanClock

        setContent {
            val isDarkMode by preferencesManager.isDarkMode.collectAsState()

            ClockInTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        nfcManager = nfcManager,
                        repository = repository,
                        preferencesManager = preferencesManager,
                        scanClock = scanClock,
                        activity = this,
                        nfcLaunchRequest = nfcLaunchRequest.value
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateNfcLaunchRequest(intent)
    }

    private fun updateNfcLaunchRequest(intent: Intent?) {
        if (intent?.isNfcIntent() == true) {
            nfcLaunchRequest.value = nfcLaunchRequest.value + 1
        }
    }

    private fun Intent.isNfcIntent(): Boolean {
        // The manifest only registers NDEF_DISCOVERED (text/plain)
        return action == NfcAdapter.ACTION_NDEF_DISCOVERED
    }
}
