package com.quickpdf.reader.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class for checking app updates from GitHub releases
 */
class UpdateChecker(private val context: Context) {

    companion object {
        private const val TAG = "UpdateChecker"
        // GitHub API endpoint for checking latest releases
        private const val GITHUB_API_URL = "https://api.github.com/repos/akshitharsola/QuickPDF/releases/latest"
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 15000 // 15 seconds
    }

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val currentVersion: String,
        val downloadUrl: String? = null,
        val releaseNotes: String? = null,
        val error: String? = null
    )

    /**
     * Check for updates asynchronously
     */
    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            Log.d(TAG, "Current version: $currentVersion")

            val latestRelease = fetchLatestRelease()
            if (latestRelease == null) {
                return@withContext UpdateInfo(
                    isUpdateAvailable = false,
                    latestVersion = currentVersion,
                    currentVersion = currentVersion,
                    error = "Failed to fetch release information"
                )
            }

            val latestVersion = latestRelease.optString("tag_name", "").removePrefix("v")
            val downloadUrl = getDownloadUrl(latestRelease)
            val releaseNotes = latestRelease.optString("body", "")

            Log.d(TAG, "Latest version: $latestVersion")

            val isUpdateAvailable = isNewerVersion(currentVersion, latestVersion)

            UpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                latestVersion = latestVersion,
                currentVersion = currentVersion,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            UpdateInfo(
                isUpdateAvailable = false,
                latestVersion = getCurrentVersion(),
                currentVersion = getCurrentVersion(),
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    /**
     * Get current app version from BuildConfig
     */
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current version", e)
            "1.0.0"
        }
    }

    /**
     * Fetch latest release information from GitHub API
     */
    private fun fetchLatestRelease(): JSONObject? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECTION_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "QuickPDF-UpdateChecker")
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "GitHub API response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
                JSONObject(response)
            } else {
                Log.e(TAG, "Failed to fetch releases: HTTP $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest release", e)
            null
        }
    }

    /**
     * Extract APK download URL from release assets
     */
    private fun getDownloadUrl(release: JSONObject): String? {
        return try {
            val assets = release.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    return asset.getString("browser_download_url")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting download URL", e)
            null
        }
    }

    /**
     * Compare version strings to determine if an update is available
     */
    private fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        return try {
            val current = parseVersion(currentVersion)
            val latest = parseVersion(latestVersion)
            
            for (i in 0 until maxOf(current.size, latest.size)) {
                val currentPart = current.getOrNull(i) ?: 0
                val latestPart = latest.getOrNull(i) ?: 0
                
                when {
                    latestPart > currentPart -> return true
                    latestPart < currentPart -> return false
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions", e)
            false
        }
    }

    /**
     * Parse version string into list of integers
     */
    private fun parseVersion(version: String): List<Int> {
        return version.split(".")
            .map { it.replace(Regex("[^0-9]"), "") }
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: 0 }
    }

    /**
     * Open download URL in browser or download manager
     */
    fun downloadUpdate(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening download URL", e)
        }
    }

    /**
     * Set the GitHub repository URL for update checking
     */
    fun setRepositoryUrl(@Suppress("UNUSED_PARAMETER") username: String, @Suppress("UNUSED_PARAMETER") repositoryName: String) {
        // This would be implemented if you want to make the repository configurable
        // For now, it's hardcoded in the companion object
    }
}