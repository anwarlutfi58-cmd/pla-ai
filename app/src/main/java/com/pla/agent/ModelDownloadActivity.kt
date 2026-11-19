package com.pla.agent

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivityModelDownloadBinding

class ModelDownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelDownloadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menonaktifkan fitur unduh karena kita sudah beralih ke Independent AI
        binding.btnDownload.isEnabled = false
        binding.txtProgress.text = "Sistem Independent AI Aktif.\n\nAplikasi ini tidak lagi bergantung pada file model .gguf atau .task dari luar. Otak AI dibangun murni dari kode pemrograman dan data pelatihan (JSON) yang Anda masukkan di menu Pengaturan."
        
        Toast.makeText(this, "Fitur ini sudah tidak digunakan.", Toast.LENGTH_LONG).show()
    }
}