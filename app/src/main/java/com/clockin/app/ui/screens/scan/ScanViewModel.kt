package com.clockin.app.ui.screens.scan

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clockin.app.data.local.PreferencesManager
import com.clockin.app.data.local.ScanRecord
import com.clockin.app.data.repository.ScanRepository
import com.clockin.app.ui.components.ScanState
import com.clockin.app.util.CardNumberUtils
import com.clockin.app.util.Constants
import com.clockin.app.util.ScanClock
import com.clockin.app.util.ScanTimeResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val scanState: ScanState = ScanState.WAITING,
    val lastCardNumber: String = "",
    val sessionCount: Int = 0,
    val message: String = "Ready to scan",
    val failureAcknowledgementRequired: Boolean = false
)

class ScanViewModel(
    private val repository: ScanRepository,
    private val preferencesManager: PreferencesManager,
    private val scanClock: ScanClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var lastScannedCard: String = ""
    private var lastScanTime: Long = 0
    private var resetJob: Job? = null

    fun onTagScanned(cardNumber: String) {
        if (_uiState.value.failureAcknowledgementRequired) {
            return
        }

        val normalizedCardNumber = CardNumberUtils.normalize(cardNumber)
        if (normalizedCardNumber.isBlank()) {
            onScanError("Badge number missing. This NFC badge does not contain a badge number.")
            return
        }
        if (!CardNumberUtils.isValidCardNumber(normalizedCardNumber)) {
            onScanError("Badge number invalid. Expected only a badge number, for example Badge 64.")
            return
        }

        // Debounce duplicate scans of same card (rapid fire); monotonic clock, immune to wall-clock changes
        val now = SystemClock.elapsedRealtime()
        if (normalizedCardNumber == lastScannedCard && (now - lastScanTime) < Constants.NFC_READER_DEBOUNCE_MS) {
            return
        }
        lastScannedCard = normalizedCardNumber
        lastScanTime = now

        viewModelScope.launch {
            val scanTimeResult = scanClock.getScanTime()
            if (scanTimeResult is ScanTimeResult.Invalid) {
                showFailure(scanTimeResult.message, normalizedCardNumber)
                return@launch
            }

            val validResult = scanTimeResult as ScanTimeResult.Valid
            val scanTime = validResult.scanTime

            // Check duplicate prevention setting
            if (preferencesManager.preventDuplicates.value) {
                val isDuplicate = repository.isDuplicateForDay(normalizedCardNumber, scanTime.scanDate)

                if (isDuplicate) {
                    showFailure("${CardNumberUtils.display(normalizedCardNumber)} already scanned today", normalizedCardNumber)
                    return@launch
                }
            }

            // Re-check the failure lock: a concurrent scan may have failed while we were suspended
            if (_uiState.value.failureAcknowledgementRequired) {
                return@launch
            }

            val record = ScanRecord(
                scanDate = scanTime.scanDate,
                cardNumber = normalizedCardNumber,
                scanTime = scanTime.scanTime,
                timestamp = scanTime.timestamp
            )
            repository.insertRecord(record)

            if (_uiState.value.failureAcknowledgementRequired) {
                return@launch
            }

            val successMessage = if (validResult.usedEstimatedTime) {
                "${CardNumberUtils.display(normalizedCardNumber)} · estimated time"
            } else {
                CardNumberUtils.display(normalizedCardNumber)
            }
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.SUCCESS,
                lastCardNumber = normalizedCardNumber,
                sessionCount = _uiState.value.sessionCount + 1,
                message = successMessage
            )

            // Cancel a pending reset from a previous success so it can't wipe this confirmation early
            resetJob?.cancel()
            resetJob = viewModelScope.launch {
                delay(1500)
                if (_uiState.value.scanState == ScanState.SUCCESS) {
                    _uiState.value = _uiState.value.copy(
                        scanState = ScanState.WAITING,
                        message = "Ready to scan"
                    )
                }
            }
        }
    }

    fun onScanError(error: String) {
        if (_uiState.value.failureAcknowledgementRequired) {
            return
        }
        showFailure(error)
    }

    fun acknowledgeScanFailure() {
        lastScannedCard = ""
        lastScanTime = 0
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.WAITING,
            message = "Ready to scan",
            failureAcknowledgementRequired = false
        )
    }

    private fun showFailure(message: String, cardNumber: String = _uiState.value.lastCardNumber) {
        resetJob?.cancel()
        resetJob = null
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.ERROR,
            lastCardNumber = cardNumber,
            message = message,
            failureAcknowledgementRequired = true
        )
    }

    class Factory(
        private val repository: ScanRepository,
        private val preferencesManager: PreferencesManager,
        private val scanClock: ScanClock
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanViewModel(repository, preferencesManager, scanClock) as T
        }
    }
}
