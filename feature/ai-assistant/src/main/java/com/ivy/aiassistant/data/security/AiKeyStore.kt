package com.ivy.aiassistant.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ivy.aiassistant.domain.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores per-provider API keys encrypted at rest using AndroidX Security's
 * EncryptedSharedPreferences (AES256-GCM under a hardware-backed master key
 * when available).
 *
 * Keys are never persisted to the regular DataStore preferences to avoid
 * leaking them in backups or logs.
 */
@Singleton
class AiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy { createPrefs() }

    @Suppress("DEPRECATION")
    private fun createPrefs(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getApiKey(provider: AiProvider): String =
        prefs.getString(provider.prefKey(), null).orEmpty()

    fun setApiKey(provider: AiProvider, key: String) {
        prefs.edit().apply {
            if (key.isBlank()) remove(provider.prefKey()) else putString(provider.prefKey(), key)
        }.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun AiProvider.prefKey(): String = "api_key_${name.lowercase()}"

    companion object {
        const val PREFS_FILE_NAME = "ivy_ai_secure_prefs"
    }
}
