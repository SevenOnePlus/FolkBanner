package com.folkbanner.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "folk_banner_settings"
        private const val KEY_USE_UPSTREAM_API = "use_upstream_api"
        private const val KEY_R18_ENABLED = "r18_enabled"
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    var useUpstreamApi: Boolean
        get() = prefs.getBoolean(KEY_USE_UPSTREAM_API, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_UPSTREAM_API, value).apply()

    var r18Enabled: Boolean
        get() = prefs.getBoolean(KEY_R18_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_R18_ENABLED, value).apply()
}