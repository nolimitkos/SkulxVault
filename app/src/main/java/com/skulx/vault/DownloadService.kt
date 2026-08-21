package com.skulx.vault

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread

class DownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val title = intent.getStringExtra("title") ?: "Download"
        val filename = title.replace(Regex("[^a-zA-Z0-9.-]"), "_") + ".mp4"

        startForeground(startId, buildNotification(title, 0))

        thread {
            try {
                val connection = URL(url).openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                val total = connection.contentLength
                val input = connection.getInputStream()
                val outDir = File(getExternalFilesDir(null), "SkulxVault")
                outDir.mkdirs()
                val outFile = File(outDir, filename)
                val output = FileOutputStream(outFile)

                val buffer = ByteArray(8192)
                var downloaded = 0
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    val progress = if (total > 0) (downloaded * 100 / total) else 0
                    updateNotification(startId, title, progress)
                }

                output.flush()
                output.close()
                input.close()

                updateNotification(startId, "$title - Done", 100)
                stopForeground(false)
            } catch (e: Exception) {
                updateNotification(startId, "$title - Failed", 0)
                stopForeground(false)
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "skulx_downloads")
            .setContentTitle("Skulx Vault")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentIntent(pending)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(id: Int, title: String, progress: Int) {
        val notification = buildNotification(title, progress)
        startForeground(id, notification)
    }
}
