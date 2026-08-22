package com.nuvio.tv.data.local.iptv

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.nuvio.tv.domain.model.iptv.XtreamCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptvCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "IptvCredentialStore"
        private const val PREFS_FILE = "iptv_secure_credentials"
    }

    private val gson = Gson()

    private val prefs: SharedPreferences by lazy {
        try {
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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to private prefs", e)
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    fun saveCredentials(playlistId: String, credentials: XtreamCredentials) {
        val json = gson.toJson(credentials)
        prefs.edit().putString("xc_$playlistId", json).apply()
    }

    fun getCredentials(playlistId: String): XtreamCredentials? {
        val json = prefs.getString("xc_$playlistId", null) ?: return null
        return runCatching { gson.fromJson(json, XtreamCredentials::class.java) }.getOrNull()
    }

    fun removeCredentials(playlistId: String) {
        prefs.edit().remove("xc_$playlistId").apply()
    }
}
