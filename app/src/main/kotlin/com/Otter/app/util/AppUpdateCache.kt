package com.Otter.app.util

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlin.math.absoluteValue

/**
 * Intelligent update cache management with versioning and expiration
 * Prevents redundant downloads and manages disk space efficiently
 */
object AppUpdateCache {
    private const val CACHE_DIR = "app_updates"
    private const val CACHE_METADATA = "cache_metadata.json"
    private const val CACHE_TTL_DAYS = 7
    private const val MAX_CACHE_SIZE_MB = 500
    
    data class CachedUpdate(
        val tagName: String,
        val fileName: String,
        val fileSize: Long,
        val downloadedAt: Long,
        val downloadUrl: String,
    )
    
    data class CacheMetadata(
        val currentUpdate: CachedUpdate? = null,
        val lastCleanup: Long = 0,
    )
    
    /**
     * Get cached APK if it exists, is valid, and matches the requested release
     */
    fun getCachedApk(context: Context, release: AppUpdateUtil.Release): File? {
        val metadata = getMetadata(context) ?: return null
        val cached = metadata.currentUpdate ?: return null
        
        // Check if cached version matches requested release
        if (cached.tagName != release.tagName) {
            android.util.Log.d("AppUpdateCache", "Cached version ${cached.tagName} != requested ${release.tagName}")
            return null
        }
        
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        val file = File(cacheDir, cached.fileName)
        
        // Verify file exists and size matches
        if (!file.exists()) {
            android.util.Log.w("AppUpdateCache", "Cached file not found: ${file.absolutePath}")
            return null
        }
        
        if (file.length() != cached.fileSize) {
            android.util.Log.w("AppUpdateCache", 
                "File size mismatch: expected ${cached.fileSize}, got ${file.length()}")
            file.delete()
            return null
        }
        
        // Check cache expiration
        val ageMs = System.currentTimeMillis() - cached.downloadedAt
        val expiredMs = CACHE_TTL_DAYS * 24 * 60 * 60 * 1000L
        if (ageMs > expiredMs) {
            android.util.Log.d("AppUpdateCache", "Cached file expired, deleting")
            file.delete()
            return null
        }
        
        android.util.Log.d("AppUpdateCache", "Using cached APK: ${file.absolutePath}")
        return file
    }
    
    /**
     * Save newly downloaded APK to cache metadata
     */
    fun cacheApk(
        context: Context,
        asset: AppUpdateUtil.Asset,
        release: AppUpdateUtil.Release,
        file: File,
    ) {
        try {
            val update = CachedUpdate(
                tagName = release.tagName,
                fileName = file.name,
                fileSize = file.length(),
                downloadedAt = System.currentTimeMillis(),
                downloadUrl = asset.downloadUrl,
            )
            
            val metadata = CacheMetadata(
                currentUpdate = update,
                lastCleanup = System.currentTimeMillis(),
            )
            
            saveMetadata(context, metadata)
            android.util.Log.d("AppUpdateCache", "Cached APK: ${file.name}")
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateCache", "Failed to save cache metadata: ${e.message}")
        }
    }
    
    /**
     * Clear expired cached files and enforce size limits
     */
    fun clearExpired(context: Context) {
        try {
            val metadata = getMetadata(context) ?: return
            val now = System.currentTimeMillis()
            
            // Only perform cleanup once per hour to avoid excessive I/O
            val timeSinceLastCleanup = now - metadata.lastCleanup
            if (timeSinceLastCleanup < 3600000) return
            
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) return
            
            val expiredMs = CACHE_TTL_DAYS * 24 * 60 * 60 * 1000L
            var totalSize = 0L
            var deletedCount = 0
            
            // Delete expired files and collect size stats
            cacheDir.listFiles()?.forEach { file ->
                val fileAge = now - file.lastModified()
                
                // Delete if expired
                if (fileAge > expiredMs) {
                    file.delete()
                    deletedCount++
                    android.util.Log.d("AppUpdateCache", "Deleted expired file: ${file.name}")
                } else {
                    totalSize += file.length()
                }
            }
            
            // Delete oldest files if total size exceeds limit
            val maxSizeBytes = MAX_CACHE_SIZE_MB * 1024L * 1024L
            if (totalSize > maxSizeBytes) {
                android.util.Log.w("AppUpdateCache", "Cache size $totalSize exceeds limit $maxSizeBytes")
                
                cacheDir.listFiles()
                    ?.sortedBy { it.lastModified() }
                    ?.forEach { file ->
                        if (totalSize <= maxSizeBytes) return@forEach
                        file.delete()
                        totalSize -= file.length()
                        deletedCount++
                        android.util.Log.d("AppUpdateCache", "Deleted file for size limit: ${file.name}")
                    }
            }
            
            // Update last cleanup time
            saveMetadata(context, metadata.copy(lastCleanup = now))
            android.util.Log.d("AppUpdateCache", "Cleanup complete: deleted $deletedCount files, size=${totalSize / (1024*1024)}MB")
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateCache", "Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * Get absolute size of all cached files in bytes
     */
    fun getCacheSizeBytes(context: Context): Long {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (!cacheDir.exists()) return 0L
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Clear all cached updates
     */
    fun clearAll(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            cacheDir.deleteRecursively()
            
            // Clear metadata
            val metadataFile = File(context.cacheDir, CACHE_METADATA)
            if (metadataFile.exists()) {
                metadataFile.delete()
            }
            
            android.util.Log.d("AppUpdateCache", "Cleared all cached updates")
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateCache", "Error clearing cache: ${e.message}")
        }
    }
    
    private fun getMetadata(context: Context): CacheMetadata? {
        return try {
            val metadataFile = File(context.cacheDir, CACHE_METADATA)
            if (!metadataFile.exists()) return CacheMetadata()
            
            val json = metadataFile.readText()
            val obj = JSONObject(json)
            
            val currentObj = obj.optJSONObject("currentUpdate")
            val current = if (currentObj != null) {
                CachedUpdate(
                    tagName = currentObj.optString("tagName"),
                    fileName = currentObj.optString("fileName"),
                    fileSize = currentObj.optLong("fileSize"),
                    downloadedAt = currentObj.optLong("downloadedAt"),
                    downloadUrl = currentObj.optString("downloadUrl"),
                )
            } else null
            
            CacheMetadata(
                currentUpdate = current,
                lastCleanup = obj.optLong("lastCleanup", 0L),
            )
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateCache", "Failed to load cache metadata: ${e.message}")
            null
        }
    }
    
    private fun saveMetadata(context: Context, metadata: CacheMetadata) {
        try {
            val obj = JSONObject()
            
            if (metadata.currentUpdate != null) {
                val cur = metadata.currentUpdate
                obj.put("currentUpdate", JSONObject().apply {
                    put("tagName", cur.tagName)
                    put("fileName", cur.fileName)
                    put("fileSize", cur.fileSize)
                    put("downloadedAt", cur.downloadedAt)
                    put("downloadUrl", cur.downloadUrl)
                })
            }
            obj.put("lastCleanup", metadata.lastCleanup)
            
            val metadataFile = File(context.cacheDir, CACHE_METADATA)
            metadataFile.writeText(obj.toString(2))
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateCache", "Failed to save cache metadata: ${e.message}")
        }
    }
}
