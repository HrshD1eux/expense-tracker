package com.hrshd1eux.expensetracker.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.hrshd1eux.expensetracker.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% Offline, Privacy-First Crash and Diagnostics Logger.
 *
 * Catches uncaught fatal exceptions and records stack traces strictly to
 * the app's private files directory (crash_logs.txt). Zero network transmission.
 * Allows users to inspect and copy crash logs when filing issues on GitHub.
 */
object CrashLogger {

    private const val LOG_FILE_NAME = "crash_logs.txt"
    private const val MAX_LOG_SIZE_BYTES = 256 * 1024 // 256 KB cap

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(context, thread, throwable)
            } catch (e: Exception) {
                Log.e("CrashLogger", "Failed to write crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val logFile = File(context.filesDir, LOG_FILE_NAME)

        // Rotate if exceeding max size
        if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
            logFile.delete()
        }

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = buildString {
            appendLine("=== FATAL EXCEPTION ===")
            appendLine("Timestamp: $timeStr")
            appendLine("App Version: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT}, Android ${Build.VERSION.RELEASE})")
            appendLine("Thread: ${thread.name} (id: ${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
            appendLine("Stack Trace:")
            appendLine(stackTrace)
            appendLine("========================")
            appendLine()
        }

        logFile.appendText(entry)
    }

    fun getCrashLogs(context: Context): String {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        return if (logFile.exists()) {
            try {
                logFile.readText()
            } catch (e: Exception) {
                "Failed to read logs: ${e.message}"
            }
        } else {
            ""
        }
    }

    fun clearCrashLogs(context: Context) {
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        if (logFile.exists()) {
            logFile.delete()
        }
    }
}
