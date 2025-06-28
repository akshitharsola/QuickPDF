package com.example.quickpdf.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quickpdf.QuickPDFApplication
import com.example.quickpdf.R
import com.example.quickpdf.data.model.RecentFile
import com.example.quickpdf.databinding.ActivityMainBinding
import com.example.quickpdf.ui.adapters.RecentFilesAdapter
import com.example.quickpdf.utils.FileUtil
import com.example.quickpdf.utils.PermissionUtil
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var recentFilesAdapter: RecentFilesAdapter
    
    private val mainViewModel: MainViewModel by viewModels {
        ViewModelFactory((application as QuickPDFApplication).repository)
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (PermissionUtil.hasStoragePermission(this)) {
            openFilePicker()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupRecyclerView()
            setupClickListeners()
            observeViewModel()
            handleIntent(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            // Show basic error message to user
            Toast.makeText(this, "Error starting app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionUtil.REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker()
                } else {
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        recentFilesAdapter = RecentFilesAdapter(
            onFileClick = { recentFile ->
                openPdfFile(recentFile)
            },
            onMoreClick = { recentFile ->
                showRecentFileOptions(recentFile)
            }
        )

        binding.recyclerViewRecentFiles.apply {
            adapter = recentFilesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupClickListeners() {
        binding.cardOpenPdf.setOnClickListener {
            requestStoragePermissionAndOpenPicker()
        }

        binding.fabOpenPdf.setOnClickListener {
            requestStoragePermissionAndOpenPicker()
        }
    }

    private fun observeViewModel() {
        mainViewModel.recentFiles.observe(this) { recentFiles ->
            recentFilesAdapter.submitList(recentFiles)
            binding.textViewNoFiles.visibility = if (recentFiles.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    private fun requestStoragePermissionAndOpenPicker() {
        if (PermissionUtil.hasStoragePermission(this)) {
            openFilePicker()
        } else {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.storage_permission_title)
            .setMessage(R.string.storage_permission_message)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val intent = PermissionUtil.getStoragePermissionIntent(this)
                    if (intent != null) {
                        permissionLauncher.launch(intent)
                    }
                } else {
                    PermissionUtil.requestStoragePermission(this)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun openFilePicker() {
        try {
            android.util.Log.d("MainActivity", "Opening file picker")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                // Add extra flags for better compatibility
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error opening file picker", e)
            Toast.makeText(this, "Error opening file picker: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        android.util.Log.d("MainActivity", "Handling selected file: $uri")
        
        try {
            // Grant persistent URI permission for future access
            contentResolver.takePersistableUriPermission(
                uri, 
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            android.util.Log.d("MainActivity", "Granted persistent URI permission")
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Could not grant persistent URI permission", e)
            // Continue anyway - file might still be accessible
        }
        
        val fileName = FileUtil.getFileName(this, uri)
        android.util.Log.d("MainActivity", "File name: $fileName")
        
        if (fileName != null && FileUtil.isPdfFile(fileName)) {
            android.util.Log.d("MainActivity", "Valid PDF file, adding to recent files")
            mainViewModel.addRecentFile(this, uri)
            openPdfViewer(uri)
        } else {
            android.util.Log.w("MainActivity", "Invalid PDF file: $fileName")
            Toast.makeText(this, R.string.invalid_pdf, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPdfFile(recentFile: RecentFile) {
        try {
            val file = File(recentFile.filePath)
            if (file.exists()) {
                mainViewModel.updateLastAccessed(recentFile.filePath)
                val uri = Uri.fromFile(file)
                openPdfViewer(uri)
            } else {
                Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show()
                mainViewModel.deleteRecentFile(recentFile)
            }
        } catch (e: Exception) {
            // Try as URI if file path doesn't work
            try {
                val uri = Uri.parse(recentFile.filePath)
                openPdfViewer(uri)
            } catch (ex: Exception) {
                Toast.makeText(this, R.string.error_opening_file, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPdfViewer(uri: Uri) {
        try {
            android.util.Log.d("MainActivity", "Opening PDF viewer with URI: $uri")
            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                data = uri
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error opening PDF viewer", e)
            Toast.makeText(this, "Error opening PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRecentFileOptions(recentFile: RecentFile) {
        val popupMenu = PopupMenu(this, binding.root)
        popupMenu.menu.add(R.string.delete)

        popupMenu.setOnMenuItemClickListener {
            when (it.title) {
                getString(R.string.delete) -> {
                    showDeleteConfirmation(recentFile)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeleteConfirmation(recentFile: RecentFile) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(R.string.are_you_sure)
            .setPositiveButton(R.string.delete) { _, _ ->
                mainViewModel.deleteRecentFile(recentFile)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_recent -> {
                showClearRecentConfirmation()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearRecentConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_recent)
            .setMessage(R.string.are_you_sure)
            .setPositiveButton(R.string.clear_recent) { _, _ ->
                mainViewModel.clearAllRecentFiles()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about)
            .setMessage("QuickPDF v1.0\n\nA lightweight PDF viewer for Android")
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}