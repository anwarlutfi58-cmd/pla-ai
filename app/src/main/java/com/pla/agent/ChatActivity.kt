package com.pla.agent

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pla.agent.databinding.ActivityChatBinding
import kotlinx.coroutines.launch
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var aiEngine: PlAiEngine
    private lateinit var database: AppDatabase
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var preferences: AppPreferences
    private val chatHistory = StringBuilder()
    private var isGenerating = false
    private var currentTopic = "Umum"
    private var currentAiName = "PLA Agent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as AppContainer
        aiEngine = app.aiEngine
        database = app.database
        voiceEngine = VoiceEngine(this)
        preferences = app.preferences

        // Muat Nama dan Avatar Kustom
        currentAiName = preferences.getAiName()
        binding.imgAvatar.setImageResource(preferences.getAiAvatar())

        lifecycleScope.launch {
            currentTopic = database.appDao().getActiveTopic()?.name ?: "Umum"
            updateTopicInfo(currentTopic)
            muatRiwayatChat()
        }

        val models = aiEngine.getAvailableModels()
        if (models.isNotEmpty() && !aiEngine.isLoaded()) {
            muatModelSpesifik(models.first())
        } else if (models.isEmpty()) {
            Toast.makeText(this, "Tidak ada model. Unduh di Pengaturan!", Toast.LENGTH_LONG).show()
            finish()
        }

        binding.btnGantiModel.setOnClickListener {
            tampilkanDialogGantiModel()
        }

        binding.btnSend.setOnClickListener {
            val input = binding.edtMessage.text.toString().trim()
            if (input.isNotEmpty() && !isGenerating) {
                binding.edtMessage.setText("")
                kirimPesan(input)
            }
        }
    }

    private fun muatRiwayatChat() {
        lifecycleScope.launch {
            val history = database.appDao().getAllChats()
            chatHistory.clear()
            for (chat in history) {
                chatHistory.append("${chat.sender}: ${chat.message}\n\n")
            }
            binding.tvChatHistory.text = chatHistory.toString()
            scrollKeBawah()
        }
    }

    private fun tampilkanDialogGantiModel() {
        val models = aiEngine.getAvailableModels()
        if (models.isEmpty()) {
            Toast.makeText(this, "Belum ada model yang diunduh.", Toast.LENGTH_SHORT).show()
            return
        }

        val namaModel = models.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pilih Model AI")
            .setItems(namaModel) { _, which ->
                val fileTerpilih = models[which]
                muatModelSpesifik(fileTerpilih)
            }
            .show()
    }

    private fun muatModelSpesifik(file: File) {
        binding.tvModelInfo.text = "Memuat ${file.name}..."
        binding.btnGantiModel.isEnabled = false

        lifecycleScope.launch {
            val success = aiEngine.loadModel(file) { progress ->
                runOnUiThread { binding.tvModelInfo.text = progress }
            }
            runOnUiThread {
                binding.btnGantiModel.isEnabled = true
                if (!success) {
                    Toast.makeText(this@ChatActivity, "Gagal memuat model", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateTopicInfo(topic: String) {
        binding.tvTopicInfo.text = "Topik: $topic"
        // Avatar otomatis berubah berdasarkan topik, kecuali user set manual
        if (preferences.getAiAvatar() == R.drawable.avatar_umum) {
            when {
                topic.contains("Coding", ignoreCase = true) -> {
                    binding.imgAvatar.setImageResource(R.drawable.avatar_coding)
                }
                topic.contains("Kesehatan", ignoreCase = true) || topic.contains("Psikologi", ignoreCase = true) -> {
                    binding.imgAvatar.setImageResource(R.drawable.avatar_dokter)
                }
                else -> {
                    binding.imgAvatar.setImageResource(R.drawable.avatar_umum)
                }
            }
        }
    }

    private fun kirimPesan(userInput: String) {
        isGenerating = true
        binding.btnSend.isEnabled = false
        binding.btnGantiModel.isEnabled = false

        chatHistory.append("User: $userInput\n\n")
        binding.tvChatHistory.text = chatHistory.toString()
        scrollKeBawah()

        lifecycleScope.launch {
            database.appDao().insertChat(ChatHistoryEntity(sender = "User", message = userInput))
        }

        // MENGGUNAKAN NAMA AI KUSTOM
        chatHistory.append("$currentAiName: ")
        binding.tvChatHistory.text = chatHistory.toString()

        val aiResponse = StringBuilder()

        lifecycleScope.launch {
            aiEngine.generateResponse(
                userInput = userInput,
                onStatusUpdate = { status ->
                    runOnUiThread { binding.tvModelInfo.text = status }
                },
                onToken = { token ->
                    runOnUiThread {
                        aiResponse.append(token)
                        chatHistory.append(token)
                        binding.tvChatHistory.text = chatHistory.toString()
                        scrollKeBawah()
                    }
                }
            )
            
            runOnUiThread {
                chatHistory.append("\n\n")
                isGenerating = false
                binding.btnSend.isEnabled = true
                binding.btnGantiModel.isEnabled = true
                binding.tvModelInfo.text = "Siap"
                
                lifecycleScope.launch {
                    // SIMPAN DENGAN NAMA AI KUSTOM
                    database.appDao().insertChat(ChatHistoryEntity(sender = currentAiName, message = aiResponse.toString()))
                }

                voiceEngine.speak(aiResponse.toString(), currentTopic)
            }
        }
    }

    private fun scrollKeBawah() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.stop()
        voiceEngine.shutdown()
    }
}