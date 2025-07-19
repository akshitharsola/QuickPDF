package com.quickpdf.reader.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.quickpdf.reader.R
import com.quickpdf.reader.databinding.DialogPasswordInputBinding

class PasswordDialog(
    private val context: Context,
    private val fileName: String,
    private val onPasswordEntered: (String) -> Unit,
    private val onCancelled: () -> Unit
) {
    
    private var dialog: AlertDialog? = null
    private var binding: DialogPasswordInputBinding? = null
    
    fun show() {
        val inflater = LayoutInflater.from(context)
        binding = DialogPasswordInputBinding.inflate(inflater)
        
        dialog = AlertDialog.Builder(context)
            .setView(binding?.root)
            .setCancelable(true)
            .setPositiveButton(R.string.unlock, null) // Set to null initially
            .setNegativeButton(R.string.cancel) { _, _ ->
                onCancelled()
            }
            .setOnCancelListener {
                onCancelled()
            }
            .create()
        
        dialog?.setOnShowListener { dialogInterface ->
            val positiveButton = (dialogInterface as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            val editText = binding?.editTextPassword
            
            // Set up custom click listener for positive button to prevent auto-dismiss
            positiveButton.setOnClickListener {
                val password = editText?.text?.toString() ?: ""
                if (password.isNotEmpty()) {
                    // Clear any previous error
                    binding?.textViewError?.visibility = android.view.View.GONE
                    onPasswordEntered(password)
                }
            }
            
            // Set up IME action to trigger unlock
            editText?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val password = editText.text?.toString() ?: ""
                    if (password.isNotEmpty()) {
                        // Clear any previous error
                        binding?.textViewError?.visibility = android.view.View.GONE
                        onPasswordEntered(password)
                    }
                    true
                } else {
                    false
                }
            }
            
            // Enable/disable positive button based on password input
            editText?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    positiveButton.isEnabled = !s.isNullOrEmpty()
                    // Hide error when user starts typing
                    if (!s.isNullOrEmpty()) {
                        binding?.textViewError?.visibility = android.view.View.GONE
                    }
                }
            })
            
            // Show keyboard
            editText?.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
        
        dialog?.show()
        
        // Initially disable the positive button
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
    }
    
    fun showError(errorMessage: String) {
        binding?.textViewError?.apply {
            text = errorMessage
            visibility = android.view.View.VISIBLE
        }
        
        // Clear password field and refocus
        binding?.editTextPassword?.apply {
            text?.clear()
            requestFocus()
        }
        
        // Show keyboard again
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding?.editTextPassword, InputMethodManager.SHOW_IMPLICIT)
    }
    
    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        binding = null
    }
}