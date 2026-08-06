package com.madeby.JAI

import android.content.SharedPreferences

fun SharedPreferences.safeInt(key: String, defValue: Int): Int {
    return (all[key] as? Number)?.toInt() ?: defValue
}
