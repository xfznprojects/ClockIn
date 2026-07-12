package com.clockin.app.util

import android.media.AudioManager
import android.media.ToneGenerator

class SoundManager {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (_: Exception) {
            // ToneGenerator can fail on some devices
        }
    }

    fun playSuccessBeep() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
