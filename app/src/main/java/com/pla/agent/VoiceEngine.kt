package com.pla.agent

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("id", "ID") // Bahasa Indonesia
            isReady = true
        }
    }

    fun speak(text: String, topic: String) {
        if (!isReady) return

        // EVOLUSI SUARA BERDASARKAN TOPIK
        when {
            topic.contains("Coding", ignoreCase = true) -> {
                // Suara tegas, cepat, profesional
                tts?.setPitch(1.1f)
                tts?.setSpeechRate(1.3f)
            }
            topic.contains("Psikologi", ignoreCase = true) || topic.contains("Kesehatan", ignoreCase = true) -> {
                // Suara tenang, lambat, empatik
                tts?.setPitch(0.9f)
                tts?.setSpeechRate(0.8f)
            }
            else -> {
                // Suara default netral
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)
            }
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PLA_VOICE")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}