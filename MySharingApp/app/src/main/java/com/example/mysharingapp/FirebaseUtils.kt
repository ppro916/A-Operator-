package com.example.mysharingapp

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

object FirebaseUtils {

    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun uploadFileMetadata(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val metadata = hashMapOf(
            "name" to file.name,
            "path" to file.absolutePath,
            "size" to file.length(),
            "lastModified" to file.lastModified()
        )

        database.getReference("files")
            .push()
            .setValue(metadata)
    }

    fun uploadFileToStorage(filePath: String, onComplete: (Boolean) -> Unit) {
        val file = File(filePath)
        if (!file.exists()) {
            onComplete(false)
            return
        }

        val storageRef = storage.reference.child("uploads/${file.name}")
        val uploadTask = storageRef.putFile(android.net.Uri.fromFile(file))

        uploadTask.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }
}
