package com.example.edgelighting

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class EdgeLightingApp : Application() {
    companion object {
        const val CHANNEL_ID = "edge_lighting_overlay_channel"
        lateinit var instance: EdgeLightingApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Edge Lighting Active Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background overlay rendering for RGB edge lighting"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
