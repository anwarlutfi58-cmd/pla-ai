package com.pla.agent

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Layout programmatic sederhana (tanpa XML baru)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Pengaturan PLA Agent"
            textSize = 24f
            setTextColor(0xFF4CAF50.toInt())
            setPadding(0, 0, 0, 32)
        }
        layout.addView(title)

        // === BAGIAN LAMA: NAMA AI (PONDASI) ===
        val aiNameLabel = TextView(this).apply {
            text = "Nama AI:"
            textSize = 16f
        }
        layout.addView(aiNameLabel)

        val aiNameInput = android.widget.EditText(this).apply {
            setText(preferences.getAiName())
            hint = "Masukkan nama AI"
        }
        layout.addView(aiNameInput)

        val saveNameBtn = Button(this).apply {
            text = "Simpan Nama AI"
            setOnClickListener {
                val newName = aiNameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    preferences.setAiName(newName)
                    Toast.makeText(this@SettingsActivity, "Nama disimpan!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(saveNameBtn)

        // Separator
        layout.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply { topMargin = 32; bottomMargin = 32 }
            setBackgroundColor(0xFFCCCCCC.toInt())
        })

        // === BAGIAN BARU: KONTROL TOMBOL FISIK ===
        val hardwareTitle = TextView(this).apply {
            text = "🎤 Kontrol Suara Latar Belakang"
            textSize = 18f
            setTextColor(0xFF4CAF50.toInt())
            setPadding(0, 0, 0, 16)
        }
        layout.addView(hardwareTitle)

        val hardwareDesc = TextView(this).apply {
            text = "Ketuk Volume Up 2x cepat untuk memanggil mikrofon PLA Agent dari aplikasi manapun. Hasil suara akan diketik otomatis ke kolom teks aktif."
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(hardwareDesc)

        val hardwareSwitch = Switch(this).apply {
            text = "Aktifkan Pemicu Tombol Volume"
            isChecked = preferences.isHardwareTriggerEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                preferences.setHardwareTriggerEnabled(isChecked)
                if (isChecked) {
                    HardwareTriggerService.start(this@SettingsActivity)
                    Toast.makeText(this@SettingsActivity, "Fitur aktif! Ketuk Volume Up 2x.", Toast.LENGTH_LONG).show()
                } else {
                    HardwareTriggerService.stop(this@SettingsActivity)
                    Toast.makeText(this@SettingsActivity, "Fitur dimatikan.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(hardwareSwitch)

        // Status Accessibility
        val accessStatus = TextView(this).apply {
            text = if (isAccessibilityEnabled()) 
                "✅ AutoTyper AKTIF - Siap mengetik otomatis" 
            else 
                "❌ AutoTyper BELUM AKTIF - Klik tombol di bawah untuk mengaktifkan"
            textSize = 14f
            setPadding(0, 16, 0, 8)
            setTextColor(if (isAccessibilityEnabled()) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
        }
        layout.addView(accessStatus)

        val enableAccessBtn = Button(this).apply {
            text = "Buka Pengaturan Accessibility"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this@SettingsActivity, 
                    "Cari 'PLA Agent AutoTyper' lalu aktifkan", 
                    Toast.LENGTH_LONG).show()
            }
        }
        layout.addView(enableAccessBtn)

        // Tombol Overlay Permission
        val overlayBtn = Button(this).apply {
            text = "Izinkan Tampil di Atas Aplikasi Lain"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
        }
        layout.addView(overlayBtn)

        setContentView(layout)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { 
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name.contains("AutoTyper")
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh status accessibility saat kembali dari settings
        recreate()
    }
}