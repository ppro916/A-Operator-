package com.example.mysharingapp

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import android.content.Context
import org.json.JSONObject
import java.io.InputStream

object FirebaseUtils {
    fun initFirebase(context: Context) {
        // Read credentials.json from assets
        val inputStream: InputStream = context.assets.open("credentials.json")
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        val json = JSONObject(String(buffer, Charsets.UTF_8))

        val options = FirebaseOptions.Builder()
            .setApiKey(json.getString("firebase_api_key"))
            .setApplicationId(json.getString("firebase_app_id"))
            .setProjectId(json.getString("firebase_project_id"))
            .setStorageBucket(json.getString("firebase_storage_bucket"))
            .build()

        FirebaseApp.initializeApp(context, options)
    }
}// Placeholder FirebaseUtils.kt — original implementation should be placed here
