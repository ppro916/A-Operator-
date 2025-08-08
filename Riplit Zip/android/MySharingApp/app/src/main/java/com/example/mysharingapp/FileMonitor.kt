package com.example.mysharingapp

import android.content.Context
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileMonitor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val deviceId: String
) {
    private val fileObservers = mutableListOf<FileObserver>()
    private val monitoringJob = SupervisorJob()
    private val monitoringScope = CoroutineScope(Dispatchers.IO + monitoringJob)
    private var isMonitoring = false
    
    companion object {
        private const val TAG = "FileMonitor"
        private val MONITORED_DIRECTORIES = listOf(
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_DOCUMENTS
        )
        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", // Images
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", // Videos
            "mp3", "wav", "ogg", "m4a", "flac", "aac", // Audio
            "pdf", "doc", "docx", "txt", "rtf", // Documents
            "zip", "rar", "7z", "tar", "gz" // Archives
        )
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        Log.d(TAG, "Starting file monitoring")
        
        // Monitor external storage directories
        monitorExternalStorageDirectories()
        
        // Perform initial scan
        performInitialScan()
    }
    
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        Log.d(TAG, "Stopping file monitoring")
        
        // Stop all file observers
        fileObservers.forEach { observer ->
            try {
                observer.stopWatching()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping file observer: ${e.message}")
            }
        }
        fileObservers.clear()
        
        // Cancel monitoring job
        monitoringJob.cancel()
    }
    
    private fun monitorExternalStorageDirectories() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG, "External storage not mounted")
            return
        }
        
        MONITORED_DIRECTORIES.forEach { dirType ->
            try {
                val directory = Environment.getExternalStoragePublicDirectory(dirType)
                if (directory.exists() && directory.isDirectory) {
                    monitorDirectory(directory)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up monitoring for $dirType: ${e.message}")
            }
        }
        
        // Also monitor custom app directories
        try {
            val externalDir = context.getExternalFilesDir(null)
            externalDir?.let { monitorDirectory(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error monitoring app external directory: ${e.message}")
        }
    }
    
    private fun monitorDirectory(directory: File) {
        Log.d(TAG, "Setting up monitoring for: ${directory.absolutePath}")
        
        val observer = object : FileObserver(directory.absolutePath, ALL_EVENTS) {
            override fun onEvent(event: Int, path: String?) {
                if (path == null) return
                
                when (event and ALL_EVENTS) {
                    CREATE, MOVED_TO -> {
                        val file = File(directory, path)
                        if (file.isFile && isFileSupported(file)) {
                            handleFileCreated(file)
                        }
                    }
                    MODIFY -> {
                        val file = File(directory, path)
                        if (file.isFile && isFileSupported(file)) {
                            handleFileModified(file)
                        }
                    }
                    DELETE, MOVED_FROM -> {
                        handleFileDeleted(directory.absolutePath + "/" + path)
                    }
                }
            }
        }
        
        observer.startWatching()
        fileObservers.add(observer)
    }
    
    private fun isFileSupported(file: File): Boolean {
        val extension = file.extension.lowercase()
        return SUPPORTED_EXTENSIONS.contains(extension)
    }
    
    private fun handleFileCreated(file: File) {
        Log.d(TAG, "File created: ${file.absolutePath}")
        monitoringScope.launch {
            uploadFileMetadata(file, "created")
        }
    }
    
    private fun handleFileModified(file: File) {
        Log.d(TAG, "File modified: ${file.absolutePath}")
        monitoringScope.launch {
            uploadFileMetadata(file, "modified")
        }
    }
    
    private fun handleFileDeleted(filePath: String) {
        Log.d(TAG, "File deleted: $filePath")
        monitoringScope.launch {
            uploadFileDeletion(filePath)
        }
    }
    
    private suspend fun uploadFileMetadata(file: File, action: String) {
        try {
            val metadata = extractFileMetadata(file, action)
            
            firestore.collection("file_metadata")
                .add(metadata)
                .addOnSuccessListener { documentRef ->
                    Log.d(TAG, "File metadata uploaded: ${documentRef.id}")
                    updateDeviceFileCount()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to upload file metadata: ${e.message}")
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error processing file metadata: ${e.message}")
        }
    }
    
    private fun extractFileMetadata(file: File, action: String): HashMap<String, Any> {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return hashMapOf(
            "device_id" to deviceId,
            "file_name" to file.name,
            "file_path" to file.absolutePath,
            "file_size" to file.length(),
            "file_extension" to file.extension.lowercase(),
            "mime_type" to getMimeType(file),
            "action" to action,
            "last_modified" to file.lastModified(),
            "last_modified_readable" to sdf.format(Date(file.lastModified())),
            "uploaded_at" to com.google.firebase.Timestamp.now(),
            "is_readable" to file.canRead(),
            "is_writable" to file.canWrite(),
            "parent_directory" to (file.parent ?: "unknown"),
            "file_type" to getFileType(file.extension)
        )
    }
    
    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "avi" -> "video/avi"
            "mkv" -> "video/mkv"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            "rar" -> "application/rar"
            else -> "application/octet-stream"
        }
    }
    
    private fun getFileType(extension: String): String {
        return when (extension.lowercase()) {
            in setOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> "image"
            in setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm") -> "video"
            in setOf("mp3", "wav", "ogg", "m4a", "flac", "aac") -> "audio"
            in setOf("pdf", "doc", "docx", "txt", "rtf") -> "document"
            in setOf("zip", "rar", "7z", "tar", "gz") -> "archive"
            else -> "other"
        }
    }
    
    private suspend fun uploadFileDeletion(filePath: String) {
        try {
            val deletionRecord = hashMapOf(
                "device_id" to deviceId,
                "file_path" to filePath,
                "action" to "deleted",
                "deleted_at" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("file_deletions")
                .add(deletionRecord)
                .addOnSuccessListener {
                    Log.d(TAG, "File deletion recorded: $filePath")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to record file deletion: ${e.message}")
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error recording file deletion: ${e.message}")
        }
    }
    
    private fun performInitialScan() {
        monitoringScope.launch {
            Log.d(TAG, "Performing initial file scan")
            
            MONITORED_DIRECTORIES.forEach { dirType ->
                try {
                    val directory = Environment.getExternalStoragePublicDirectory(dirType)
                    if (directory.exists() && directory.isDirectory) {
                        scanDirectory(directory)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during initial scan of $dirType: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun scanDirectory(directory: File) {
        try {
            directory.listFiles()?.forEach { file ->
                if (isActive && file.isFile && isFileSupported(file)) {
                    // Only upload if file is newer than 24 hours (to avoid spam on first run)
                    val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                    if (file.lastModified() > oneDayAgo) {
                        uploadFileMetadata(file, "scanned")
                        delay(100) // Small delay to avoid overwhelming Firebase
                    }
                }
                
                // Recursively scan subdirectories (limited depth)
                if (file.isDirectory && file.name.length > 1) {
                    scanDirectory(file)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory ${directory.absolutePath}: ${e.message}")
        }
    }
    
    private fun updateDeviceFileCount() {
        monitoringScope.launch {
            try {
                // Count total monitored files
                var totalFiles = 0
                MONITORED_DIRECTORIES.forEach { dirType ->
                    val directory = Environment.getExternalStoragePublicDirectory(dirType)
                    if (directory.exists()) {
                        totalFiles += countSupportedFiles(directory)
                    }
                }
                
                val updates = hashMapOf<String, Any>(
                    "file_count" to totalFiles,
                    "last_file_scan" to com.google.firebase.Timestamp.now()
                )
                
                firestore.collection("devices")
                    .document(deviceId)
                    .update(updates)
                    
            } catch (e: Exception) {
                Log.e(TAG, "Error updating device file count: ${e.message}")
            }
        }
    }
    
    private fun countSupportedFiles(directory: File): Int {
        var count = 0
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isFile && isFileSupported(file)) {
                    count++
                } else if (file.isDirectory) {
                    count += countSupportedFiles(file)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting files in ${directory.absolutePath}: ${e.message}")
        }
        return count
    }
}
