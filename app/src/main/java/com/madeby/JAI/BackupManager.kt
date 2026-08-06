package com.madeby.JAI

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.*

class BackupManager(private val context: Context) {

    private fun getBackupFile(): File = File(context.filesDir, "study_timer_backup.dat")

    fun runSilentAutoBackup() {
        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val json = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                json.put(key, value)
            }
            getBackupFile().writeText(json.toString())
        } catch (_: Exception) {}
    }

    fun triggerAutoRestoreIfPresent() {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        if (sharedPrefs.all.isNotEmpty()) return

        try {
            val file = getBackupFile()
            if (!file.exists()) return

            val jsonString = file.readText()
            val importedJsonObject = JSONObject(jsonString)
            val editor = sharedPrefs.edit()
            sanitizeAndBuildPreferences(importedJsonObject, editor)
            editor.apply()
        } catch (_: Exception) {}
    }

    fun exportDataToJSON(uri: Uri): Boolean {
        try {
            val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
            val json = JSONObject()
            for ((key, value) in sharedPrefs.all) {
                json.put(key, value)
            }
            val outputStream = context.contentResolver.openOutputStream(uri, "w")
                ?: return false
            outputStream.use { stream ->
                stream.write(json.toString().toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            return true
        } catch (_: Exception) {}
        return false
    }

    fun importDataFromJSON(uri: Uri): Boolean {
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = BufferedReader(InputStreamReader(inputStream)).readText()
                val importedJsonObject = JSONObject(jsonString)
                val editor = sharedPrefs.edit()
                editor.clear()
                sanitizeAndBuildPreferences(importedJsonObject, editor)
                editor.apply()
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    private val intPrefKeys = setOf(
        "customBg", "customPrimary", "customHue", "customSecondary", "customSecondaryHue",
        "current_streak", "selected_days_filter", "reminder_hour", "reminder_minute"
    )

    private fun sanitizeAndBuildPreferences(sourceJson: JSONObject, editor: android.content.SharedPreferences.Editor) {
        val keys = sourceJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (key) {
                "timerState" -> editor.putString(key, "IDLE")
                "accumulatedStudy", "currentBreakSeconds", "lastTimestamp", "focus_remaining_secs", "pre_pause_state", "streak_last_calculated" -> {
                    // Never resurrect an in-flight session from a backup; streak is recomputed on next stats open.
                }
                else -> when (val value = sourceJson.get(key)) {
                    is Number -> {
                        if (key in intPrefKeys) {
                            editor.putInt(key, value.toInt())
                        } else {
                            editor.putLong(key, value.toLong())
                        }
                    }
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                }
            }
        }
    }
}
