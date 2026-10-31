package com.kiosk.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class KioskPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var kioskUrl: String
        get() = prefs.getString(KEY_KIOSK_URL, DEFAULT_URL).orEmpty()
        set(value) = prefs.edit { putString(KEY_KIOSK_URL, value.trim()) }

    var kioskPin: String
        get() = prefs.getString(KEY_KIOSK_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        set(value) = prefs.edit { putString(KEY_KIOSK_PIN, value.trim()) }

    fun isUrlConfigured(): Boolean = kioskUrl.isNotBlank()

    fun isPinConfigured(): Boolean = kioskPin.isNotEmpty()

    fun verifyPin(input: String): Boolean = input == kioskPin

    companion object {
        private const val PREFS_NAME = "kiosk_prefs"
        private const val KEY_KIOSK_URL = "kiosk_url"
        private const val KEY_KIOSK_PIN = "kiosk_pin"

        const val DEFAULT_URL = ""
        const val DEFAULT_PIN = ""

        const val SAMPLE_FATEC_URL = "https://www.fatecsp.br/"
    }
}
