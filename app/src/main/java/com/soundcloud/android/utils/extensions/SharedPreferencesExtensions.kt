package com.soundcloud.android.utils.extensions

import android.content.SharedPreferences

inline fun <T> SharedPreferences.put(key: String, value: T) {
    edit().apply {
        putValue(key, value)
        apply()
    }
}

inline fun <T> SharedPreferences.Editor.putValue(key: String, value: T) {
    when(value) {
        is String -> putString(key, value)
        is Long -> putLong(key, value)
        is Float -> putFloat(key, value)
        is Boolean -> putBoolean(key, value)
        else -> throw IllegalArgumentException("Can't handle type of key $key")
    }
}
