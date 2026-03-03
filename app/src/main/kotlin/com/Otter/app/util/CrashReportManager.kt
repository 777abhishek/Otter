package com.Otter.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReportManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val crashFile = File(File(context.filesDir, "crash"), "last_crash.txt")

        fun writeCrash(throwable: Throwable) {
            kotlin.runCatching {
                crashFile.parentFile?.mkdirs()
                crashFile.writeText(throwable.stackTraceToString())
            }
        }

        fun getCrashContent(): String {
            return kotlin.runCatching {
                if (!crashFile.exists()) return@runCatching ""
                crashFile.readText()
            }.getOrDefault("")
        }

        fun hasCrash(): Boolean = crashFile.exists() && crashFile.length() > 0

        fun clearCrash() {
            kotlin.runCatching {
                crashFile.delete()
            }
        }
    }
