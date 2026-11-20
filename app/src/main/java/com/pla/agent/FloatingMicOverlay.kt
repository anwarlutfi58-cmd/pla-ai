package com.pla.agent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast

class FloatingMicOverlay : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    companion object {
        const val ACTION_SHOW = "com.pla.agent.SHOW_MIC"
        const val ACTION_HIDE = "com.pla.agent.HIDE_MIC"
        
        fun show(context: Context) {
            val intent = Intent(context, FloatingMicOverlay::class.java)
            intent.action = ACTION_SHOW
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun hide(context: Context) {
            val intent = Intent(context, FloatingMicOverlay::class.java)
            intent.action = ACTION_HIDE
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else 
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        params.x = 0
        params.y = 0

        // Buat view sederhana secara programmatic (tanpa XML baru)
        val button = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setBackgroundColor(0xFF4CAF50.toInt())
            setPadding(32, 32, 32, 32)
            setOnClickListener { toggleListening() }
        }
        
        overlayView = button
        windowManager?.addView(button, params)
        
        // Langsung mulai mendengarkan saat overlay muncul
        startListening()
    }

    private fun hideOverlay() {
        stopListening()
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        stopSelf()
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "STT tidak tersedia di HP ini", Toast.LENGTH_SHORT).show()
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                Toast.makeText(this@FloatingMicOverlay, "Error STT: $error", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val typed = AutoTyperAccessibility.typeText(text)
                    if (typed) {
                        Toast.makeText(this@FloatingMicOverlay, "Teks diketik!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@FloatingMicOverlay, "Gagal ketik. Aktifkan Accessibility dulu.", Toast.LENGTH_LONG).show()
                    }
                }
                hideOverlay()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.startListening(intent)
        isListening = true
    }

    private fun stopListening() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}