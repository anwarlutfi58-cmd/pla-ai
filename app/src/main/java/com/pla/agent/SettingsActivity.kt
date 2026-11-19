package com.pla.agent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pla.agent.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnManageModels.setOnClickListener {
            startActivity(Intent(this, ModelDownloadActivity::class.java))
        }

        binding.btnBackSettings.setOnClickListener {
            finish()
        }

        binding.switchInternet.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Akses internet AI diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Akses internet AI dimatikan", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol untuk menyalakan Jarvis (Floating Overlay)
        binding.btnManageModels.setOnLongClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                Toast.makeText(this, "Izinkan aplikasi ini untuk muncul di atas aplikasi lain", Toast.LENGTH_LONG).show()
            } else {
                startService(Intent(this, FloatingService::class.java))
                Toast.makeText(this, "Jarvis (Melayang) Diaktifkan!", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}