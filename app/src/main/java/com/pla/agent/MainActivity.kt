package com.pla.agent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol Mulai Percakapan
        binding.btnChat.setOnClickListener {
            try {
                startActivity(Intent(this, ChatActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuka Chat: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Tombol Riwayat
        binding.btnHistory.setOnClickListener {
            try {
                startActivity(Intent(this, HistoryActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuka Riwayat: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Tombol Pengaturan
        binding.btnSettings.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuka Pengaturan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}