package com.clockin.app.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.Charset

class NfcManager(context: Context) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun isNfcAvailable(): Boolean = nfcAdapter != null
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * Enable reader mode for scanning tags. Reads the card number from the NDEF Text Record.
     */
    fun enableReaderMode(
        activity: Activity,
        onTagRead: (cardNumber: String) -> Unit,
        onError: (String) -> Unit
    ) {
        nfcAdapter?.enableReaderMode(
            activity,
            { tag -> handleTagRead(tag, onTagRead, onError) },
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
    }

    /**
     * Enable reader mode for writing tags. Passes the raw Tag object to the callback.
     */
    fun enableWriteMode(
        activity: Activity,
        onTagDiscovered: (Tag) -> Unit
    ) {
        nfcAdapter?.enableReaderMode(
            activity,
            { tag -> onTagDiscovered(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
    }

    fun disableReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    private fun handleTagRead(
        tag: Tag,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    val cardNumber = readTextFromMessage(ndef.ndefMessage)
                    if (cardNumber != null) {
                        onSuccess(cardNumber)
                    } else {
                        onError("This badge doesn't contain a readable badge number. Rewrite it via Admin.")
                    }
                } finally {
                    try {
                        ndef.close()
                    } catch (_: Exception) {
                    }
                }
            } else {
                onError("This badge isn't set up correctly. Rewrite it via Admin.")
            }
        } catch (e: Exception) {
            onError("Couldn't read the badge. Hold it still against the phone and try again.")
        }
    }

    fun readTextTag(tag: Tag): Result<String?> {
        val ndef = Ndef.get(tag) ?: return Result.success(null)
        return try {
            ndef.connect()
            Result.success(readTextFromMessage(ndef.ndefMessage))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                ndef.close()
            } catch (_: Exception) {
            }
        }
    }

    fun writeTag(tag: Tag, cardNumber: String): Result<Unit> {
        return try {
            val ndefRecord = NdefRecord.createTextRecord("en", cardNumber)
            val ndefMessage = NdefMessage(arrayOf(ndefRecord))

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return Result.failure(Exception("Tag is read-only"))
                }
                if (ndefMessage.toByteArray().size > ndef.maxSize) {
                    ndef.close()
                    return Result.failure(Exception("Data too large for tag"))
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                Result.success(Unit)
            } else {
                // Try to format unformatted tag
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    ndefFormatable.format(ndefMessage)
                    ndefFormatable.close()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Tag cannot be NDEF formatted"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readTextFromMessage(message: NdefMessage?): String? {
        return message?.records
            ?.firstNotNullOfOrNull { record -> parseTextRecord(record) }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun parseTextRecord(record: NdefRecord): String? {
        if (record.tnf != NdefRecord.TNF_WELL_KNOWN) return null
        if (!record.type.contentEquals(NdefRecord.RTD_TEXT)) return null

        val payload = record.payload ?: return null
        if (payload.isEmpty()) return null

        val status = payload[0].toInt()
        val languageCodeLength = status and 0x3F
        val textStart = languageCodeLength + 1
        if (payload.size <= textStart) return null

        val charset: Charset = if ((status and 0x80) != 0) Charsets.UTF_16 else Charsets.UTF_8
        return String(payload, textStart, payload.size - textStart, charset)
    }
}
