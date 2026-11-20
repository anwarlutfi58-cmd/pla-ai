package com.pla.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat

class HardwareTriggerService : Service() {

    private var mediaSession: MediaSessionCompat? = null
    private var lastVolumeUpTime = 0L
    private val DOUBLE_TAP_THRESHOLD = 500L // ms antar ketukan

    companion object {
        const val CHANNEL_ID = "pla_hardware_trigger"
        const val NOTIF_ID = 1001
        
        fun start(context: Context) {
            val intent = android.content.Intent(context, HardwareTriggerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(android.content.Intent(context, HardwareTriggerService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        setupMediaSession()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PLA Hardware Trigger",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mendengarkan tombol volume untuk memanggil PLA Agent"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PLA Agent Siaga")
            .setContentText("Ketuk Volume Up 2x cepat untuk memanggil mikrofon")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "PLATrigger").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or 
                     MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .build()
            )
            
            setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "PLA Agent Listener")
                    .build()
            )
            
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onMediaButtonEvent(mediaButtonIntent: android.content.Intent): Boolean {
                    val event = mediaButtonIntent.getParcelableExtra<android.view.KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
                    
                    if (event?.action == android.view.KeyEvent.ACTION_DOWN && 
                        event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
                        
                        val now = System.currentTimeMillis()
                        if (now - lastVolumeUpTime < DOUBLE_TAP_THRESHOLD) {
                            FloatingMicOverlay.show(this@HardwareTriggerService)
                            lastVolumeUpTime = 0L
                        } else {
                            lastVolumeUpTime = now
                        }
                        return true // Konsumsi event agar tidak mengubah volume
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })
            
            isActive = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
    }
}