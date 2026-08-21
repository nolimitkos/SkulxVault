package com.skulx.vault

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Environment
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
        val title = intent?.getStringExtra("title") ?: "Video"
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(50)
        val ext = when {
            url.contains(".m3u8") -> ".m3u8"
            url.contains(".webm") -> ".webm"
            url.contains(".ts") -> ".ts"
            else -> ".mp4"
        }
        val filename = "${cleanTitle}_skulx$ext"

        startForeground(startId, buildNotification("Starting: $filename", 0))

        thread {
            try {
                val connection = URL(url).openConnection()
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36")
                connection.setRequestProperty("Accept", "*/*")
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                val total = connection.contentLength
                val input = connection.getInputStream()

                val outDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SkulxVault")
                outDir.mkdirs()
                val outFile = File(outDir, filename)
                val output = FileOutputStream(outFile)

                val buffer = ByteArray(8192)
                var downloaded = 0
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    val progress = if (total > 0) (downloaded * 100 / total) else -1
                    updateNotification(startId, "Downloading: $filename", progress)
                }

                output.flush()
                output.close()
                input.close()

                val doneNotif = NotificationCompat.Builder(this, "skulx_downloads")
                    .setContentTitle("Skulx Vault")
                    .setContentText("Done: $filename")
                    .setSmallIcon(android.R.drawable.ic_menu_save)
                    .setAutoCancel(true)
                    .build()
                startForeground(startId, doneNotif)

            } catch (e: Exception) {
                val failNotif = NotificationCompat.Builder(this, "skulx_downloads")
                    .setContentTitle("Skulx Vault")
                    .setContentText("Failed: $filename")
                    .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                    .build()
                startForeground(startId, failNotif)
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, "skulx_downloads")
            .setContentTitle("Skulx Vault")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentIntent(pending)
            .setOngoing(true)
        if (progress in 0..100) builder.setProgress(100, progress, false)
        return builder.build()
    }

    private fun updateNotification(id: Int, title: String, progress: Int) {
        startForeground(id, buildNotification(title, progress))
    }
}
