package com.madeby.JAI

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class StudyRoom(
    val code: String,
    val name: String,
    val creatorName: String,
    val memberCount: Int,
    val category: String = "General",
    val isPrivate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class StudyRoomMember(
    val id: String,
    val displayName: String,
    val avatarEmoji: String,
    val isStudying: Boolean,
    val currentSubject: String?,
    val elapsedMinutes: Int,
    val isSelf: Boolean = false,
    val lastActiveAt: Long = System.currentTimeMillis()
)

object StudyRoomManager {

    private const val TAG = "StudyRoomManager"
    private const val PREFS_NAME = "studytimer_study_rooms"
    private const val KEY_ACTIVE_ROOM = "active_room_json"
    private const val KEY_LAST_JOINED_ROOM = "last_joined_room_json"
    private const val KEY_USER_ROOMS = "user_created_rooms_json"
    private const val KEY_DEV_MOCK_ACTIVE = "dev_mock_active"
    private const val KEY_DEV_MOCK_ROOMS = "dev_mock_rooms_json"
    private const val KEY_DEV_MOCK_MEMBERS_PREFIX = "dev_mock_members_"

    // In-memory caching to minimize server egress & avoid redundant queries
    private const val ROOMS_CACHE_TTL_MS = 30_000L // 30 seconds
    private const val MEMBERS_CACHE_TTL_MS = 5_000L // 5 seconds (reduced for fresh presence)

    @Volatile
    private var cachedPublicRooms: List<StudyRoom>? = null
    @Volatile
    private var cachedRoomsTimestamp: Long = 0L

    private val cachedMembers = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<StudyRoomMember>>>()
    @Volatile
    private var lastPresenceSyncTimestamp: Long = 0L
    @Volatile
    private var lastReportedStudyingState: Boolean? = null

    private fun JSONObject.putNullable(name: String, value: Any?) {
        if (value != null) {
            put(name, value)
        } else {
            put(name, JSONObject.NULL as Any)
        }
    }

    // ==========================================
    // Local Room Storage & Active Room State
    // ==========================================

    fun getActiveRoom(context: Context): StudyRoom? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ACTIVE_ROOM, null) ?: return null
        return try {
            val json = JSONObject(raw)
            StudyRoom(
                code = json.getString("code"),
                name = json.getString("name"),
                creatorName = json.optString("creatorName", "Student"),
                memberCount = json.optInt("memberCount", 1),
                category = json.optString("category", "General"),
                isPrivate = json.optBoolean("isPrivate", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getLastJoinedRoom(context: Context): StudyRoom? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LAST_JOINED_ROOM, null) ?: return null
        return try {
            val json = JSONObject(raw)
            StudyRoom(
                code = json.getString("code"),
                name = json.getString("name"),
                creatorName = json.optString("creatorName", "Student"),
                memberCount = json.optInt("memberCount", 1),
                category = json.optString("category", "General"),
                isPrivate = json.optBoolean("isPrivate", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    fun setActiveRoom(context: Context, room: StudyRoom?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (room == null) {
            prefs.edit().remove(KEY_ACTIVE_ROOM).apply()
        } else {
            val json = JSONObject().apply {
                put("code", room.code)
                put("name", room.name)
                put("creatorName", room.creatorName)
                put("memberCount", room.memberCount)
                put("category", room.category)
                put("isPrivate", room.isPrivate)
                put("createdAt", room.createdAt)
            }
            prefs.edit()
                .putString(KEY_ACTIVE_ROOM, json.toString())
                .putString(KEY_LAST_JOINED_ROOM, json.toString())
                .apply()
        }
    }

    fun getUserCreatedRooms(context: Context): List<StudyRoom> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_USER_ROOMS, "[]") ?: "[]"
        val list = mutableListOf<StudyRoom>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StudyRoom(
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        creatorName = obj.optString("creatorName", "You"),
                        memberCount = obj.optInt("memberCount", 1),
                        category = obj.optString("category", "General"),
                        isPrivate = obj.optBoolean("isPrivate", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveUserCreatedRoomLocally(context: Context, room: StudyRoom) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = getUserCreatedRooms(context).filterNot { it.code == room.code }.toMutableList()
        existing.add(0, room)
        val arr = JSONArray()
        for (r in existing) {
            arr.put(JSONObject().apply {
                put("code", r.code)
                put("name", r.name)
                put("creatorName", r.creatorName)
                put("memberCount", r.memberCount)
                put("category", r.category)
                put("isPrivate", r.isPrivate)
                put("createdAt", r.createdAt)
            })
        }
        prefs.edit().putString(KEY_USER_ROOMS, arr.toString()).apply()
    }

    // ==========================================
    // Server REST / RPC Network Operations
    // ==========================================

    private fun getSupabaseCredentials(): Pair<String, String>? {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/')
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) return null
        return url to key
    }

    suspend fun fetchPublicRooms(
        context: Context,
        category: String = "All",
        search: String = "",
        forceRefresh: Boolean = false
    ): Result<List<StudyRoom>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. If Developer Mock Mode is active, return local mock rooms immediately (excluding private rooms from open explore)
        if (isDevMockActive(context)) {
            val mockRooms = getDevMockRooms(context).filterNot { it.isPrivate }
            val filtered = if (category == "All") mockRooms else mockRooms.filter { it.category.equals(category, ignoreCase = true) }
            return@withContext Result.success(filtered)
        }

        // 2. Check In-Memory Cache for server requests
        if (!forceRefresh && cachedPublicRooms != null && (now - cachedRoomsTimestamp) < ROOMS_CACHE_TTL_MS) {
            val cached = cachedPublicRooms!!
            val filtered = if (category == "All") cached else cached.filter { it.category.equals(category, ignoreCase = true) }
            return@withContext Result.success(filtered)
        }

        val creds = getSupabaseCredentials()
        if (creds == null) {
            // Offline or credentials not set: return user created public local rooms only
            val localRooms = getUserCreatedRooms(context).filterNot { it.isPrivate }
            return@withContext Result.success(localRooms)
        }

        val (supabaseUrl, anonKey) = creds
        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/get_public_study_rooms")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Accept-Encoding", "gzip, deflate")
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
            }

            val body = JSONObject().apply {
                putNullable("p_category", if (category == "All") null else category)
                put("p_search", search.trim())
                put("p_limit", 30)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val stream = if (conn.headerFields["Content-Encoding"]?.contains("gzip") == true) {
                    java.util.zip.GZIPInputStream(conn.inputStream)
                } else {
                    conn.inputStream
                }
                val rawResponse = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val jsonArr = if (rawResponse.isNotBlank()) JSONArray(rawResponse) else JSONArray()
                val rooms = mutableListOf<StudyRoom>()

                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    rooms.add(
                        StudyRoom(
                            code = obj.getString("code"),
                            name = obj.getString("name"),
                            creatorName = obj.optString("creator_name", "Student"),
                            memberCount = obj.optInt("member_count", 1),
                            category = obj.optString("category", "General"),
                            isPrivate = obj.optBoolean("is_private", false),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // Merge user created public rooms that might still be syncing
                val userCreated = getUserCreatedRooms(context).filterNot { it.isPrivate }
                val combined = (userCreated + rooms).distinctBy { it.code }

                cachedPublicRooms = combined
                cachedRoomsTimestamp = now

                val filtered = if (category == "All") combined else combined.filter { it.category.equals(category, ignoreCase = true) }
                Result.success(filtered)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "fetchPublicRooms HTTP $responseCode: $err")
                // Graceful fallback to user created public rooms on server error
                Result.success(getUserCreatedRooms(context).filterNot { it.isPrivate })
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchPublicRooms network exception", e)
            Result.success(getUserCreatedRooms(context).filterNot { it.isPrivate })
        }
    }

    fun getStableUserId(context: Context): String {
        val authId = AuthManager.getUserId(context)
        if (!authId.isNullOrBlank()) return authId
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString("stable_device_uid", null)
        if (deviceId.isNullOrBlank()) {
            deviceId = "dev_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("stable_device_uid", deviceId).apply()
        }
        return deviceId
    }

    suspend fun fetchRoomMembers(
        context: Context,
        room: StudyRoom,
        forceRefresh: Boolean = false
    ): Result<List<StudyRoomMember>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val roomCode = room.code

        // 1. If Developer Mock Mode is active, return mock members
        if (isDevMockActive(context)) {
            val devMembers = getDevMockMembers(context, roomCode)
            return@withContext Result.success(devMembers)
        }

        // 2. Check In-Memory Cache
        val cached = cachedMembers[roomCode]
        if (!forceRefresh && cached != null && (now - cached.first) < MEMBERS_CACHE_TTL_MS) {
            return@withContext Result.success(cached.second)
        }

        val myProfile = ProfileManager.getProfile(context)
        val myUserId = getStableUserId(context)
        val myDisplayName = myProfile.displayName.trim().ifEmpty { "Student" }
        val sharedPrefs = context.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE)
        val isStudying = (sharedPrefs.safeString("timerState", "IDLE") ?: "IDLE") == "STUDYING"
        val currentSub = SubjectTagManager.getSelectedSubject(context).name
        val elapsedMins = (sharedPrefs.safeLong("accumulatedStudy", 0L) / 60).toInt().coerceAtLeast(0)
        val initialLetter = myDisplayName.take(1).uppercase().ifEmpty { "S" }

        val selfMember = StudyRoomMember(
            id = myUserId,
            displayName = myDisplayName,
            avatarEmoji = initialLetter,
            isStudying = isStudying,
            currentSubject = if (isStudying) currentSub else "Resting",
            elapsedMinutes = elapsedMins,
            isSelf = true
        )

        val creds = getSupabaseCredentials()
        if (creds == null) {
            val singleMemberList = listOf(selfMember)
            return@withContext Result.success(singleMemberList)
        }

        val (supabaseUrl, anonKey) = creds
        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/get_room_members")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Connection", "Keep-Alive")
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("p_room_code", roomCode)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val arr = if (raw.isNotBlank()) JSONArray(raw) else JSONArray()
                val list = mutableListOf<StudyRoomMember>()
                val seenIds = mutableSetOf<String>()
                val seenNames = mutableSetOf<String>()
                // Add self first to guarantee presence and avoid duplicates
                list.add(selfMember)
                seenIds.add(myUserId)
                seenNames.add(myDisplayName.lowercase())

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val uid = obj.getString("user_id")
                    val rawName = obj.optString("user_name", "Student").trim()
                    val lowerName = rawName.lowercase()

                    // Match self by either stable UID or display name to clean up any past legacy rows
                    val isSelf = (uid == myUserId || lowerName == myDisplayName.lowercase())
                    if (isSelf) {
                        continue // Self is already added once at index 0
                    }

                    // Deduplicate peers by UID and Name
                    if (seenIds.contains(uid) || seenNames.contains(lowerName)) {
                        continue
                    }
                    seenIds.add(uid)
                    seenNames.add(lowerName)

                    val firstChar = rawName.take(1).uppercase().ifEmpty { "S" }
                    val member = StudyRoomMember(
                        id = uid,
                        displayName = rawName,
                        avatarEmoji = firstChar,
                        isStudying = obj.optBoolean("is_studying", false),
                        currentSubject = obj.optString("current_subject", "Focusing").ifEmpty { "Focusing" },
                        elapsedMinutes = (obj.optInt("elapsed_today_seconds", 0) / 60).coerceAtLeast(0),
                        isSelf = false
                    )
                    list.add(member)
                }

                cachedMembers[roomCode] = now to list
                Result.success(list)
            } else {
                Result.success(listOf(selfMember))
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchRoomMembers network exception", e)
            Result.success(listOf(selfMember))
        }
    }

    suspend fun createRoom(
        context: Context,
        name: String,
        category: String = "General",
        isPrivate: Boolean = false,
        passCode: String? = null
    ): Result<StudyRoom> = withContext(Dispatchers.IO) {
        if (!AuthManager.isLoggedIn(context) || AuthManager.isGuest(context)) {
            return@withContext Result.failure(IllegalStateException("Please sign in with Google to create a study room."))
        }

        val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { codeChars.random() }.joinToString("")
        val myProfile = ProfileManager.getProfile(context)
        val creatorName = myProfile.displayName.ifEmpty { "Student" }
        val creatorId = getStableUserId(context)

        val newRoom = StudyRoom(
            code = code,
            name = name.trim().ifEmpty { "Focus Room" },
            creatorName = creatorName,
            memberCount = 1,
            category = category,
            isPrivate = isPrivate
        )

        val creds = getSupabaseCredentials()
        if (creds == null) {
            saveUserCreatedRoomLocally(context, newRoom)
            setActiveRoom(context, newRoom)
            cachedPublicRooms = null
            return@withContext Result.success(newRoom)
        }

        val (supabaseUrl, anonKey) = creds
        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/create_study_room")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("p_code", code)
                put("p_name", newRoom.name)
                put("p_category", category)
                put("p_creator_id", creatorId)
                put("p_creator_name", creatorName)
                put("p_is_private", isPrivate)
                putNullable("p_pass_code", passCode)
                put("p_max_members", 50)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                saveUserCreatedRoomLocally(context, newRoom)
                setActiveRoom(context, newRoom)
                cachedPublicRooms = null
                Result.success(newRoom)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w(TAG, "create_study_room HTTP $responseCode: $err")
                Result.failure(Exception("Failed to create room on server (HTTP $responseCode)"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "create_study_room network error", e)
            Result.failure(e)
        }
    }

    suspend fun quickJoinRoom(
        context: Context,
        category: String = "All"
    ): Result<StudyRoom> = withContext(Dispatchers.IO) {
        if (!AuthManager.isLoggedIn(context) || AuthManager.isGuest(context)) {
            return@withContext Result.failure(IllegalStateException("Please sign in with Google to join a study room."))
        }

        // 1. Check dev mock if active
        if (isDevMockActive(context)) {
            val devRooms = getDevMockRooms(context).filterNot { it.isPrivate }
            val pick = devRooms.firstOrNull { category == "All" || it.category.equals(category, ignoreCase = true) }
                ?: devRooms.firstOrNull()
            if (pick != null) {
                setActiveRoom(context, pick)
                return@withContext Result.success(pick)
            }
        }

        // 2. Fetch public active rooms
        val roomsRes = fetchPublicRooms(context, category = "All", forceRefresh = true)
        val openRooms = roomsRes.getOrElse { emptyList() }.filterNot { it.isPrivate }

        val target = if (category != "All") {
            openRooms.firstOrNull { it.category.equals(category, ignoreCase = true) }
                ?: openRooms.maxByOrNull { it.memberCount }
                ?: openRooms.firstOrNull()
        } else {
            // Prioritize most populated room to avoid fracturing small communities
            openRooms.maxByOrNull { it.memberCount } ?: openRooms.firstOrNull()
        }

        if (target != null) {
            return@withContext joinRoom(context, target.code)
        }

        // 3. If no open public room exists, auto-create a friendly community co-study room
        val defaultCategory = if (category != "All") category else "General"
        val defaultName = if (category != "All") "$category Focus Hub" else "Global Silent Co-Study"
        createRoom(context, name = defaultName, category = defaultCategory, isPrivate = false)
    }

    suspend fun joinRoom(
        context: Context,
        roomCode: String,
        passCode: String? = null
    ): Result<StudyRoom> = withContext(Dispatchers.IO) {
        if (!AuthManager.isLoggedIn(context) || AuthManager.isGuest(context)) {
            return@withContext Result.failure(IllegalStateException("Please sign in with Google to join a study room."))
        }

        val cleanCode = roomCode.trim().uppercase()
        val myProfile = ProfileManager.getProfile(context)
        val userName = myProfile.displayName.ifEmpty { "Student" }
        val userId = getStableUserId(context)

        // Check if dev mock room
        if (isDevMockActive(context)) {
            val mockRoom = getDevMockRooms(context).find { it.code.equals(cleanCode, ignoreCase = true) }
            if (mockRoom != null) {
                setActiveRoom(context, mockRoom)
                return@withContext Result.success(mockRoom)
            }
        }

        val creds = getSupabaseCredentials()
        if (creds == null) {
            // Local fallback
            val userRoom = getUserCreatedRooms(context).find { it.code.equals(cleanCode, ignoreCase = true) }
            if (userRoom != null) {
                setActiveRoom(context, userRoom)
                return@withContext Result.success(userRoom)
            }
            return@withContext Result.failure(IllegalStateException("Internet connection required to join study room #$cleanCode"))
        }

        val (supabaseUrl, anonKey) = creds
        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/join_study_room")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("p_room_code", cleanCode)
                put("p_user_id", userId)
                put("p_user_name", userName)
                put("p_avatar_url", myProfile.avatarUrl)
                putNullable("p_pass_code", passCode)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val resp = JSONObject(raw)
                if (resp.optBoolean("success", false)) {
                    val room = StudyRoom(
                        code = resp.getString("code"),
                        name = resp.getString("name"),
                        creatorName = resp.optString("creator_name", "Student"),
                        memberCount = resp.optInt("member_count", 1),
                        category = resp.optString("category", "General"),
                        isPrivate = resp.optBoolean("is_private", false)
                    )
                    setActiveRoom(context, room)
                    cachedMembers.remove(cleanCode)
                    Result.success(room)
                } else {
                    Result.failure(Exception(resp.optString("error", "Failed to join room")))
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("HTTP $code: $err"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "join_study_room network error", e)
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(context: Context, roomCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanCode = roomCode.trim().uppercase()
        val userId = getStableUserId(context)

        setActiveRoom(context, null)
        cachedMembers.remove(cleanCode)

        val creds = getSupabaseCredentials() ?: return@withContext Result.success(Unit)
        val (supabaseUrl, anonKey) = creds

        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/leave_study_room")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("p_room_code", cleanCode)
                put("p_user_id", userId)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            conn.responseCode
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "leave_study_room network error", e)
            Result.success(Unit)
        }
    }

    suspend fun updateRoomPresenceIfActive(
        context: Context,
        isStudying: Boolean,
        subject: String?,
        colorHex: String?,
        elapsedSeconds: Int
    ) = withContext(Dispatchers.IO) {
        val active = getActiveRoom(context) ?: return@withContext
        val now = System.currentTimeMillis()

        val stateChanged = (lastReportedStudyingState != isStudying)

        // Professional Throttle:
        // 1. If study state changed (Start studying, pause, finish), sync immediately so peers see instant live status!
        // 2. Steady state heartbeats: throttled to 30s while studying, 60s while resting to keep server completely load-free.
        if (!stateChanged) {
            val cooldown = if (isStudying) 30_000L else 60_000L
            if ((now - lastPresenceSyncTimestamp) < cooldown) {
                return@withContext
            }
        }
        lastPresenceSyncTimestamp = now
        lastReportedStudyingState = isStudying
        cachedMembers.remove(active.code)

        val creds = getSupabaseCredentials() ?: return@withContext
        val myProfile = ProfileManager.getProfile(context)
        val userId = getStableUserId(context)
        val (supabaseUrl, anonKey) = creds

        try {
            val url = URL("$supabaseUrl/rest/v1/rpc/update_room_presence")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
            }

            val body = JSONObject().apply {
                put("p_room_code", active.code)
                put("p_user_id", userId)
                put("p_user_name", myProfile.displayName.ifEmpty { "Student" })
                put("p_avatar_url", myProfile.avatarUrl)
                put("p_is_studying", isStudying)
                put("p_current_subject", subject ?: "")
                put("p_subject_color", colorHex ?: "#3b82f6")
                put("p_elapsed_seconds", elapsedSeconds)
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            conn.responseCode
        } catch (_: Exception) {}
    }

    // =========================================================================
    // Developer Menu Mock Suite Engine
    // Completely isolated from production data; stored in separate dev keys
    // =========================================================================

    fun isDevMockActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DEV_MOCK_ACTIVE, false)
    }

    fun seedDevMockRooms(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mockRooms = listOf(
            StudyRoom("SILENT", "Silent Library", "System", 24, "Silent", false),
            StudyRoom("CODE42", "Coding Sprint", "Alex", 12, "Coding", false),
            StudyRoom("EXAM99", "Exam Prep & Drills", "Priya", 18, "Exams", false),
            StudyRoom("POMO25", "Pomodoro Focus 50/10", "Kenji", 15, "Pomodoro", false),
            StudyRoom("PRIV88", "Private Study Circle", "Maya", 4, "General", true)
        )

        val arr = JSONArray()
        for (r in mockRooms) {
            arr.put(JSONObject().apply {
                put("code", r.code)
                put("name", r.name)
                put("creatorName", r.creatorName)
                put("memberCount", r.memberCount)
                put("category", r.category)
                put("isPrivate", r.isPrivate)
                put("createdAt", r.createdAt)
            })
        }

        val editor = prefs.edit()
        editor.putBoolean(KEY_DEV_MOCK_ACTIVE, true)
        editor.putString(KEY_DEV_MOCK_ROOMS, arr.toString())

        // Seed realistic peers for each mock room
        val samplePeers = listOf(
            Triple("Aarav", "Math", 45),
            Triple("Elena", "Physics", 62),
            Triple("Lucas", "Coding", 30),
            Triple("Mei", "Chemistry", 55),
            Triple("Liam", "Literature", 20)
        )

        for (room in mockRooms) {
            val memberArr = JSONArray()
            for (p in samplePeers) {
                memberArr.put(JSONObject().apply {
                    put("id", "dev_${p.first.lowercase()}")
                    put("name", p.first)
                    put("subject", p.second)
                    put("minutes", p.third)
                    put("isStudying", true)
                })
            }
            editor.putString(KEY_DEV_MOCK_MEMBERS_PREFIX + room.code, memberArr.toString())
        }
        editor.apply()

        // Set default active room to SILENT for instant preview
        setActiveRoom(context, mockRooms[0])
        cachedPublicRooms = null
        cachedMembers.clear()
    }

    fun clearDevMockRooms(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(KEY_DEV_MOCK_ACTIVE)
        editor.remove(KEY_DEV_MOCK_ROOMS)
        for (code in listOf("SILENT", "CODE42", "EXAM99", "POMO25", "PRIV88")) {
            editor.remove(KEY_DEV_MOCK_MEMBERS_PREFIX + code)
        }
        editor.apply()

        setActiveRoom(context, null)
        cachedPublicRooms = null
        cachedMembers.clear()
    }

    private fun getDevMockRooms(context: Context): List<StudyRoom> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DEV_MOCK_ROOMS, "[]") ?: "[]"
        val list = mutableListOf<StudyRoom>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StudyRoom(
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        creatorName = obj.optString("creatorName", "System"),
                        memberCount = obj.optInt("memberCount", 1),
                        category = obj.optString("category", "General"),
                        isPrivate = obj.optBoolean("isPrivate", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun getDevMockMembers(context: Context, roomCode: String): List<StudyRoomMember> {
        val myProfile = ProfileManager.getProfile(context)
        val myInitial = myProfile.displayName.trim().take(1).uppercase().ifEmpty { "S" }

        val list = mutableListOf<StudyRoomMember>()
        list.add(
            StudyRoomMember(
                id = "self",
                displayName = "${myProfile.displayName.ifEmpty { "Student" }} (You)",
                avatarEmoji = myInitial,
                isStudying = true,
                currentSubject = "Focus",
                elapsedMinutes = 25,
                isSelf = true
            )
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DEV_MOCK_MEMBERS_PREFIX + roomCode, "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.getString("name")
                list.add(
                    StudyRoomMember(
                        id = obj.getString("id"),
                        displayName = name,
                        avatarEmoji = name.take(1).uppercase(),
                        isStudying = obj.optBoolean("isStudying", true),
                        currentSubject = obj.optString("subject", "Focusing"),
                        elapsedMinutes = obj.optInt("minutes", 15),
                        isSelf = false
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun injectDevMockPeer(context: Context, roomCode: String, name: String, subject: String, isStudying: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DEV_MOCK_MEMBERS_PREFIX + roomCode, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        arr.put(JSONObject().apply {
            put("id", "dev_injected_${System.currentTimeMillis()}")
            put("name", name)
            put("subject", subject)
            put("minutes", (10..75).random())
            put("isStudying", isStudying)
        })
        prefs.edit().putString(KEY_DEV_MOCK_MEMBERS_PREFIX + roomCode, arr.toString()).apply()
        cachedMembers.remove(roomCode)
    }
}
