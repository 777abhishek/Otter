package com.Otter.app.util

/**
 * Classified update errors for better UX and error handling
 */
sealed class UpdateError(open val message: String, open val userMessage: String) {
    
    /**
     * User needs to enable "Install from Unknown Sources" permission
     */
    data class PermissionRequired(val canOpenSettings: Boolean = true) : UpdateError(
        message = "Install from Unknown Sources permission required",
        userMessage = "Please enable 'Install from Unknown Sources' in settings. " +
                      "Go to Settings > Apps > Special app access > Install unknown apps > This app > Allow"
    )
    
    /**
     * Network connectivity issue
     */
    data class NetworkError(val cause: Exception) : UpdateError(
        message = "Network error: ${cause.message}",
        userMessage = "Network connection failed. Check your internet and try again. " +
                      "You can retry and resume the download."
    )
    
    /**
     * Failed to download APK file
     */
    data class DownloadError(val cause: Exception) : UpdateError(
        message = "Download failed: ${cause.message}",
        userMessage = "Failed to download the update. Please try again."
    )
    
    /**
     * No APK asset found in the release
     */
    data class NoAssetFound(val releaseTag: String) : UpdateError(
        message = "No APK asset found in release $releaseTag",
        userMessage = "Update not available for your device. Please check GitHub releases."
    )
    
    /**
     * Failed to start installation
     */
    data class InstallationError(val cause: Exception) : UpdateError(
        message = "Installation failed: ${cause.message}",
        userMessage = "Could not start the installation. Try again or install manually."
    )
    
    /**
     * Downloaded file is corrupted
     */
    data class CorruptFile(val expectedSize: Long, val actualSize: Long) : UpdateError(
        message = "Downloaded file corrupted (expected $expectedSize bytes, got $actualSize)",
        userMessage = "The downloaded file is corrupted. Deleting cached file and starting fresh download."
    )
    
    /**
     * Generic/unknown error
     */
    data class Unknown(val cause: Exception) : UpdateError(
        message = cause.message ?: "Unknown error",
        userMessage = "An unexpected error occurred. Please try again."
    )
    
    companion object {
        fun wrap(exception: Exception): UpdateError {
            return when (exception) {
                is UpdateError -> exception
                else -> Unknown(exception)
            }
        }
    }
}

/**
 * Extension to convert exceptions to UpdateError
 */
fun Exception.toUpdateError(): UpdateError {
    return when {
        this is UpdateError -> this
        this.message?.contains("permission", ignoreCase = true) == true ->
            UpdateError.PermissionRequired()
        this.message?.contains("network", ignoreCase = true) == true ->
            UpdateError.NetworkError(this)
        this.message?.contains("download", ignoreCase = true) == true ->
            UpdateError.DownloadError(this)
        else -> UpdateError.Unknown(this)
    }
}
