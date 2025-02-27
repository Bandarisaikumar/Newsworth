package com.example.newsworth.utils


import android.content.Context
import android.content.SharedPreferences

object SharedPrefModule {

    const val PREF_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"

    fun provideTokenManager(context: Context): TokenManager {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return TokenManager(sharedPreferences)
    }

    class TokenManager(private val sharedPreferences: SharedPreferences) {
        var accessToken: String?
            get() = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
            set(value) {
                sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, value).apply()
            }

        var userId: String?
            get() = sharedPreferences.getString("user_id", null)
            set(value) = sharedPreferences.edit().putString("user_id", value).apply()

        var userId2: String?
            get() = sharedPreferences.getString("user_id", null)
            set(value) { sharedPreferences.edit().putString("user_id", value).apply()
            }

        fun SharedPreferences.Editor.putDouble(key: String, value: Double) {
            putFloat(key, value.toFloat())
        }
        fun clearTokens() {
            sharedPreferences.edit().remove("access_token").remove("user_id").apply()
        }
    }
}


