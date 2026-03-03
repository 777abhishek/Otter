package com.Otter.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileLogger
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val logFile = File(context.filesDir, "log.txt")
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        fun log(
            tag: String,
            message: String,
        ) {
            try {
                val timestamp = dateFormat.format(Date())
                val logLine = "[$timestamp] [$tag] $message\n"
                logFile.appendText(logLine)
            } catch (e: Exception) {
                // Silently fail to avoid crashing the app
            }
        }

        fun logError(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) {
            try {
                val timestamp = dateFormat.format(Date())
                val logLine = "[$timestamp] [$tag] ERROR: $message\n${throwable?.stackTraceToString() ?: ""}\n"
                logFile.appendText(logLine)
            } catch (e: Exception) {
                // Silently fail to avoid crashing the app
            }
        }

        fun clearLog() {
            try {
                logFile.delete()
            } catch (e: Exception) {
                // Silently fail
            }
        }

        fun getLogContent(): String {
            return try {
                if (!logFile.exists()) return ""
                logFile.readText()
            } catch (e: Exception) {
                ""
            }
        }
    }
