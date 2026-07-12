package com.clockin.app.ui.screens.admin

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clockin.app.nfc.NfcManager
import com.clockin.app.util.CardNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WriteState {
    IDLE, WRITING, SUCCESS, ERROR, CONFIRM_OVERWRITE
}

data class WriteUiState(
    val cardNumberInput: String = "",
    val inputError: String? = null,
    val writeState: WriteState = WriteState.IDLE,
    val message: String = "Enter a badge number and tap a tag",
    val existingData: String? = null
)

class WriteTagViewModel(private val nfcManager: NfcManager) : ViewModel() {

    private val _uiState = MutableStateFlow(WriteUiState())
    val uiState: StateFlow<WriteUiState> = _uiState.asStateFlow()

    private var pendingTag: Tag? = null

    fun updateCardNumber(value: String) {
        val normalized = CardNumberUtils.normalize(value)
        _uiState.value = _uiState.value.copy(
            cardNumberInput = normalized,
            inputError = if (normalized.isEmpty() || CardNumberUtils.isValidCardNumber(normalized)) {
                null
            } else {
                "Badge number must contain only digits"
            }
        )
    }

    fun onTagDiscovered(tag: Tag) {
        val state = _uiState.value
        // Ignore new tags while an overwrite decision or a write is in progress
        if (state.writeState == WriteState.CONFIRM_OVERWRITE || state.writeState == WriteState.WRITING) {
            return
        }

        val cardNumber = CardNumberUtils.normalize(state.cardNumberInput)
        if (cardNumber.isEmpty()) {
            _uiState.value = state.copy(
                writeState = WriteState.ERROR,
                message = "Enter a badge number first"
            )
            return
        }
        if (!CardNumberUtils.isValidCardNumber(cardNumber)) {
            _uiState.value = state.copy(
                writeState = WriteState.ERROR,
                inputError = "Badge number must contain only digits",
                message = "Badge number must contain only digits"
            )
            return
        }

        // Check if tag already has data
        viewModelScope.launch {
            val existingData = try {
                withContext(Dispatchers.IO) { readExistingData(tag) }
            } catch (e: Exception) {
                // Can't read existing data, try to write anyway
                null
            }
            val currentWriteState = _uiState.value.writeState
            if (currentWriteState == WriteState.CONFIRM_OVERWRITE || currentWriteState == WriteState.WRITING) {
                return@launch
            }
            if (existingData != null && existingData.isNotEmpty()) {
                pendingTag = tag
                _uiState.value = _uiState.value.copy(
                    writeState = WriteState.CONFIRM_OVERWRITE,
                    existingData = existingData,
                    message = "Tag already contains data: \"$existingData\"\nOverwrite?"
                )
            } else {
                // Tag is empty, write directly
                performWrite(tag, cardNumber)
            }
        }
    }

    fun confirmOverwrite() {
        val tag = pendingTag ?: return
        pendingTag = null
        val cardNumber = CardNumberUtils.normalize(_uiState.value.cardNumberInput)
        viewModelScope.launch {
            performWrite(tag, cardNumber)
        }
    }

    fun cancelOverwrite() {
        pendingTag = null
        _uiState.value = _uiState.value.copy(
            writeState = WriteState.IDLE,
            existingData = null,
            message = "Enter a badge number and tap a tag"
        )
    }

    fun resetState() {
        pendingTag = null
        _uiState.value = _uiState.value.copy(
            writeState = WriteState.IDLE,
            existingData = null,
            message = "Enter a badge number and tap a tag"
        )
    }

    private suspend fun performWrite(tag: Tag, cardNumber: String) {
        val payload = CardNumberUtils.nfcPayload(cardNumber)
        _uiState.value = _uiState.value.copy(
            writeState = WriteState.WRITING,
            existingData = null,
            message = "Writing..."
        )

        val result = withContext(Dispatchers.IO) {
            nfcManager.writeTag(tag, payload)
        }

        if (result.isSuccess) {
            _uiState.value = _uiState.value.copy(
                writeState = WriteState.SUCCESS,
                message = "Written: $payload"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                writeState = WriteState.ERROR,
                message = "Error: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    private fun readExistingData(tag: Tag): String? {
        return nfcManager.readTextTag(tag).getOrNull()
    }
}
