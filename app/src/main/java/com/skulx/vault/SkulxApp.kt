package com.skulx.vault

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SkulxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "skulx_downloads",
                "Skulx Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active download notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
