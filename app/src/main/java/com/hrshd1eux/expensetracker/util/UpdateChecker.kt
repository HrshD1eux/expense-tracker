package com.hrshd1eux.expensetracker.util

import android.util.Log
import com.hrshd1eux.expensetracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateCheckResult {
    data object Idle : UpdateCheckResult()
    data object Checking : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseTitle: String,
        val releaseNotes: String,
        val releaseHtmlUrl: String,
        val downloadUrl: String?
    ) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/HrshD1eux/expense-tracker/releases/latest"
    const val GITHUB_REPO_URL = "https://github.com/HrshD1eux/expense-tracker"
    const val GITHUB_DEV_PROFILE_URL = "https://github.com/HrshD1eux"

    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "ExpenseTracker-AndroidApp")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext UpdateCheckResult.UpToDate
            }
            if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult.Error("GitHub responded with code $responseCode")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", "New Release")
            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", GITHUB_REPO_URL)

            val remoteCleanVersion = tagName.removePrefix("v").trim()
            val localVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            // Find APK direct download URL if attached
            var apkDownloadUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = if (asset.has("browser_download_url")) asset.getString("browser_download_url") else null
                        break
                    }
                }
            }

            if (isRemoteVersionNewer(remoteCleanVersion, localVersion)) {
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = tagName,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    releaseHtmlUrl = htmlUrl,
                    downloadUrl = apkDownloadUrl ?: htmlUrl
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Failed to check GitHub releases", e)
            UpdateCheckResult.Error(e.localizedMessage ?: "Could not connect to GitHub. Check internet connection.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun isRemoteVersionNewer(remote: String, local: String): Boolean {
        if (remote.isEmpty() || local.isEmpty()) return false
        val rParts = remote.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
