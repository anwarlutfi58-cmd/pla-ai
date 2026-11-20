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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferences: AppPreferences
    private var accessStatusView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)
        
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

        val aiNameInput = EditText(this).apply {
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
                if (isChecked) {
                    // Cek permission dulu sebelum aktifkan
                    if (!hasAllPermissions()) {
                        isChecked = false
                        requestMissingPermissions()
                        return@setOnCheckedChangeListener
                    }
                    preferences.setHardwareTriggerEnabled(true)
                    HardwareTriggerService.start(this@SettingsActivity)
                    Toast.makeText(this@SettingsActivity, "Fitur aktif! Ketuk Volume Up 2x.", Toast.LENGTH_LONG).show()
                } else {
                    preferences.setHardwareTriggerEnabled(false)
                    HardwareTriggerService.stop(this@SettingsActivity)
                    Toast.makeText(this@SettingsActivity, "Fitur dimatikan.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(hardwareSwitch)

        // Status Accessibility
        accessStatusView = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 8)
        }
        updateAccessStatus()
        layout.addView(accessStatusView)

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

    private fun updateAccessStatus() {
        accessStatusView?.apply {
            text = if (isAccessibilityEnabled()) 
                "✅ AutoTyper AKTIF - Siap mengetik otomatis" 
            else 
                "❌ AutoTyper BELUM AKTIF - Klik tombol di bawah untuk mengaktifkan"
            setTextColor(if (isAccessibilityEnabled()) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { 
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name.contains("AutoTyper")
        }
    }

    private fun hasAllPermissions(): Boolean {
        val overlayOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
        
        val audioOk = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        
        return overlayOk && audioOk && notifOk
    }

    private fun requestMissingPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 100)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan 'Tampil di atas aplikasi lain' dulu", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }

    override fun onResume() {
        super.onResume()
        // Hanya refresh status, TIDAK recreate() agar tidak infinite loop
        updateAccessStatus()
    }
}