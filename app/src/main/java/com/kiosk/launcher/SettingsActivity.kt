package com.kiosk.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kiosk.launcher.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: KioskPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = KioskPreferences(this)
        binding.inputUrlLayout.helperText = getString(
            R.string.settings_url_helper,
            KioskPreferences.SAMPLE_FATEC_URL
        )
        binding.inputUrl.setText(preferences.kioskUrl)
        binding.inputPin.setText(preferences.kioskPin)

        binding.btnSaveStart.setOnClickListener { saveAndStartKiosk() }
        binding.btnAndroidSettings.setOnClickListener { openAndroidSettings() }
        binding.btnChangeDefaultLauncher.setOnClickListener { openHomeSettings() }
    }

    private fun saveAndStartKiosk() {
        val url = binding.inputUrl.text?.toString().orEmpty().trim()
        val pin = binding.inputPin.text?.toString().orEmpty().trim()

        if (url.isEmpty()) {
            binding.inputUrlLayout.error = getString(R.string.error_url_required)
            return
        }
        binding.inputUrlLayout.error = null

        if (pin.isNotEmpty() && pin.length < 4) {
            binding.inputPinLayout.error = getString(R.string.error_pin_min_length)
            return
        }
        binding.inputPinLayout.error = null

        if (pin.isEmpty()) {
            showNoPinWarningDialog(url)
            return
        }

        persistAndLaunchKiosk(url, pin)
    }

    private fun showNoPinWarningDialog(url: String) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KioskLauncher_PinDialog)
            .setMessage(R.string.settings_no_pin_warning_message)
            .setPositiveButton(R.string.settings_no_pin_continue) { _, _ ->
                persistAndLaunchKiosk(url, "")
            }
            .setNegativeButton(R.string.settings_no_pin_cancel, null)
            .show()
    }

    private fun persistAndLaunchKiosk(url: String, pin: String) {
        preferences.kioskUrl = url
        preferences.kioskPin = pin

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_RELOAD_KIOSK, true)
        }
        startActivity(intent)
        finish()
    }

    private fun openAndroidSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    private fun openHomeSettings() {
        val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS)
        try {
            startActivity(homeSettingsIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.settings_home_settings_fallback, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
