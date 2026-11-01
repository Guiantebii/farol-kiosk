package com.kiosk.launcher

import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kiosk.launcher.databinding.DialogPinBinding

object PinUnlockDialog {

    fun show(
        context: Context,
        preferences: KioskPreferences,
        onUnlocked: () -> Unit
    ): AlertDialog {
        val binding = DialogPinBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_KioskLauncher_PinDialog)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        binding.btnPinCancel.setOnClickListener { dialog.dismiss() }

        val savedHint = preferences.pinHint.trim()
        if (savedHint.isNotEmpty()) {
            binding.textPinRecoveryHint.visibility = android.view.View.VISIBLE
            binding.textPinRecoveryHint.text =
                context.getString(R.string.pin_dialog_recovery_hint, savedHint)
        } else {
            binding.textPinRecoveryHint.visibility = android.view.View.GONE
        }

        val submit = {
            val pin = binding.inputPin.text?.toString().orEmpty()
            if (preferences.verifyPin(pin)) {
                dialog.dismiss()
                onUnlocked()
            } else {
                binding.inputPin.error = context.getString(R.string.pin_incorrect)
                binding.inputPin.text?.clear()
                Toast.makeText(context, R.string.pin_incorrect, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPinConfirm.setOnClickListener { submit() }
        binding.inputPin.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        dialog.setOnShowListener {
            binding.inputPin.requestFocus()
            binding.inputPin.postDelayed({
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.inputPin, InputMethodManager.SHOW_IMPLICIT)
            }, 200)
        }

        return dialog
    }
}
