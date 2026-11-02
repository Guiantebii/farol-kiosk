package com.kiosk.launcher

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest

class KioskPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val legacyPrefs: SharedPreferences =
        appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    private val prefs: SharedPreferences = openPreferences(appContext)

    var kioskUrl: String
        get() = prefs.getString(KEY_KIOSK_URL, DEFAULT_URL).orEmpty()
        set(value) = prefs.edit { putString(KEY_KIOSK_URL, value.trim()) }

    var pinHint: String
        get() = prefs.getString(KEY_PIN_HINT, DEFAULT_PIN_HINT).orEmpty()
        set(value) = prefs.edit { putString(KEY_PIN_HINT, value.trim()) }

    fun isUrlConfigured(): Boolean = kioskUrl.isNotBlank()

    fun isPinConfigured(): Boolean = getStoredPinHash().isNotEmpty()

    fun setPinFromPlainText(plainPin: String, hint: String = pinHint) {
        val trimmed = plainPin.trim()
        prefs.edit {
            if (trimmed.isEmpty()) {
                putString(KEY_KIOSK_PIN, DEFAULT_PIN)
                putString(KEY_PIN_HINT, DEFAULT_PIN_HINT)
            } else {
                putString(KEY_KIOSK_PIN, hashPin(trimmed))
                putString(KEY_PIN_HINT, hint.trim())
            }
        }
    }

    fun verifyPin(input: String): Boolean {
        val stored = getStoredPinHash()
        if (stored.isEmpty()) {
            return input.isEmpty()
        }
        val inputHash = hashPin(input.trim())
        if (inputHash == stored) {
            return true
        }
        if (!isSha256Hex(stored) && stored == input.trim()) {
            setPinFromPlainText(input.trim())
            return true
        }
        return false
    }

    private fun getStoredPinHash(): String =
        prefs.getString(KEY_KIOSK_PIN, DEFAULT_PIN) ?: DEFAULT_PIN

    private fun hashPin(plain: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(plain.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun isSha256Hex(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }

    private fun openPreferences(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encrypted = EncryptedSharedPreferences.create(
                ENCRYPTED_PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            migrateFromLegacyIfNeeded(encrypted)
            encrypted
        } catch (e: Exception) {
            Log.w(TAG, "Preferências criptografadas indisponíveis; usando armazenamento legado", e)
            legacyPrefs
        }
    }

    private fun migrateFromLegacyIfNeeded(encrypted: SharedPreferences) {
        if (encrypted.getBoolean(KEY_MIGRATED, false)) return

        val hasLegacyData = legacyPrefs.all.isNotEmpty()
        if (!hasLegacyData) {
            encrypted.edit { putBoolean(KEY_MIGRATED, true) }
            return
        }

        encrypted.edit {
            putString(KEY_KIOSK_URL, legacyPrefs.getString(KEY_KIOSK_URL, DEFAULT_URL))
            putString(KEY_KIOSK_PIN, legacyPrefs.getString(KEY_KIOSK_PIN, DEFAULT_PIN))
            putString(KEY_PIN_HINT, legacyPrefs.getString(KEY_PIN_HINT, DEFAULT_PIN_HINT))
            putBoolean(KEY_MIGRATED, true)
        }
        legacyPrefs.edit { clear() }
    }

    companion object {
        private const val TAG = "KioskPreferences"
        private const val LEGACY_PREFS_NAME = "kiosk_prefs"
        private const val ENCRYPTED_PREFS_NAME = "kiosk_prefs_encrypted"
        private const val KEY_MIGRATED = "prefs_migrated_v2"

        private const val KEY_KIOSK_URL = "kiosk_url"
        private const val KEY_KIOSK_PIN = "kiosk_pin"
        private const val KEY_PIN_HINT = "pin_hint"

        const val DEFAULT_URL = ""
        const val DEFAULT_PIN = ""
        const val DEFAULT_PIN_HINT = ""

        const val SAMPLE_FATEC_URL = "https://www.fatecsp.br/"
    }
}
