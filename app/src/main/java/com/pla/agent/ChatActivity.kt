package com.pla.agent

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var aiEngine: PlAiEngine
    private lateinit var database: AppDatabase
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var preferences: AppPreferences
    
    // Container untuk bubble chat
    private lateinit var chatContainer: LinearLayout
    private val currentAiName: String get() = preferences.getAiName()
    private var isGenerating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as AppContainer
        aiEngine = app.aiEngine
        database = app.database
        voiceEngine = VoiceEngine(this)
        preferences = app.preferences

        // Setup UI Profesional
        setupProfessionalUI()
        
        // Sembunyikan elemen lama yang tidak dipakai
        binding.tvChatHistory.visibility = android.view.View.GONE
        binding.btnGantiModel.visibility = android.view.View.GONE
        binding.tvModelInfo.text = "Independent AI Engine"

        lifecycleScope.launch {
            val version = database.appDao().getActiveVersion()?.versionName ?: "V1. pla"
            binding.tvTopicInfo.text = "Model: $version"
            muatRiwayatChatBubble()
        }

        binding.btnSend.setOnClickListener { kirimPesan() }
    }

    private fun setupProfessionalUI() {
        // Buat container scrollable untuk bubble chat
        chatContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Masukkan container ke dalam ScrollView yang sudah ada di XML
        binding.scrollView.removeAllViews()
        binding.scrollView.addView(chatContainer)
        
        // Styling input field agar lebih modern
        binding.edtMessage.apply {
            hint = "Ketik pertanyaan..."
            textSize = 16f
            setPadding(24, 16, 24, 16)
        }
    }

    private fun muatRiwayatChatBubble() {
        lifecycleScope.launch {
            val history = database.appDao().getAllChats()
            chatContainer.removeAllViews()
            for (chat in history) {
                if (chat.sender == "User") {
                    tambahBubbleUser(chat.message, false)
                } else {
                    tambahBubbleAI(chat.message, false)
                }
            }
            scrollKeBawah()
        }
    }

    private fun kirimPesan() {
        val input = binding.edtMessage.text.toString().trim()
        if (input.isEmpty() || isGenerating) return

        binding.edtMessage.setText("")
        isGenerating = true
        binding.btnSend.isEnabled = false

        // Tampilkan bubble user segera
        tambahBubbleUser(input, true)
        scrollKeBawah()

        // Simpan ke DB
        lifecycleScope.launch {
            database.appDao().insertChat(ChatHistoryEntity(sender = "User", message = input))
        }

        // Tampilkan indikator typing AI
        val typingBubble = TextView(this).apply {
            text = "$currentAiName sedang mengetik..."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(16, 8, 16, 8)
            gravity = Gravity.START
        }
        chatContainer.addView(typingBubble)
        scrollKeBawah()

        val aiResponse = StringBuilder()

        lifecycleScope.launch {
            aiEngine.generateResponse(
                userInput = input,
                onStatusUpdate = { status ->
                    runOnUiThread { binding.tvModelInfo.text = status }
                },
                onToken = { token ->
                    runOnUiThread {
                        aiResponse.append(token)
                        // Update bubble AI secara realtime
                        updateBubbleAI(aiResponse.toString())
                        scrollKeBawah()
                    }
                }
            )
            
            runOnUiThread {
                chatContainer.removeView(typingBubble)
                isGenerating = false
                binding.btnSend.isEnabled = true
                binding.tvModelInfo.text = "Independent AI Engine"
                
                // Simpan jawaban AI ke DB
                lifecycleScope.launch {
                    database.appDao().insertChat(
                        ChatHistoryEntity(sender = currentAiName, message = aiResponse.toString())
                    )
                }
                
                // TTS jika aktif
                try {
                    if (preferences.isTtsEnabled()) {
                        voiceEngine.speak(aiResponse.toString(), "V1. pla", true)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // === FUNGSI BUBBLE CHAT ===

    private fun tambahBubbleUser(teks: String, simpanKeDb: Boolean) {
        val bubble = createBubbleView(teks, true)
        chatContainer.addView(bubble)
    }

    private fun tambahBubbleAI(teks: String, animasi: Boolean) {
        val bubble = createBubbleView(teks, false)
        chatContainer.addView(bubble)
    }

    private fun updateBubbleAI(teks: String) {
        // Cari TextView terakhir yang merupakan bubble AI
        val lastView = chatContainer.getChildAt(chatContainer.childCount - 1)
        if (lastView is TextView && lastView.tag == "ai_bubble") {
            lastView.text = teks
        } else {
            // Jika belum ada bubble AI (misal baru mulai streaming), buat baru
            val bubble = createBubbleView(teks, false)
            chatContainer.addView(bubble)
        }
    }

    private fun createBubbleView(teks: String, isUser: Boolean): TextView {
        return TextView(this).apply {
            text = teks
            textSize = 16f
            setPadding(24, 16, 24, 16)
            tag = if (isUser) "user_bubble" else "ai_bubble"
            
            // Styling Bubble
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                color = if (isUser) 
                    android.graphics.Color.parseColor("#2196F3") // Biru User
                else 
                    android.graphics.Color.parseColor("#E0E0E0") // Abu AI
                
                if (isUser) {
                    // Sudut tajam di kanan atas untuk user
                    cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 24f, 24f)
                } else {
                    // Sudut tajam di kiri atas untuk AI
                    cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 24f, 24f, 0f, 0f)
                }
            }
            background = drawable
            
            // Warna teks & alignment
            setTextColor(if (isUser) Color.WHITE else Color.BLACK)
            gravity = if (isUser) Gravity.END else Gravity.START
            
            // Layout params
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                width = resources.displayMetrics.widthPixels * 75 / 100 // Max 75% lebar layar
                marginStart = if (isUser) resources.displayMetrics.widthPixels * 25 / 100 else 0
                marginEnd = if (isUser) 0 else resources.displayMetrics.widthPixels * 25 / 100
                topMargin = 8
                bottomMargin = 8
            }
            
            // Long press untuk Salin/Edit (hanya untuk User)
            if (isUser) {
                setOnLongClickListener {
                    tampilkanMenuUserBubble(teks)
                    true
                }
            }
        }
    }

    private fun tampilkanMenuUserBubble(teks: String) {
        val options = arrayOf("📋 Salin Teks", "✏️ Edit Pertanyaan")
        AlertDialog.Builder(this)
            .setTitle("Opsi Pesan")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Salin
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Pesan User", teks))
                        Toast.makeText(this, "Teks disalin!", Toast.LENGTH_SHORT).show()
                    }
                    1 -> { // Edit
                        binding.edtMessage.setText(teks)
                        binding.edtMessage.setSelection(teks.length)
                        binding.edtMessage.requestFocus()
                    }
                }
            }
            .show()
    }

    private fun scrollKeBawah() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { voiceEngine.stop(); voiceEngine.shutdown() } catch (_: Exception) {}
    }
}