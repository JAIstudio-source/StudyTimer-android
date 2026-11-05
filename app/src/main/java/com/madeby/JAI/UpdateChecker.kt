package com.madeby.JAI

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Announcement / broadcast payload dispatched to users.
 * Supports informational updates, warnings, maintenance alerts, and critical error notices.
 */
data class BroadcastMessage(
    val id: String,
    val title: String,
    val message: String,
    val type: String = "info", // "info", "warning", "error", "maintenance"
    val canDismiss: Boolean = true,
    val actionUrl: String? = null,
    val actionText: String? = null,
    val minVersionCode: Int = 0,
    val maxVersionCode: Int = Int.MAX_VALUE
)

/**
 * Manifest information parsed from version.json / Supabase.
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val apkUrl: String?,
    val releaseNotes: String?,
    val minSupportedVersionCode: Int = 0,
    val forceUpdate: Boolean = false,
    val broadcast: BroadcastMessage? = null
) {
    val hasReleaseNotes: Boolean get() = !releaseNotes.isNullOrBlank()

    /**
     * Determines whether the current installed version is blocked from proceeding
     * without installing this mandatory update.
     */
    fun isHardBlocked(currentVersion: Long): Boolean =
        (versionCode > currentVersion && forceUpdate) || (currentVersion < minSupportedVersionCode)
}

object UpdateChecker {

    private const val MANIFEST_URL = "https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/version.json"
    private const val FALLBACK_URL = "https://get-studytimer.vercel.app/?download=true"

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching { fetch() }.getOrNull()
    }

    fun check(@Suppress("UNUSED_PARAMETER") context: Context, onResult: (UpdateInfo?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { fetch() }.getOrNull()
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun parseUpdateInfo(body: String): UpdateInfo? {
        return runCatching {
            val json = JSONObject(body)
            val versionCode = json.optInt("versionCode", -1)
            val versionName = json.optString("versionName", "")
            val url = json.optString("url", "").ifBlank { FALLBACK_URL }
            val apkUrl = json.optString("apkUrl", "").ifBlank { null }
            val releaseNotes = if (json.has("releaseNotes") && !json.isNull("releaseNotes")) {
                json.optString("releaseNotes").ifBlank { null }
            } else null
            val minSupportedVersionCode = json.optInt("minSupportedVersionCode", 0)
            val forceUpdate = json.optBoolean("forceUpdate", false)

            var broadcast: BroadcastMessage? = null
            if (json.has("broadcast") && !json.isNull("broadcast")) {
                val bObj = json.optJSONObject("broadcast")
                if (bObj != null) {
                    val msg = bObj.optString("message", "")
                    if (msg.isNotBlank()) {
                        broadcast = BroadcastMessage(
                            id = bObj.optString("id", "").ifBlank { "broadcast_${System.currentTimeMillis()}" },
                            title = bObj.optString("title", "Announcement"),
                            message = msg,
                            type = bObj.optString("type", "info"),
                            canDismiss = bObj.optBoolean("canDismiss", true),
                            actionUrl = bObj.optString("actionUrl", "").ifBlank { null },
                            actionText = bObj.optString("actionText", "").ifBlank { null },
                            minVersionCode = bObj.optInt("minVersionCode", 0),
                            maxVersionCode = bObj.optInt("maxVersionCode", Int.MAX_VALUE)
                        )
                    }
                }
            }

            if (versionCode <= 0) return null
            UpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                url = url,
                apkUrl = apkUrl,
                releaseNotes = releaseNotes,
                minSupportedVersionCode = minSupportedVersionCode,
                forceUpdate = forceUpdate,
                broadcast = broadcast
            )
        }.getOrNull()
    }

    private fun fetch(): UpdateInfo? {
        val connection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        var info: UpdateInfo? = null
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                info = parseUpdateInfo(body)
            }
        } finally {
            connection.disconnect()
        }

        // If manifest did not have an active broadcast, check Supabase real-time broadcast channel
        if (info != null && info.broadcast == null) {
            val sbBroadcast = fetchActiveSupabaseBroadcast()
            if (sbBroadcast != null) {
                info = info.copy(broadcast = sbBroadcast)
            }
        }
        return info
    }

    fun fetchActiveSupabaseBroadcast(): BroadcastMessage? {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY
        if (supabaseUrl.isBlank() || anonKey.isBlank()) return null
        return try {
            val conn = URL("$supabaseUrl/rest/v1/app_broadcasts?is_active=eq.true&order=created_at.desc&limit=1").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", anonKey)
            conn.setRequestProperty("Authorization", "Bearer $anonKey")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val array = JSONArray(body)
            if (array.length() == 0) return null
            val obj = array.getJSONObject(0)
            val msg = obj.optString("message", "")
            if (msg.isBlank()) return null
            BroadcastMessage(
                id = obj.optString("id", "sb_notice"),
                title = obj.optString("title", "Announcement"),
                message = msg,
                type = obj.optString("type", "info"),
                canDismiss = obj.optBoolean("can_dismiss", obj.optBoolean("canDismiss", true)),
                actionUrl = obj.optString("action_url", obj.optString("actionUrl", "")).ifBlank { null },
                actionText = obj.optString("action_text", obj.optString("actionText", "")).ifBlank { null },
                minVersionCode = obj.optInt("min_version_code", obj.optInt("minVersionCode", 0)),
                maxVersionCode = obj.optInt("max_version_code", obj.optInt("maxVersionCode", Int.MAX_VALUE))
            )
        } catch (_: Exception) {
            null
        }
    }
}
