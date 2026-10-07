package com.madeby.JAI

import android.content.SharedPreferences

fun SharedPreferences.safeInt(key: String, defValue: Int): Int {
    return try {
        (all[key] as? Number)?.toInt() ?: (all[key] as? String)?.toIntOrNull() ?: defValue
    } catch (_: Exception) {
        defValue
    }
}

fun SharedPreferences.safeLong(key: String, defValue: Long): Long {
    return try {
        (all[key] as? Number)?.toLong() ?: (all[key] as? String)?.toLongOrNull() ?: defValue
    } catch (_: Exception) {
        defValue
    }
}

fun SharedPreferences.safeBoolean(key: String, defValue: Boolean): Boolean {
    return try {
        val v = all[key]
        when (v) {
            is Boolean -> v
            is String -> v.toBooleanStrictOrNull() ?: defValue
            is Number -> v.toLong() != 0L
            else -> defValue
        }
    } catch (_: Exception) {
        defValue
    }
}

fun SharedPreferences.safeFloat(key: String, defValue: Float): Float {
    return try {
        (all[key] as? Number)?.toFloat() ?: (all[key] as? String)?.toFloatOrNull() ?: defValue
    } catch (_: Exception) {
        defValue
    }
}

fun SharedPreferences.safeString(key: String, defValue: String? = null): String? {
    return try {
        val v = all[key]
        v?.toString() ?: defValue
    } catch (_: Exception) {
        defValue
    }
}

