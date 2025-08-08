package com.example.mysharingapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.util.*

class SharingService : Service() {
    
    private lateinit var locationTracker: LocationTracker
    private lateinit var fileMonitor: FileMonitor
    private lateinit var firestore: FirebaseFirestore
    private lateinit var wakeLock: PowerManager.WakeLock
    
    private var serviceJob: Job? = null
    private var deviceId: String = ""
    private var isServiceRunning = false
    
    companion object {
        const val ACTION_START_SERVICE = "START_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "MySharingAppChannel"
        
        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (SharingService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        
        // Generate unique device ID
        deviceId = generateDeviceId()
        
        // Initialize components
        locationTracker = LocationTracker(this, firestore, deviceId)
        fileMonitor = FileMonitor(this, firestore, deviceId)
        
        // Initialize wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MySharingApp::SharingServiceWakeLock"
        )
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> startSharingService()
            ACTION_STOP_SERVICE -> stopSharingService()
        }
        
        return START_STICKY // Restart service if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startSharingService() {
        if (isServiceRunning) return
        
        isServiceRunning = true
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification("Starting sharing service..."))
        
        // Acquire wake lock
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L) // 10 minutes
        }
        
        // Start coroutine job
        serviceJob = CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            try {
                // Register device in Firebase
                registerDevice()
                
                // Start location tracking
                locationTracker.startTracking()
                
                // Start file monitoring
                fileMonitor.startMonitoring()
                
                // Update notification
                updateNotification("Sharing service active")
                
                // Keep service alive and monitor Firebase for stop commands
                monitorFirebaseCommands()
                
            } catch (e: Exception) {
                updateNotification("Service error: ${e.message}")
            }
        }
    }
    
    private fun stopSharingService() {
        isServiceRunning = false
        
        // Cancel the job
        serviceJob?.cancel()
        
        // Stop tracking and monitoring
        locationTracker.stopTracking()
        fileMonitor.stopMonitoring()
        
        // Update device status in Firebase
        updateDeviceStatus(false)
        
        // Release wake lock
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        
        // Stop foreground service
        stopForeground(true)
        stopSelf()
    }
    
    private suspend fun registerDevice() {
        val deviceInfo = hashMapOf(
            "device_id" to deviceId,
            "device_name" to getDeviceName(),
            "android_version" to Build.VERSION.RELEASE,
            "app_version" to getAppVersion(),
            "sharing_active" to true,
            "last_seen" to com.google.firebase.Timestamp.now(),
            "online" to true
        )
        
        try {
            firestore.collection("devices")
                .document(deviceId)
                .set(deviceInfo)
                .addOnSuccessListener {
                    updateNotification("Device registered successfully")
                }
                .addOnFailureListener { e ->
                    updateNotification("Failed to register device: ${e.message}")
                }
        } catch (e: Exception) {
            updateNotification("Registration error: ${e.message}")
        }
    }
    
    private suspend fun monitorFirebaseCommands() {
        while (isServiceRunning) {
            try {
                // Listen for stop commands from web interface
                firestore.collection("devices")
                    .document(deviceId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val sharingActive = document.getBoolean("sharing_active") ?: true
                            if (!sharingActive && isServiceRunning) {
                                // Stop command received from web interface
                                stopSharingService()
                            }
                        }
                    }
                
                // Update last seen timestamp
                updateLastSeen()
                
                delay(30000) // Check every 30 seconds
                
            } catch (e: Exception) {
                delay(60000) // Wait longer on error
            }
        }
    }
    
    private fun updateLastSeen() {
        val updates = hashMapOf<String, Any>(
            "last_seen" to com.google.firebase.Timestamp.now(),
            "online" to true
        )
        
        firestore.collection("devices")
            .document(deviceId)
            .update(updates)
    }
    
    private fun updateDeviceStatus(active: Boolean) {
        val updates = hashMapOf<String, Any>(
            "sharing_active" to active,
            "last_seen" to com.google.firebase.Timestamp.now(),
            "online" to active
        )
        
        if (!active) {
            updates["stopped_at"] = com.google.firebase.Timestamp.now()
        }
        
        firestore.collection("devices")
            .document(deviceId)
            .update(updates)
    }
    
    private fun generateDeviceId(): String {
        val sharedPref = getSharedPreferences("MySharingApp", Context.MODE_PRIVATE)
        var deviceId = sharedPref.getString("device_id", null)
        
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPref.edit().putString("device_id", deviceId).apply()
        }
        
        return deviceId
    }
    
    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MySharingApp Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for MySharingApp background service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(content: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MySharingApp")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        isServiceRunning = false
        serviceJob?.cancel()
        
        locationTracker.stopTracking()
        fileMonitor.stopMonitoring()
        
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        
        updateDeviceStatus(false)
    }
}
