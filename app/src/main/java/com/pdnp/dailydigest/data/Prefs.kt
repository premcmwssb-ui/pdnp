package com.pdnp.dailydigest.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Encrypted key-value settings (falls back to plain prefs if keystore is unavailable). */
object Prefs {
    private const val FILE = "digest_prefs"

    @Volatile
    private var cached: SharedPreferences? = null

    fun get(context: Context): SharedPreferences =
        cached ?: synchronized(this) {
            cached ?: create(context.applicationContext).also { cached = it }
        }

    private fun create(context: Context): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun apiKey(context: Context): String? = get(context).getString("anthropic_api_key", null)
    fun setApiKey(context: Context, value: String) =
        get(context).edit().putString("anthropic_api_key", value.trim()).apply()

    fun aiEnabled(context: Context): Boolean = get(context).getBoolean("ai_enabled", false)
    fun setAiEnabled(context: Context, value: Boolean) =
        get(context).edit().putBoolean("ai_enabled", value).apply()

    fun lastSync(context: Context, key: String): Long = get(context).getLong(key, 0L)
    fun setLastSync(context: Context, key: String, value: Long) =
        get(context).edit().putLong(key, value).apply()
}
