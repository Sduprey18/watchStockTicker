package com.stocktracker.wear.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.stocktracker.wear.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureApiKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        const val PREFS_FILE = "secure_stock_prefs"
        const val KEY_API_KEY = "stock_api_key"
    }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        seedFromBuildConfigIfNeeded()
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
        Timber.i("API key updated")
    }

    fun isKeyConfigured(): Boolean = getApiKey().isNotBlank()

    private fun seedFromBuildConfigIfNeeded() {
        if (!prefs.contains(KEY_API_KEY) || prefs.getString(KEY_API_KEY, "").isNullOrBlank()) {
            val buildConfigKey = BuildConfig.STOCK_API_KEY
            if (buildConfigKey.isNotBlank()) {
                setApiKey(buildConfigKey)
                Timber.i("API key seeded from BuildConfig")
            } else {
                Timber.w("No API key found in BuildConfig or secure storage")
            }
        }
    }
}

