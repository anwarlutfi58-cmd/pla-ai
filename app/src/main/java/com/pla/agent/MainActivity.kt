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
            // Nanti kita arahkan ke ChatActivity
            Toast.makeText(this, "Membuka layar percakapan...", Toast.LENGTH_SHORT).show()
        }

        // Tombol Riwayat
        binding.btnHistory.setOnClickListener {
            Toast.makeText(this, "Fitur riwayat akan segera hadir", Toast.LENGTH_SHORT).show()
        }

        // Tombol Pengaturan
        binding.btnSettings.setOnClickListener {
            Toast.makeText(this, "Membuka pengaturan...", Toast.LENGTH_SHORT).show()
        }
    }
}