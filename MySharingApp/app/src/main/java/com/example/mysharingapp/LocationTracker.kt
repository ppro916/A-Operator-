package com.example.mysharingapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class LocationTracker(private val context: Context) : LocationListener {

    private var locationManager: LocationManager? = null

    @SuppressLint("MissingPermission")
    fun startTracking() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            10000L,
            0f,
            this
        )
        locationManager?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            10000L,
            0f,
            this
        )
    }

    override fun onLocationChanged(location: Location) {
        val locationData = hashMapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance()
            .getReference("locations")
            .push()
            .setValue(locationData)

        Log.d("LocationTracker", "Location updated: $locationData")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
// Placeholder LocationTracker.kt — original implementation should be placed here
