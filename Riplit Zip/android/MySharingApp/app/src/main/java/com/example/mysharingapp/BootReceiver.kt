package com.example.mysharingapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Device booted or app updated, checking if service should restart")
                
                // Check if service was previously running
                val sharedPref = context.getSharedPreferences("MySharingApp", Context.MODE_PRIVATE)
                val wasServiceRunning = sharedPref.getBoolean("service_was_running", false)
                
                if (wasServiceRunning) {
                    Log.d(TAG, "Service was previously running, restarting...")
                    startSharingService(context)
                } else {
                    Log.d(TAG, "Service was not running before, not starting automatically")
                }
            }
        }
    }
    
    private fun startSharingService(context: Context) {
        try {
            val serviceIntent = Intent(context, SharingService::class.java)
            serviceIntent.action = SharingService.ACTION_START_SERVICE
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "SharingService started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SharingService: ${e.message}")
        }
    }
}
