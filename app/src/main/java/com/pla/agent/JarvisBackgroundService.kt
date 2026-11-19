package com.pla.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class JarvisBackgroundService : Service() {

    private val CHANNEL_ID = "JarvisForegroundChannel"
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Tampilkan notifikasi permanen
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PLA Agent Aktif")
            .setContentText("Jarvis sedang siaga. AI belajar di latar belakang.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Tidak bisa di-swipe
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Nyalakan juga gelembung Jarvis (jika belum nyala)
        val floatingServiceIntent = Intent(this, FloatingService::class.java)
        startService(floatingServiceIntent)

        return START_STICKY // Meminta Android untuk menghidupkan ulang service jika dimatikan sistem
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "PLA Agent Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}