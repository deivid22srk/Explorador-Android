package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.model.ExtractionManager
import com.example.model.ZipFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class ExtractionService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val CHANNEL_ID = "extraction_service_channel"
        const val NOTIFICATION_ID = 4242

        const val ACTION_START_EXTRACTION = "com.example.action.START_EXTRACTION"
        const val EXTRA_ARCHIVE_PATH = "com.example.extra.ARCHIVE_PATH"
        const val EXTRA_TARGET_FOLDER = "com.example.extra.TARGET_FOLDER"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_START_EXTRACTION) {
            val archivePath = intent.getStringExtra(EXTRA_ARCHIVE_PATH) ?: ""
            val targetFolder = intent.getStringExtra(EXTRA_TARGET_FOLDER) ?: ""

            if (archivePath.isNotEmpty() && targetFolder.isNotEmpty()) {
                val file = File(archivePath)
                val archiveName = file.name

                // Start Foreground Immediately
                val notification = buildProgressNotification(archiveName, 0, 100, "Iniciando extração...")
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID, 
                            notification, 
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Run extraction asynchronously
                serviceScope.launch {
                    ExtractionManager.updateProgress(archiveName, 0, 100, "A ler o arquivo...")
                    val result = ZipFileSystem.extractAllWithProgress(archivePath, targetFolder) { current, total, entryName ->
                        // Update ExtractionManager
                        ExtractionManager.updateProgress(archiveName, current, total, entryName)

                        // Update Notification
                        val progressPercent = if (total > 0) (current * 100) / total else 0
                        val updateNotification = buildProgressNotification(archiveName, progressPercent, 100, entryName)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, updateNotification)
                    }

                    if (result) {
                        ExtractionManager.setSuccess(archiveName)
                        showCompletionNotification(archiveName, "Extração de '$archiveName' concluída com sucesso!")
                    } else {
                        ExtractionManager.setError(archiveName, "Ocorreu um erro durante a extração do arquivo.")
                        showCompletionNotification(archiveName, "Erro ao extrair o arquivo '$archiveName'.")
                    }

                    stopForeground(true)
                    stopSelf()
                }
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildProgressNotification(archiveName: String, progressPercent: Int, max: Int, subText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Extraindo Arquivo")
            .setContentText(archiveName)
            .setSubText(subText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(max, progressPercent, false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .build()
    }

    private fun showCompletionNotification(archiveName: String, message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Extração Concluída")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal de Extração de Arquivos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de progresso para a extração de arquivos"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
