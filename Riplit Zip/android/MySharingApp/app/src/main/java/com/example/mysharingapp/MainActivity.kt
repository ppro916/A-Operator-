package com.example.mysharingapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var serviceSwitch: Switch
    private lateinit var permissionsButton: Button
    private lateinit var settingsButton: Button
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val BATTERY_OPTIMIZATION_REQUEST_CODE = 1002
        private const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1003
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        initViews()
        setupClickListeners()
        checkPermissions()
        updateUI()
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        serviceSwitch = findViewById(R.id.serviceSwitch)
        permissionsButton = findViewById(R.id.permissionsButton)
        settingsButton = findViewById(R.id.settingsButton)
    }
    
    private fun setupClickListeners() {
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (allPermissionsGranted()) {
                    startSharingService()
                } else {
                    serviceSwitch.isChecked = false
                    Toast.makeText(this, "All permissions required to start service", Toast.LENGTH_SHORT).show()
                    requestPermissions()
                }
            } else {
                stopSharingService()
            }
        }
        
        permissionsButton.setOnClickListener {
            requestPermissions()
        }
        
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        // Background location permission (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        // Storage permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Check for special permissions
            checkSpecialPermissions()
        }
    }
    
    private fun checkSpecialPermissions() {
        // Check for manage external storage permission (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog()
                return
            }
        }
        
        // Check battery optimization
        if (!isIgnoringBatteryOptimizations()) {
            showBatteryOptimizationDialog()
        }
    }
    
    private fun showManageStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs access to manage all files to monitor file changes. Please grant this permission in the next screen.")
            .setPositiveButton("Grant") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.addCategory("android.intent.category.DEFAULT")
                        intent.data = Uri.parse("package:${applicationContext.packageName}")
                        startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage("To ensure the app works properly in the background, please disable battery optimization for this app.")
            .setPositiveButton("Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open battery settings", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Skip", null)
            .show()
    }
    
    private fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }
    
    private fun allPermissionsGranted(): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun checkPermissions() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasManageStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
        
        val batteryOptimized = isIgnoringBatteryOptimizations()
        
        permissionsButton.isEnabled = !allPermissionsGranted() || !hasManageStoragePermission || !batteryOptimized
        
        if (permissionsButton.isEnabled) {
            permissionsButton.text = "Grant Permissions"
        } else {
            permissionsButton.text = "All Permissions Granted"
        }
    }
    
    private fun updateUI() {
        val serviceRunning = SharingService.isServiceRunning(this)
        serviceSwitch.isChecked = serviceRunning
        
        statusText.text = if (serviceRunning) {
            "Service Status: Running\nLocation tracking and file monitoring active"
        } else {
            "Service Status: Stopped\nNo monitoring active"
        }
        
        checkPermissions()
    }
    
    private fun startSharingService() {
        if (allPermissionsGranted()) {
            val serviceIntent = Intent(this, SharingService::class.java)
            serviceIntent.action = SharingService.ACTION_START_SERVICE
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Toast.makeText(this, "Sharing service started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions required to start service", Toast.LENGTH_SHORT).show()
            serviceSwitch.isChecked = false
        }
    }
    
    private fun stopSharingService() {
        val serviceIntent = Intent(this, SharingService::class.java)
        serviceIntent.action = SharingService.ACTION_STOP_SERVICE
        startService(serviceIntent)
        
        Toast.makeText(this, "Sharing service stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("App Settings")
            .setMessage("Configure monitoring settings and view app information.")
            .setPositiveButton("App Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNeutralButton("Location Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                
                if (allGranted) {
                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                    checkSpecialPermissions()
                } else {
                    Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
                }
                
                updateUI()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            BATTERY_OPTIMIZATION_REQUEST_CODE,
            MANAGE_EXTERNAL_STORAGE_REQUEST_CODE -> {
                updateUI()
            }
        }
    }
}
