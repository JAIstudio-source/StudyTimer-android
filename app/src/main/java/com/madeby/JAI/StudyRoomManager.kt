package com.madeby.JAI

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

data class StudyRoom(
    val code: String,
    val name: String,
    val creatorName: String,
    val memberCount: Int,
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
    val isSelf: Boolean = false
)

object StudyRoomManager {

    private const val PREFS_NAME = "studytimer_study_rooms"
    private const val KEY_ACTIVE_ROOM = "active_room_json"
    private const val KEY_USER_ROOMS = "user_created_rooms_json"

    val DEFAULT_PUBLIC_ROOMS = listOf(
        StudyRoom("SILENT", "Silent Library 🏛️", "StudyTimer", 28),
        StudyRoom("NIGHT1", "Late Night Grinders 🌙", "Alex", 14),
        StudyRoom("DEV101", "STEM & Coding 💻", "Priya", 19),
        StudyRoom("POMO25", "Pomodoro Sprint Group 🍅", "Kenji", 22)
    )

    fun getActiveRoom(context: Context): StudyRoom? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ACTIVE_ROOM, null) ?: return null
        return try {
            val json = JSONObject(raw)
            StudyRoom(
                code = json.getString("code"),
                name = json.getString("name"),
                creatorName = json.optString("creatorName", "User"),
                memberCount = json.optInt("memberCount", 1),
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
                put("isPrivate", room.isPrivate)
                put("createdAt", room.createdAt)
            }
            prefs.edit().putString(KEY_ACTIVE_ROOM, json.toString()).apply()
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
                        isPrivate = obj.optBoolean("isPrivate", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun createRoom(context: Context, name: String, isPrivate: Boolean = false): StudyRoom {
        val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { codeChars.random() }.joinToString("")
        val myProfile = ProfileManager.getProfile(context)
        val newRoom = StudyRoom(
            code = code,
            name = name.trim().ifEmpty { "Focus Group" },
            creatorName = myProfile.displayName.ifEmpty { "You" },
            memberCount = 1,
            isPrivate = isPrivate
        )

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = getUserCreatedRooms(context).toMutableList()
        existing.add(0, newRoom)

        val arr = JSONArray()
        for (r in existing) {
            arr.put(JSONObject().apply {
                put("code", r.code)
                put("name", r.name)
                put("creatorName", r.creatorName)
                put("memberCount", r.memberCount)
                put("isPrivate", r.isPrivate)
                put("createdAt", r.createdAt)
            })
        }
        prefs.edit().putString(KEY_USER_ROOMS, arr.toString()).apply()
        setActiveRoom(context, newRoom)
        return newRoom
    }

    fun getRoomMembers(activity: MainActivity, room: StudyRoom): List<StudyRoomMember> {
        val myProfile = ProfileManager.getProfile(activity)
        val isStudying = activity.currentTimerState == TimerState.STUDYING
        val currentSub = SubjectTagManager.getSelectedSubject(activity).name
        val elapsedMins = (activity.accumulatedStudy / 60).toInt().coerceAtLeast(1)

        val members = mutableListOf<StudyRoomMember>()
        // 1. Current user
        val initialLetter = myProfile.displayName.trim().take(1).uppercase()
        members.add(
            StudyRoomMember(
                id = "self",
                displayName = "${myProfile.displayName.ifEmpty { "You" }} (You)",
                avatarEmoji = if (initialLetter.isNotEmpty()) initialLetter else "🎓",
                isStudying = isStudying,
                currentSubject = if (isStudying) currentSub else "Break",
                elapsedMinutes = if (isStudying) elapsedMins else 0,
                isSelf = true
            )
        )

        // 2. Room Peers (Simulated active room presence synced to room code)
        val seed = room.code.hashCode().toLong()
        val random = java.util.Random(seed)
        val peerNames = listOf("Aarav", "Elena", "Lucas", "Mei", "Liam", "Zainab", "Mateo", "Sophie")
        val peerEmojis = listOf("🦊", "🦁", "🐼", "🚀", "🦉", "⚡", "🌟", "📚")
        val peerSubjects = listOf("Math", "Physics", "Coding", "Biology", "Literature", "Chemistry")

        val peerCount = (room.memberCount - 1).coerceIn(2, 6)
        for (i in 0 until peerCount) {
            val name = peerNames[(i + random.nextInt(peerNames.size)) % peerNames.size]
            val emoji = peerEmojis[(i + random.nextInt(peerEmojis.size)) % peerEmojis.size]
            val subject = peerSubjects[(i + random.nextInt(peerSubjects.size)) % peerSubjects.size]
            val peerStudying = i % 4 != 3 // 75% actively studying
            val mins = (15 + (i * 12) + random.nextInt(25)).coerceAtLeast(5)

            members.add(
                StudyRoomMember(
                    id = "peer_$i",
                    displayName = name,
                    avatarEmoji = emoji,
                    isStudying = peerStudying,
                    currentSubject = if (peerStudying) subject else "Resting",
                    elapsedMinutes = if (peerStudying) mins else 0
                )
            )
        }
        return members
    }

    fun showStudyRoomsDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val primary = themeCoordinator.primaryColor
        fun dp(v: Int): Int = activity.dp(v)

        fun renderContent() {
            val activeRoom = getActiveRoom(activity)
            val container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createDialogBackground(28f)
                setPadding(dp(22), dp(20), dp(22), dp(20))
            }

            if (activeRoom != null) {
                // ACTIVE ROOM VIEW
                val topRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                topRow.addView(TextView(activity).apply {
                    text = "ACTIVE STUDY ROOM"
                    textSize = 11.5f
                    letterSpacing = 0.14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(primary)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                val leaveBtn = TextView(activity).apply {
                    text = "Leave Room"
                    textSize = 12f
                    setTextColor(if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt())
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        setActiveRoom(activity, null)
                        Toast.makeText(activity, "Left study room", Toast.LENGTH_SHORT).show()
                        renderContent()
                    }
                }
                topRow.addView(leaveBtn)
                container.addView(topRow)

                container.addView(TextView(activity).apply {
                    text = activeRoom.name
                    textSize = 21f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.textColor)
                    setPadding(0, dp(4), 0, dp(2))
                })

                // Code Pill
                val codePillRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, dp(12))
                }
                codePillRow.addView(TextView(activity).apply {
                    text = "Room Code: ${activeRoom.code} 📋 (Tap to copy)"
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(primary)
                    background = themeCoordinator.createGlassChip(tintedColor(primary, 40), 10f)
                    setPadding(dp(10), dp(4), dp(10), dp(4))
                    setOnClickListener {
                        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Room Code", activeRoom.code))
                        Toast.makeText(activity, "Room Code ${activeRoom.code} copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                })
                container.addView(codePillRow)

                // Members List Header
                val members = getRoomMembers(activity, activeRoom)
                val activeStudying = members.count { it.isStudying }
                container.addView(TextView(activity).apply {
                    text = "LIVE PARTICIPANTS (${activeStudying}/${members.size} Focusing)"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (isDark) 0x88FFFFFF.toInt() else 0xFF64748B.toInt())
                    setPadding(0, dp(4), 0, dp(8))
                })

                // Members Grid / List
                val membersList = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 0, 0, dp(14))
                }
                for (m in members) {
                    val row = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = if (isDark) {
                            themeCoordinator.createGlassChip(if (m.isStudying) 0x2210B981.toInt() else 0x1A64748B.toInt(), 14f)
                        } else {
                            GradientDrawable().apply {
                                cornerRadius = dp(14).toFloat()
                                setColor(if (m.isStudying) 0xFFECFDF5.toInt() else 0xFFF1F5F9.toInt())
                                setStroke(dp(1), if (m.isStudying) 0xFFA7F3D0.toInt() else 0xFFE2E8F0.toInt())
                            }
                        }
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(6))
                        }
                    }

                    // Avatar
                    row.addView(TextView(activity).apply {
                        text = m.avatarEmoji
                        textSize = 20f
                        setPadding(0, 0, dp(10), 0)
                    })

                    // Name + Subject
                    val col = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    col.addView(TextView(activity).apply {
                        text = m.displayName
                        textSize = 13.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(themeCoordinator.textColor)
                    })
                    col.addView(TextView(activity).apply {
                        text = if (m.isStudying) "Focusing on ${m.currentSubject}" else "Taking a break"
                        textSize = 11.5f
                        setTextColor(if (m.isStudying) (if (isDark) 0xFF34D399.toInt() else 0xFF059669.toInt()) else (if (isDark) 0x88FFFFFF.toInt() else 0xFF64748B.toInt()))
                    })
                    row.addView(col)

                    // Timer badge
                    if (m.isStudying) {
                        row.addView(TextView(activity).apply {
                            text = "⚡ ${m.elapsedMinutes}m"
                            textSize = 12f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            setTextColor(if (isDark) 0xFF34D399.toInt() else 0xFF059669.toInt())
                            background = themeCoordinator.createGlassChip(0x3310B981.toInt(), 10f)
                            setPadding(dp(8), dp(3), dp(8), dp(3))
                        })
                    } else {
                        row.addView(TextView(activity).apply {
                            text = "☕ Rest"
                            textSize = 11.5f
                            setTextColor(if (isDark) 0x88FFFFFF.toInt() else 0xFF64748B.toInt())
                        })
                    }
                    membersList.addView(row)
                }
                container.addView(membersList)

            } else {
                // NO ROOM ACTIVE - LOBBY VIEW
                container.addView(TextView(activity).apply {
                    text = "👥 LIVE STUDY ROOMS"
                    textSize = 11.5f
                    letterSpacing = 0.14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(primary)
                    setPadding(0, 0, 0, dp(4))
                })

                container.addView(TextView(activity).apply {
                    text = "Study Together with Friends"
                    textSize = 21f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(themeCoordinator.textColor)
                    setPadding(0, 0, 0, dp(6))
                })

                container.addView(TextView(activity).apply {
                    text = "Join a live room to study with friends in real-time, share timers, and stay accountable."
                    textSize = 13f
                    setTextColor(themeCoordinator.textColor)
                    alpha = 0.75f
                    setLineSpacing(dp(2).toFloat(), 1.15f)
                    setPadding(0, 0, 0, dp(14))
                })

                // Code Input Row
                val joinRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, dp(14))
                }
                val codeInput = EditText(activity).apply {
                    hint = "ENTER 6-DIGIT CODE"
                    setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
                    setTextColor(themeCoordinator.textColor)
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    filters = arrayOf(android.text.InputFilter.LengthFilter(8), android.text.InputFilter.AllCaps())
                    background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, dp(8), 0)
                    }
                }
                val joinCodeBtn = TextView(activity).apply {
                    text = "Join"
                    textSize = 13f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(primary)
                    }
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    setOnClickListener {
                        val inputCode = codeInput.text.toString().trim().uppercase()
                        if (inputCode.length < 4) {
                            Toast.makeText(activity, "Please enter a valid room code", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val allRooms = DEFAULT_PUBLIC_ROOMS + getUserCreatedRooms(activity)
                        val match = allRooms.find { it.code.equals(inputCode, ignoreCase = true) }
                            ?: StudyRoom(code = inputCode, name = "Private Room #$inputCode", creatorName = "Friend", memberCount = 4)
                        setActiveRoom(activity, match)
                        Toast.makeText(activity, "Joined ${match.name}! 🎉", Toast.LENGTH_SHORT).show()
                        renderContent()
                    }
                }
                joinRow.addView(codeInput)
                joinRow.addView(joinCodeBtn)
                container.addView(joinRow)

                // Create New Room Button
                val createRoomBtn = TextView(activity).apply {
                    text = "+ Create Study Room"
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isDark) Color.WHITE else 0xFF0F172A.toInt())
                    background = if (isDark) {
                        themeCoordinator.createGlassChip(tintedColor(primary, 60), 14f)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(14).toFloat()
                            setColor(0xFFF1F5F9.toInt())
                            setStroke(dp(1), 0xFFCBD5E1.toInt())
                        }
                    }
                    gravity = Gravity.CENTER
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dp(14))
                    }
                    setOnClickListener {
                        showCreateRoomDialog(activity, themeCoordinator) {
                            renderContent()
                        }
                    }
                }
                container.addView(createRoomBtn)

                // Public Rooms List Header
                container.addView(TextView(activity).apply {
                    text = "EXPLORE OPEN ROOMS"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (isDark) 0x88FFFFFF.toInt() else 0xFF64748B.toInt())
                    setPadding(0, dp(4), 0, dp(8))
                })

                val publicList = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                for (r in DEFAULT_PUBLIC_ROOMS) {
                    val roomCard = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = if (isDark) {
                            themeCoordinator.createGlassChip(0x22FFFFFF.toInt(), 14f)
                        } else {
                            GradientDrawable().apply {
                                cornerRadius = dp(14).toFloat()
                                setColor(0xFFF8FAFC.toInt())
                                setStroke(dp(1), 0xFFE2E8F0.toInt())
                            }
                        }
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(8))
                        }
                    }

                    val col = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    col.addView(TextView(activity).apply {
                        text = r.name
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(themeCoordinator.textColor)
                    })
                    col.addView(TextView(activity).apply {
                        text = "🟢 ${r.memberCount} members studying · Code: ${r.code}"
                        textSize = 11.5f
                        setTextColor(if (isDark) 0x88FFFFFF.toInt() else 0xFF64748B.toInt())
                        setPadding(0, dp(2), 0, 0)
                    })
                    roomCard.addView(col)

                    val joinThisBtn = TextView(activity).apply {
                        text = "Join"
                        textSize = 12.5f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(Color.WHITE)
                        background = GradientDrawable().apply {
                            cornerRadius = dp(10).toFloat()
                            setColor(primary)
                        }
                        setPadding(dp(14), dp(6), dp(14), dp(6))
                        setOnClickListener {
                            setActiveRoom(activity, r)
                            Toast.makeText(activity, "Joined ${r.name}! 🎉", Toast.LENGTH_SHORT).show()
                            renderContent()
                        }
                    }
                    roomCard.addView(joinThisBtn)
                    publicList.addView(roomCard)
                }
                container.addView(publicList)
            }

            // Bottom Close Button
            val closeBtn = TextView(activity).apply {
                text = "Close"
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(primary)
                }
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                    setMargins(0, dp(12), 0, 0)
                }
                setOnClickListener { dialog.dismiss() }
            }
            container.addView(closeBtn)

            val scroll = ScrollView(activity).apply {
                isVerticalScrollBarEnabled = false
                addView(container)
            }
            dialog.setContentView(scroll)
        }

        renderContent()
        dialog.window?.let { win ->
            win.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.90f).toInt(),
                (activity.resources.displayMetrics.heightPixels * 0.85f).toInt()
            )
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun showCreateRoomDialog(activity: MainActivity, themeCoordinator: ThemeCoordinator, onCreated: () -> Unit) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        fun dp(v: Int): Int = activity.dp(v)

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        container.addView(TextView(activity).apply {
            text = "Create Study Room"
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(themeCoordinator.textColor)
            setPadding(0, 0, 0, dp(10))
        })

        val nameInput = EditText(activity).apply {
            hint = "Room Name (e.g. Focus Squad 🎯)"
            setHintTextColor(tintedColor(themeCoordinator.textColor, 120))
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 12f)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        container.addView(nameInput)

        val createBtn = TextView(activity).apply {
            text = "Create & Join"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(14), 0, 0)
            }
            setOnClickListener {
                val rName = nameInput.text.toString().trim()
                if (rName.isEmpty()) {
                    Toast.makeText(activity, "Please enter a room name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val room = createRoom(activity, rName)
                Toast.makeText(activity, "Created room: ${room.name} (${room.code})", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onCreated()
            }
        }
        container.addView(createBtn)

        dialog.setContentView(container)
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.85f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun tintedColor(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
