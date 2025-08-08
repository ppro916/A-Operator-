package com.example.mysharingapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class LocationTracker(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val deviceId: String
) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var locationCallback: LocationCallback? = null
    private var trackingJob: Job? = null
    private var isTracking = false
    
    companion object {
        private const val LOCATION_UPDATE_INTERVAL = 300000L // 5 minutes
        private const val LOCATION_FASTEST_INTERVAL = 60000L // 1 minute
    }
    
    init {
        initializeLocationServices()
    }
    
    private fun initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    uploadLocationToFirebase(location)
                }
            }
        }
    }
    
    fun startTracking() {
        if (isTracking) return
        
        if (!hasLocationPermissions()) {
            return
        }
        
        isTracking = true
        
        try {
            locationCallback?.let { callback ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            }
            
            // Also get last known location immediately
            getLastKnownLocation()
            
        } catch (e: SecurityException) {
            isTracking = false
        }
    }
    
    fun stopTracking() {
        if (!isTracking) return
        
        isTracking = false
        trackingJob?.cancel()
        
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun getLastKnownLocation() {
        if (!hasLocationPermissions()) return
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        uploadLocationToFirebase(it)
                    }
                }
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }
    
    private fun uploadLocationToFirebase(location: Location) {
        val locationData = hashMapOf(
            "device_id" to deviceId,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "altitude" to location.altitude,
            "speed" to location.speed,
            "bearing" to location.bearing,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "provider" to (location.provider ?: "unknown")
        )
        
        // Add location to locations collection
        firestore.collection("locations")
            .add(locationData)
            .addOnSuccessListener {
                // Update device's last location
                updateDeviceLocation(location)
            }
            .addOnFailureListener { e ->
                // Handle error
            }
    }
    
    private fun updateDeviceLocation(location: Location) {
        val deviceUpdates = hashMapOf<String, Any>(
            "last_latitude" to location.latitude,
            "last_longitude" to location.longitude,
            "last_location_accuracy" to location.accuracy,
            "last_location_time" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection("devices")
            .document(deviceId)
            .update(deviceUpdates)
    }
}
