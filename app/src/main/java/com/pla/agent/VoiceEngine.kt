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
            // Set bahasa ke Indonesia
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Jika HP tidak punya bahasa Indonesia, fallback ke English
                tts?.setLanguage(Locale.US)
            }
            isReady = true
        }
    }

    fun speak(text: String, topic: String, isTtsEnabled: Boolean) {
        if (!isReady || !isTtsEnabled) return

        // ==========================================
        // EVOLUSI KARAKTER SUARA BERDASARKAN TOPIK
        // ==========================================
        when {
            topic.contains("Coding", ignoreCase = true) -> {
                // Suara programmer: Cepat, tegas, nada agak tinggi
                tts?.setPitch(1.15f)
                tts?.setSpeechRate(1.25f)
            }
            topic.contains("Psikologi", ignoreCase = true) || topic.contains("Kesehatan", ignoreCase = true) -> {
                // Suara dokter/psikolog: Tenang, lambat, nada rendah (menenangkan)
                tts?.setPitch(0.85f)
                tts?.setSpeechRate(0.85f)
            }
            topic.contains("Matematika", ignoreCase = true) -> {
                // Suara analitis: Sedikit monoton, sedang
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)
            }
            else -> {
                // Suara Default (Asisten)
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.0f)
            }
        }

        // Hentikan suara sebelumnya jika masih ada
        tts?.stop()
        
        // Mainkan suara baru
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PLA_VOICE")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}