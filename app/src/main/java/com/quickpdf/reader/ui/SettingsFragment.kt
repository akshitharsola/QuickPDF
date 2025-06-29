package com.quickpdf.reader.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.quickpdf.reader.R
import com.quickpdf.reader.data.repository.SimpleRepository
import com.quickpdf.reader.utils.UpdateChecker
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var updateChecker: UpdateChecker
    private lateinit var repository: SimpleRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        initializeComponents()
        setupPreferenceClickListeners()
        updateVersionSummary()
    }

    private fun initializeComponents() {
        updateChecker = UpdateChecker(requireContext())
        repository = SimpleRepository() // You might want to inject this properly
    }

    private fun setupPreferenceClickListeners() {
        // Check for updates preference
        findPreference<Preference>("check_for_updates")?.setOnPreferenceClickListener {
            checkForUpdatesManually()
            true
        }

        // Clear recent files preference
        findPreference<Preference>("clear_recent_files")?.setOnPreferenceClickListener {
            showClearRecentFilesDialog()
            true
        }

        // Clear bookmarks preference
        findPreference<Preference>("clear_bookmarks")?.setOnPreferenceClickListener {
            showClearBookmarksDialog()
            true
        }

        // Version preference - show detailed version info
        findPreference<Preference>("version")?.setOnPreferenceClickListener {
            showVersionInfo()
            true
        }

        // Open source licenses preference
        findPreference<Preference>("open_source")?.setOnPreferenceClickListener {
            showOpenSourceLicenses()
            true
        }
    }

    private fun updateVersionSummary() {
        val versionPreference = findPreference<Preference>("version")
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0.0"
            val versionCode = packageInfo.longVersionCode
            versionPreference?.summary = getString(R.string.current_version, versionName)
        } catch (e: Exception) {
            versionPreference?.summary = getString(R.string.current_version, "1.0.0")
        }
    }

    private fun checkForUpdatesManually() {
        val progressDialog = createProgressDialog()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                val updateInfo = updateChecker.checkForUpdates()
                progressDialog.dismiss()
                
                if (updateInfo.error != null) {
                    showUpdateErrorDialog(updateInfo.error)
                } else if (updateInfo.isUpdateAvailable) {
                    showUpdateAvailableDialog(updateInfo)
                } else {
                    showNoUpdatesDialog()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                showUpdateErrorDialog(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun createProgressDialog(): ProgressDialog {
        return ProgressDialog(requireContext()).apply {
            setMessage(getString(R.string.checking_for_updates))
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
        }
    }

    private fun showUpdateAvailableDialog(updateInfo: UpdateChecker.UpdateInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.update_available)
            .setMessage(getString(R.string.update_available_message, updateInfo.latestVersion))
            .setPositiveButton(R.string.download_update) { _, _ ->
                updateInfo.downloadUrl?.let { url ->
                    updateChecker.downloadUpdate(url)
                }
            }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }

    private fun showNoUpdatesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.no_updates_available)
            .setMessage(R.string.no_updates_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showUpdateErrorDialog(error: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.update_check_failed)
            .setMessage(getString(R.string.update_check_failed_message) + "\n\n" + error)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showClearRecentFilesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage("This will remove all files from your recent files list. Are you sure?")
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    repository.clearAllRecentFiles()
                    Toast.makeText(requireContext(), "Recent files cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showClearBookmarksDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setMessage("This will remove all your saved bookmarks. Are you sure?")
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    repository.clearAllBookmarks()
                    Toast.makeText(requireContext(), "All bookmarks cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showVersionInfo() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0.0"
            val versionCode = packageInfo.longVersionCode
            val packageName = packageInfo.packageName
            
            val message = """
                Version: $versionName
                Build: $versionCode
                Package: $packageName
                
                QuickPDF - Lightweight PDF Viewer
                Built with ❤️ for Android
            """.trimIndent()
            
            AlertDialog.Builder(requireContext())
                .setTitle("About QuickPDF")
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error retrieving version information", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOpenSourceLicenses() {
        val licenses = """
            QuickPDF uses the following open source libraries:
            
            • PhotoView by Chris Banes
              https://github.com/chrisbanes/PhotoView
              License: Apache License 2.0
            
            • AndroidX Libraries
              https://developer.android.com/jetpack/androidx
              License: Apache License 2.0
            
            • Kotlin
              https://kotlinlang.org/
              License: Apache License 2.0
            
            • Material Components
              https://material.io/
              License: Apache License 2.0
        """.trimIndent()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Open Source Licenses")
            .setMessage(licenses)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        
        // Check for updates automatically if enabled
        val autoCheck = preferenceManager.sharedPreferences?.getBoolean("auto_check_updates", true) ?: true
        if (autoCheck) {
            checkForUpdatesInBackground()
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "auto_check_updates" -> {
                val enabled = sharedPreferences?.getBoolean(key, true) ?: true
                // You can add logic here to enable/disable background update checking
            }
            "night_mode_default" -> {
                // Handle night mode preference change
                val enabled = sharedPreferences?.getBoolean(key, false) ?: false
                // You might want to apply this change globally
            }
            "high_quality_rendering" -> {
                // Handle rendering quality preference change
                val enabled = sharedPreferences?.getBoolean(key, true) ?: true
                // You might want to notify other components about this change
            }
        }
    }

    private fun checkForUpdatesInBackground() {
        // Only check once per app session to avoid excessive network usage
        val prefs = requireContext().getSharedPreferences("update_check", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong("last_check", 0)
        val currentTime = System.currentTimeMillis()
        
        // Check only if it's been more than 24 hours since last check
        if (currentTime - lastCheck > 24 * 60 * 60 * 1000) {
            lifecycleScope.launch {
                try {
                    val updateInfo = updateChecker.checkForUpdates()
                    prefs.edit().putLong("last_check", currentTime).apply()
                    
                    if (updateInfo.isUpdateAvailable && updateInfo.error == null) {
                        // Show a subtle notification or toast about available update
                        Toast.makeText(requireContext(), 
                            "Update available: v${updateInfo.latestVersion}", 
                            Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    // Silently fail for background checks
                }
            }
        }
    }
}