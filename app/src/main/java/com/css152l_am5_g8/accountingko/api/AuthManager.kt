package com.css152l_am5_g8.accountingko.api

import android.content.Context

class AuthManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "auth"
        private const val TOKEN_KEY = "token"

        // Static method overload to get token with context param
        fun getToken(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(TOKEN_KEY, null)
        }

        // Static method overload to save token with context param
        fun saveToken(context: Context, token: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(TOKEN_KEY, token).apply()
        }

        // Static method overload to clear token with context param
        fun clearToken(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(TOKEN_KEY).apply()
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString(TOKEN_KEY, null)

    fun clearToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }
}
