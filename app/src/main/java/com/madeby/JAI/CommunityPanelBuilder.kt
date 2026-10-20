package com.madeby.JAI

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unified Community Hub combining real-time Study Rooms and Global/Friend Leaderboards.
 * Clean, lightweight, professional UI with zero dummy data in production.
 */
class CommunityPanelBuilder(private val host: MainActivity) {

    enum class CommunitySegment {
        ROOMS,
        LEADERBOARD
    }

    private val density = host.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    private val themeCoordinator get() = host.themeCoordinator
    private var activeSegment = CommunitySegment.LEADERBOARD
    private var selectedCategory: String = "All"
    private val categories = listOf("All", "General", "Coding", "Exams", "Pomodoro", "Silent")
    private var isBrowsingExplore = false
    private var rootContentHost: FrameLayout? = null
    private var updateSegmentStylesFunc: (() -> Unit)? = null
    private var roomPollingJob: Job? = null

    fun switchSegment(segment: CommunitySegment) {
        roomPollingJob?.cancel()
        roomPollingJob = null
        activeSegment = segment
        updateSegmentStylesFunc?.invoke()
        val ch = rootContentHost ?: return
        ch.removeAllViews()
        if (activeSegment == CommunitySegment.ROOMS) {
            renderRoomsView(ch)
        } else {
            renderLeaderboardView(ch)
        }
        FontStyleManager.applyRecursively(ch)
    }

    fun build(target: ViewGroup = host.panelContainer, initialSegment: CommunitySegment = CommunitySegment.LEADERBOARD) {
        activeSegment = initialSegment

        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Universal Top Header
        val header = UniversalHeaderHelper.createHeader(host, title = "COMMUNITY")
        root.addView(header)

        // 2. Segment Switcher [ Study Rooms | Leaderboard ]
        val switcherContainer = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = themeCoordinator.createCardBackground()
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                setMargins(dp(12), dp(2), dp(12), dp(8))
            }
        }

        val contentHost = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        rootContentHost = contentHost

        val roomsTab = TextView(host).apply {
            text = "Study Rooms"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        val leaderboardTab = TextView(host).apply {
            text = "Leaderboard"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }

        fun updateSegmentStyles() {
            val primary = themeCoordinator.primaryColor

            fun pillBg(selected: Boolean) = if (selected) {
                GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(primary)
                }
            } else {
                null
            }

            roomsTab.background = pillBg(activeSegment == CommunitySegment.ROOMS)
            roomsTab.setTextColor(if (activeSegment == CommunitySegment.ROOMS) Color.WHITE else themeCoordinator.textColor)
            roomsTab.alpha = if (activeSegment == CommunitySegment.ROOMS) 1.0f else 0.65f

            leaderboardTab.background = pillBg(activeSegment == CommunitySegment.LEADERBOARD)
            leaderboardTab.setTextColor(if (activeSegment == CommunitySegment.LEADERBOARD) Color.WHITE else themeCoordinator.textColor)
            leaderboardTab.alpha = if (activeSegment == CommunitySegment.LEADERBOARD) 1.0f else 0.65f
        }

        updateSegmentStylesFunc = { updateSegmentStyles() }

        fun renderContent() {
            contentHost.removeAllViews()
            updateSegmentStyles()
            if (activeSegment == CommunitySegment.ROOMS) {
                renderRoomsView(contentHost)
            } else {
                renderLeaderboardView(contentHost)
            }
        }

        roomsTab.setOnClickListener {
            if (activeSegment != CommunitySegment.ROOMS) {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                activeSegment = CommunitySegment.ROOMS
                renderContent()
            }
        }

        leaderboardTab.setOnClickListener {
            if (activeSegment != CommunitySegment.LEADERBOARD) {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                activeSegment = CommunitySegment.LEADERBOARD
                renderContent()
            }
        }

        switcherContainer.addView(leaderboardTab)
        switcherContainer.addView(roomsTab)
        root.addView(switcherContainer)
        root.addView(contentHost)

        renderContent()

        FontStyleManager.applyRecursively(root)
        target.removeAllViews()
        target.addView(root)
    }

    private fun renderLeaderboardView(container: FrameLayout) {
        val leaderboardContainer = FrameLayout(host).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(0, 0, 0, dp(80))
        }
        LeaderboardPanelBuilder(host).build(leaderboardContainer, showBackButton = false)
        container.addView(leaderboardContainer)
    }

    private fun renderRoomsView(container: FrameLayout) {
        val scroll = ScrollView(host).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(84))
        }

        val primary = themeCoordinator.primaryColor
        val isDark = themeCoordinator.isDarkMode()
        val textColor = themeCoordinator.textColor

        val activeRoom = StudyRoomManager.getActiveRoom(host)

        // =========================================================================
        // MODE 1: ACTIVE STUDY ROOM VIEW (Spacious, Focused, Distraction-Free)
        // Only shown when the user is currently inside a room and not exploring
        // =========================================================================
        if (activeRoom != null && !isBrowsingExplore) {
            val roomCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(18), dp(18), dp(18), dp(18))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Top Row: Title + Leave Room
            val titleRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            titleRow.addView(TextView(host).apply {
                text = activeRoom.name
                textSize = 21f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            val leaveBtn = TextView(host).apply {
                text = "Leave Room"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt())
                background = themeCoordinator.createGlassChip(if (isDark) 0x22EF4444.toInt() else 0x1AEF4444.toInt(), 10f)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        roomPollingJob?.cancel()
                        roomPollingJob = null
                        StudyRoomManager.leaveRoom(host, activeRoom.code)
                        AppToast.show(host, "Left study room")
                        renderRoomsView(container)
                    }
                }
            }
            titleRow.addView(leaveBtn)
            roomCard.addView(titleRow)

            // Tags row: [🔒 Private] [Category] [#Code 📋]
            val tagsRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(14))
            }

            if (activeRoom.isPrivate) {
                tagsRow.addView(TextView(host).apply {
                    text = "Private"
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isDark) 0xFFA78BFA.toInt() else 0xFF7C3AED.toInt())
                    background = themeCoordinator.createGlassChip(if (isDark) 0x338B5CF6.toInt() else 0x227C3AED.toInt(), 8f)
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = dp(6)
                    }
                })
            }

            tagsRow.addView(TextView(host).apply {
                text = activeRoom.category
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
                background = themeCoordinator.createGlassChip(host.tintedColor(primary, 35), 8f)
                setPadding(dp(8), dp(3), dp(8), dp(3))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dp(6)
                }
            })

            val codePill = TextView(host).apply {
                text = "#${activeRoom.code} (Copy)"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
                alpha = 0.85f
                background = themeCoordinator.createGlassChip(host.tintedColor(textColor, 25), 8f)
                setPadding(dp(8), dp(3), dp(8), dp(3))
                setOnClickListener {
                    val cm = host.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("Room Code", activeRoom.code))
                    AppToast.show(host, "Room code #${activeRoom.code} copied")
                }
            }
            tagsRow.addView(codePill)
            roomCard.addView(tagsRow)

            // Clean Actions Row: [ ↗ Share Invite ] & [ 🌐 Browse All Rooms ]
            val roomActionsRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(16)
                }
            }

            val shareAction = TextView(host).apply {
                text = "Share Invite"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(primary)
                }
                setPadding(dp(14), dp(8), dp(14), dp(8))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
                setOnClickListener {
                    try {
                        val inviteUrl = "https://get-studytimer.vercel.app/"
                        val shareText = if (activeRoom.isPrivate) {
                            "Join my private study room '${activeRoom.name}' on StudyTimer!\nRoom Code: ${activeRoom.code}\nDownload the app: $inviteUrl"
                        } else {
                            "Join my study room '${activeRoom.name}' on StudyTimer!\nRoom Code: ${activeRoom.code}\nDownload the app: $inviteUrl"
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        host.startActivity(Intent.createChooser(shareIntent, "Share Study Room"))
                    } catch (_: Exception) {
                        val cm = host.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Room Code", activeRoom.code))
                        AppToast.show(host, "Invite copied to clipboard")
                    }
                }
            }
            roomActionsRow.addView(shareAction)

            val browseOtherBtn = TextView(host).apply {
                text = "Browse Rooms"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
                background = themeCoordinator.createGlassChip(host.tintedColor(textColor, 30), 12f)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(6)
                }
                setOnClickListener {
                    roomPollingJob?.cancel()
                    roomPollingJob = null
                    isBrowsingExplore = true
                    renderRoomsView(container)
                }
            }
            roomActionsRow.addView(browseOtherBtn)
            roomCard.addView(roomActionsRow)

            // Members Roster Header with Active Studying Count in Green
            val membersHeaderRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(10))
            }
            val membersLabel = TextView(host).apply {
                text = "Study Partners"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val activeCountBadge = TextView(host).apply {
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(0xFF10B981.toInt())
                background = themeCoordinator.createGlassChip(0x2610B981.toInt(), 10f)
                setPadding(dp(10), dp(4), dp(10), dp(4))
                visibility = View.GONE
            }

            val refreshBtn = TextView(host).apply {
                text = "↻ Refresh"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
                background = themeCoordinator.createGlassChip(host.tintedColor(primary, 25), 8f)
                setPadding(dp(8), dp(3), dp(8), dp(3))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dp(8)
                }
            }

            membersHeaderRow.addView(membersLabel)
            membersHeaderRow.addView(refreshBtn)
            membersHeaderRow.addView(activeCountBadge)
            roomCard.addView(membersHeaderRow)

            val membersContainer = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
            }
            roomCard.addView(membersContainer)
            content.addView(roomCard)

            // Direct Focus Action Button
            val isStudying = (host.currentTimerState == TimerState.STUDYING)
            val focusCard = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = if (isStudying) {
                    themeCoordinator.createGlassChip(host.tintedColor(textColor, 24), 14f)
                } else {
                    GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(primary)
                    }
                }
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                }
                setOnClickListener {
                    host.navigateToPanel(AppPanel.FOCUS)
                }
            }
            focusCard.addView(TextView(host).apply {
                text = if (isStudying) "Focus session connected in room" else "Start Focusing in this Room"
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isStudying) textColor else Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            focusCard.addView(TextView(host).apply {
                text = "Timer →"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(if (isStudying) primary else Color.WHITE)
            })
            content.addView(focusCard)

            refreshBtn.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                refreshMembersList(activeRoom, membersContainer, membersLabel, activeCountBadge, forceRefresh = true)
                AppToast.show(host, "Refreshed live study partners")
            }

            // Initial load of study partners
            refreshMembersList(activeRoom, membersContainer, membersLabel, activeCountBadge, forceRefresh = false)

            // Live presence polling loop while this room is open (every 12 seconds)
            roomPollingJob?.cancel()
            roomPollingJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive && activeSegment == CommunitySegment.ROOMS && !isBrowsingExplore && StudyRoomManager.getActiveRoom(host)?.code == activeRoom.code) {
                    delay(12_000L)
                    if (isActive && activeSegment == CommunitySegment.ROOMS && !isBrowsingExplore) {
                        refreshMembersList(activeRoom, membersContainer, membersLabel, activeCountBadge, forceRefresh = true)
                    }
                }
            }

            scroll.addView(content)
            container.removeAllViews()
            container.addView(scroll)
            return
        }

        // =========================================================================
        // MODE 2: EXPLORE ROOMS DIRECTORY (When not in a room, or browsing all rooms)
        // =========================================================================

        // If user is browsing explore while having an active room, show return banner
        if (activeRoom != null && isBrowsingExplore) {
            val returnBanner = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createGlassChip(host.tintedColor(primary, 40), 12f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
                setOnClickListener {
                    isBrowsingExplore = false
                    renderRoomsView(container)
                }
            }
            returnBanner.addView(TextView(host).apply {
                text = "← Back to My Room: ${activeRoom.name}"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            returnBanner.addView(TextView(host).apply {
                text = "Open"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
            })
            content.addView(returnBanner)
        }

        val isLoggedIn = AuthManager.isLoggedIn(host) && !AuthManager.isGuest(host)

        if (!isLoggedIn) {
            // =========================================================================
            // GUEST BANNER: Sign-in CTA for live co-study rooms
            // =========================================================================
            val guestCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(18f)
                setPadding(dp(18), dp(16), dp(18), dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(14)
                }
            }

            val guestTopRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            guestTopRow.addView(TextView(host).apply {
                text = "👥"
                textSize = 20f
                setPadding(0, 0, dp(10), 0)
            })
            val guestTextCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            guestTextCol.addView(TextView(host).apply {
                text = "Sign In for Live Study Rooms"
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
            })
            guestTextCol.addView(TextView(host).apply {
                text = "Real-time co-studying is synchronized for signed-in students. Sign in with Google to join rooms, share live status, and climb rankings."
                textSize = 12f
                alpha = 0.75f
                setTextColor(textColor)
                setPadding(0, dp(2), 0, 0)
            })
            guestTopRow.addView(guestTextCol)
            guestCard.addView(guestTopRow)

            val googleBtn = TextView(host).apply {
                text = "Sign in with Google"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(primary)
                }
                setPadding(dp(18), dp(10), dp(18), dp(10))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
                setOnClickListener {
                    val intent = Intent(host, LoginActivity::class.java).apply {
                        putExtra("auto_google", true)
                    }
                    host.startActivity(intent)
                }
            }
            guestCard.addView(googleBtn)
            content.addView(guestCard)
        } else {
            // =========================================================================
            // HERO QUICK-ACTION CARD (Option A + B Merged)
            // =========================================================================
            val heroCard = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                background = themeCoordinator.createCardBackground(20f)
                setPadding(dp(18), dp(16), dp(18), dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            val heroHeader = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            heroHeader.addView(TextView(host).apply {
                text = "1-TAP CO-STUDY"
                textSize = 10.5f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            heroCard.addView(heroHeader)

            val heroTitle = TextView(host).apply {
                text = "Study with Others Now"
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
                setPadding(0, dp(4), 0, dp(2))
            }
            heroCard.addView(heroTitle)

            val heroSubtitle = TextView(host).apply {
                text = "Jump directly into an active co-study room with silent peer accountability."
                textSize = 12f
                alpha = 0.75f
                setTextColor(textColor)
                setPadding(0, 0, 0, dp(12))
            }
            heroCard.addView(heroSubtitle)

            val quickJoinBtn = TextView(host).apply {
                text = "Quick Join Any Room"
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(primary)
                }
                setPadding(dp(18), dp(12), dp(18), dp(12))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    ensureLoggedIn("quick join a study room") {
                        text = "Finding active room..."
                        isEnabled = false
                        CoroutineScope(Dispatchers.Main).launch {
                            val res = StudyRoomManager.quickJoinRoom(host, category = selectedCategory)
                            isEnabled = true
                            text = "Quick Join Any Room"
                            if (res.isSuccess) {
                                val room = res.getOrThrow()
                                AppToast.show(host, "Connected to ${room.name}!")
                                isBrowsingExplore = false
                                renderRoomsView(container)
                            } else {
                                AppToast.show(host, "Could not join room. Please try again.")
                            }
                        }
                    }
                }
            }
            heroCard.addView(quickJoinBtn)

            // Secondary Action Row: [ 🔑 Enter Code ] • [ + Create Room ]
            val secondaryRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(2))
            }

            val enterCodeLink = TextView(host).apply {
                text = "Have a Room Code?"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
                background = themeCoordinator.createGlassChip(host.tintedColor(primary, 25), 10f)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    ensureLoggedIn("join with a room code") {
                        showJoinRoomDialog {
                            isBrowsingExplore = false
                            renderRoomsView(container)
                        }
                    }
                }
            }
            secondaryRow.addView(enterCodeLink)

            val spacer = View(host).apply {
                layoutParams = LinearLayout.LayoutParams(dp(10), dp(1))
            }
            secondaryRow.addView(spacer)

            val createRoomLink = TextView(host).apply {
                text = "+ Create Room"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
                background = themeCoordinator.createGlassChip(host.tintedColor(textColor, 25), 10f)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    ensureLoggedIn("create a study room") {
                        showCreateRoomDialog {
                            isBrowsingExplore = false
                            renderRoomsView(container)
                        }
                    }
                }
            }
            secondaryRow.addView(createRoomLink)
            heroCard.addView(secondaryRow)

            content.addView(heroCard)

            // =========================================================================
            // MINI LEADERBOARD SNAPSHOT (Option B)
            // =========================================================================
            val miniLeaderboard = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createGlassChip(host.tintedColor(textColor, 20), 16f)
                setPadding(dp(14), dp(11), dp(14), dp(11))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(14)
                }
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    switchSegment(CommunitySegment.LEADERBOARD)
                }
            }

            miniLeaderboard.addView(TextView(host).apply {
                text = "🏆"
                textSize = 18f
                setPadding(0, 0, dp(10), 0)
            })

            val miniCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            miniCol.addView(TextView(host).apply {
                text = "Global Leaderboard"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
            })
            miniCol.addView(TextView(host).apply {
                text = "See daily rankings & focus streaks"
                textSize = 11f
                alpha = 0.65f
                setTextColor(textColor)
            })
            miniLeaderboard.addView(miniCol)

            miniLeaderboard.addView(TextView(host).apply {
                text = "View Ranks →"
                textSize = 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
            })
            content.addView(miniLeaderboard)
        }

        // Quick Rejoin Last Room Card (If available and user is not in an active room)
        val lastRoom = StudyRoomManager.getLastJoinedRoom(host)
        if (activeRoom == null && lastRoom != null) {
            val quickRejoinCard = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = themeCoordinator.createCardBackground()
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            val rejoinCol = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            rejoinCol.addView(TextView(host).apply {
                text = "RECENT STUDY ROOM"
                textSize = 10f
                letterSpacing = 0.12f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
            })
            rejoinCol.addView(TextView(host).apply {
                text = lastRoom.name
                textSize = 14.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(textColor)
            })
            rejoinCol.addView(TextView(host).apply {
                text = "#${lastRoom.code} • ${lastRoom.category}"
                textSize = 11.5f
                alpha = 0.7f
                setTextColor(textColor)
            })
            quickRejoinCard.addView(rejoinCol)

            val rejoinBtn = TextView(host).apply {
                text = "Rejoin"
                textSize = 12.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(primary)
                }
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    ensureLoggedIn("rejoin this study room") {
                        CoroutineScope(Dispatchers.Main).launch {
                            val joinRes = StudyRoomManager.joinRoom(host, lastRoom.code)
                            if (joinRes.isSuccess) {
                                AppToast.show(host, "Rejoined ${lastRoom.name}")
                                isBrowsingExplore = false
                                renderRoomsView(container)
                            } else {
                                AppToast.show(host, "Could not rejoin: ${joinRes.exceptionOrNull()?.message ?: "Error"}")
                            }
                        }
                    }
                }
            }
            quickRejoinCard.addView(rejoinBtn)
            content.addView(quickRejoinCard)
        }

        // Section: Category Filter Chips Row
        content.addView(TextView(host).apply {
            text = "EXPLORE OPEN ROOMS"
            textSize = 12f
            letterSpacing = 0.14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            setPadding(dp(4), dp(4), 0, dp(8))
        })

        val categoryScroll = android.widget.HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }
        val categoryChipsRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val roomsListContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun refreshCategoryChips() {
            categoryChipsRow.removeAllViews()
            for (cat in categories) {
                val isSelected = (selectedCategory == cat)
                val chip = TextView(host).apply {
                    text = cat
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else textColor)
                    background = if (isSelected) {
                        GradientDrawable().apply {
                            cornerRadius = dp(14).toFloat()
                            setColor(primary)
                        }
                    } else {
                        themeCoordinator.createGlassChip(host.tintedColor(textColor, 25), 14f)
                    }
                    setPadding(dp(14), dp(7), dp(14), dp(7))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = dp(8)
                    }
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        selectedCategory = cat
                        refreshCategoryChips()
                        loadRoomsList(roomsListContainer, activeRoom)
                    }
                }
                categoryChipsRow.addView(chip)
            }
        }
        categoryScroll.addView(categoryChipsRow)
        content.addView(categoryScroll)
        refreshCategoryChips()

        // Rooms List Container with Async Loading
        content.addView(roomsListContainer)
        loadRoomsList(roomsListContainer, activeRoom)

        scroll.addView(content)
        container.removeAllViews()
        container.addView(scroll)
    }

    private fun loadRoomsList(container: LinearLayout, activeRoom: StudyRoom?) {
        container.removeAllViews()
        val isDark = themeCoordinator.isDarkMode()
        val primary = themeCoordinator.primaryColor
        val textColor = themeCoordinator.textColor

        val progressBar = ProgressBar(host).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, dp(24), 0, dp(24))
            }
        }
        container.addView(progressBar)

        CoroutineScope(Dispatchers.Main).launch {
            val result = StudyRoomManager.fetchPublicRooms(host, category = selectedCategory)
            container.removeAllViews()

            val rooms = result.getOrElse { emptyList() }

            if (rooms.isEmpty()) {
                // Clean Minimalist Empty State (No emoji spam)
                val emptyCard = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(24), dp(32), dp(24), dp(32))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(8)
                    }
                }

                emptyCard.addView(TextView(host).apply {
                    text = "No Open Study Rooms"
                    textSize = 16f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(textColor)
                    gravity = Gravity.CENTER
                })

                emptyCard.addView(TextView(host).apply {
                    text = if (selectedCategory == "All") {
                        "There are currently no active study rooms. Create an open room for your study group or join friends with a code."
                    } else {
                        "No study rooms found under $selectedCategory. Create one or browse other categories."
                    }
                    textSize = 13f
                    setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
                    gravity = Gravity.CENTER
                    setLineSpacing(dp(2).toFloat(), 1.15f)
                    setPadding(dp(8), dp(6), dp(8), dp(16))
                })

                val emptyCreateBtn = TextView(host).apply {
                    text = "+ Create Study Room"
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(primary)
                    }
                    setPadding(dp(20), dp(10), dp(20), dp(10))
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        ensureLoggedIn("create a study room") {
                            showCreateRoomDialog {
                                isBrowsingExplore = false
                                rootContentHost?.let { renderRoomsView(it) }
                            }
                        }
                    }
                }
                emptyCard.addView(emptyCreateBtn)
                container.addView(emptyCard)
                return@launch
            }

            for (room in rooms) {
                val isCurrentActive = activeRoom?.code == room.code

                val roomCard = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = themeCoordinator.createCardBackground()
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }
                }

                val col = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val headerLine = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                headerLine.addView(TextView(host).apply {
                    text = room.name
                    textSize = 14.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(textColor)
                })
                headerLine.addView(TextView(host).apply {
                    text = room.category
                    textSize = 10.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
                    background = themeCoordinator.createGlassChip(host.tintedColor(textColor, 18), 6f)
                    setPadding(dp(6), dp(2), dp(6), dp(2))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = dp(8)
                    }
                })
                col.addView(headerLine)

                val metaRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(4), 0, 0)
                }
                metaRow.addView(TextView(host).apply {
                    text = "Room #${room.code}"
                    textSize = 12f
                    setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = dp(8)
                    }
                })
                if (room.memberCount > 0) {
                    val activeCountPill = TextView(host).apply {
                        text = "${room.memberCount} active"
                        textSize = 11f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        setTextColor(0xFF10B981.toInt())
                        background = themeCoordinator.createGlassChip(0x2210B981.toInt(), 6f)
                        setPadding(dp(6), dp(1), dp(6), dp(1))
                    }
                    metaRow.addView(activeCountPill)
                }
                col.addView(metaRow)
                roomCard.addView(col)

                val enterBtn = TextView(host).apply {
                    text = if (isCurrentActive) "Entered ✓" else "Enter"
                    textSize = 12.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isCurrentActive) primary else Color.WHITE)
                    background = if (isCurrentActive) {
                        themeCoordinator.createGlassChip(host.tintedColor(primary, 40), 12f)
                    } else {
                        GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(primary)
                        }
                    }
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                    setOnClickListener {
                        if (!isCurrentActive) {
                            ensureLoggedIn("enter this study room") {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val joinRes = StudyRoomManager.joinRoom(host, room.code)
                                    if (joinRes.isSuccess) {
                                        AppToast.show(host, "Entered ${room.name}")
                                        isBrowsingExplore = false
                                        rootContentHost?.let { renderRoomsView(it) }
                                    } else {
                                        AppToast.show(host, "Could not enter room: ${joinRes.exceptionOrNull()?.message ?: "Error"}")
                                    }
                                }
                            }
                        }
                    }
                }
                roomCard.addView(enterBtn)
                container.addView(roomCard)
            }
        }
    }

    private fun ensureLoggedIn(actionName: String, onProceed: () -> Unit) {
        if (AuthManager.isLoggedIn(host) && !AuthManager.isGuest(host)) {
            onProceed()
        } else {
            showSignInDialog(actionName)
        }
    }

    private fun showSignInDialog(actionName: String) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val isDark = themeCoordinator.isDarkMode()
        val primary = themeCoordinator.primaryColor
        val textColor = themeCoordinator.textColor

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createCardBackground(20f)
            setPadding(dp(22), dp(24), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "Google Account Required"
            textSize = 17f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        })

        content.addView(TextView(host).apply {
            text = "Study Rooms are synchronized live for registered students. Sign in with Google to $actionName, co-study with friends, and appear on leaderboards."
            textSize = 13f
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            setLineSpacing(dp(2).toFloat(), 1.15f)
            setPadding(0, dp(8), 0, dp(18))
        })

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelBtn = TextView(host).apply {
            text = "Cancel"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            alpha = 0.7f
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { dialog.dismiss() }
        }
        btnRow.addView(cancelBtn)

        val signInBtn = TextView(host).apply {
            text = "Sign in with Google"
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(primary)
            }
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener {
                dialog.dismiss()
                val intent = Intent(host, LoginActivity::class.java).apply {
                    putExtra("auto_google", true)
                }
                host.startActivity(intent)
            }
        }
        btnRow.addView(signInBtn)
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun showJoinRoomDialog(onJoined: () -> Unit) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val primary = themeCoordinator.primaryColor

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "JOIN STUDY ROOM"
            textSize = 11.5f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            setPadding(0, 0, 0, dp(6))
        })

        content.addView(TextView(host).apply {
            text = "Enter the 6-character room code shared by your study partner or group."
            textSize = 13f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.8f
            setPadding(0, 0, 0, dp(14))
        })

        val codeInput = EditText(host).apply {
            hint = "e.g. SILENT or K8M2P4"
            setHintTextColor(if (isDark) 0xFF64748B.toInt() else 0xFF94A3B8.toInt())
            setTextColor(themeCoordinator.textColor)
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            filters = arrayOf(android.text.InputFilter.LengthFilter(10), android.text.InputFilter.AllCaps())
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0xFF1E293B.toInt() else 0xFFF1F5F9.toInt())
                setStroke(dp(1), primary)
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        content.addView(codeInput)

        val passInput = EditText(host).apply {
            hint = "Passcode (optional if private)"
            setHintTextColor(if (isDark) 0xFF64748B.toInt() else 0xFF94A3B8.toInt())
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0xFF1E293B.toInt() else 0xFFF1F5F9.toInt())
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }
        content.addView(passInput)

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(16), 0, 0)
        }

        val cancelBtn = TextView(host).apply {
            text = "Cancel"
            textSize = 14f
            setTextColor(themeCoordinator.textColor)
            alpha = 0.7f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { dialog.dismiss() }
        }

        val joinBtn = TextView(host).apply {
            text = "Join Room"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(primary)
            }
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setOnClickListener {
                val input = codeInput.text.toString().trim().uppercase()
                val pass = passInput.text.toString().trim().ifEmpty { null }
                if (input.length < 3) {
                    AppToast.show(host, "Please enter a valid room code")
                    return@setOnClickListener
                }

                it.isEnabled = false
                it.alpha = 0.5f

                CoroutineScope(Dispatchers.Main).launch {
                    val result = StudyRoomManager.joinRoom(host, input, pass)
                    if (result.isSuccess) {
                        val room = result.getOrNull()
                        AppToast.show(host, "Joined ${room?.name ?: input}")
                        dialog.dismiss()
                        onJoined()
                    } else {
                        it.isEnabled = true
                        it.alpha = 1.0f
                        val errMsg = result.exceptionOrNull()?.message ?: "Failed to join room"
                        AppToast.show(host, errMsg)
                    }
                }
            }
        }

        btnRow.addView(cancelBtn)
        btnRow.addView(joinBtn)
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun showCreateRoomDialog(onCreated: () -> Unit) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isDark = themeCoordinator.isDarkMode()
        val primary = themeCoordinator.primaryColor
        val textColor = themeCoordinator.textColor

        val content = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(22), dp(20), dp(22), dp(20))
        }

        content.addView(TextView(host).apply {
            text = "CREATE STUDY ROOM"
            textSize = 11.5f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            setPadding(0, 0, 0, dp(10))
        })

        val nameInput = EditText(host).apply {
            hint = "e.g. Physics Finals Prep"
            setHintTextColor(if (isDark) 0xFF64748B.toInt() else 0xFF94A3B8.toInt())
            setTextColor(textColor)
            textSize = 15f
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0xFF1E293B.toInt() else 0xFFF1F5F9.toInt())
                setStroke(dp(1), primary)
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        content.addView(nameInput)

        // Category Picker Label
        content.addView(TextView(host).apply {
            text = "Select Category"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, dp(12), 0, dp(6))
        })

        var selectedCat = "General"
        val catPickerScroll = android.widget.HorizontalScrollView(host).apply {
            isHorizontalScrollBarEnabled = false
        }
        val catChipsRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun updateDialogChips() {
            catChipsRow.removeAllViews()
            for (cat in categories.filterNot { it == "All" }) {
                val isSelected = (selectedCat == cat)
                val chip = TextView(host).apply {
                    text = cat
                    textSize = 11.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isSelected) Color.WHITE else textColor)
                    background = if (isSelected) {
                        GradientDrawable().apply {
                            cornerRadius = dp(12).toFloat()
                            setColor(primary)
                        }
                    } else {
                        themeCoordinator.createGlassChip(host.tintedColor(textColor, 25), 12f)
                    }
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = dp(6)
                    }
                    setOnClickListener {
                        selectedCat = cat
                        updateDialogChips()
                    }
                }
                catChipsRow.addView(chip)
            }
        }
        updateDialogChips()
        catPickerScroll.addView(catChipsRow)
        content.addView(catPickerScroll)

        // Room Privacy / Visibility Selector
        content.addView(TextView(host).apply {
            text = "Room Visibility"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            setPadding(0, dp(12), 0, dp(6))
        })

        var isPrivateRoom = false

        val privacySegmentRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeCoordinator.createCardBackground()
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }

        val publicOpt = TextView(host).apply {
            text = "Public Room"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val privateOpt = TextView(host).apply {
            text = "Private Room"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        privacySegmentRow.addView(publicOpt)
        privacySegmentRow.addView(privateOpt)
        content.addView(privacySegmentRow)

        val privacyDesc = TextView(host).apply {
            text = "Open to everyone in Explore. Anyone can join freely."
            textSize = 11.5f
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            setPadding(dp(2), dp(6), dp(2), dp(4))
        }
        content.addView(privacyDesc)

        val passcodeContainer = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(4), 0, 0)
        }

        val passcodeInput = EditText(host).apply {
            hint = "Optional Passcode (e.g. 1234)"
            setHintTextColor(if (isDark) 0xFF64748B.toInt() else 0xFF94A3B8.toInt())
            setTextColor(textColor)
            textSize = 14f
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (isDark) 0xFF1E293B.toInt() else 0xFFF1F5F9.toInt())
                setStroke(dp(1), host.tintedColor(primary, 40))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        passcodeContainer.addView(passcodeInput)

        val passcodeHelp = TextView(host).apply {
            text = "Leave empty if anyone with the 6-character room code can enter. Your study time still counts toward the leaderboard!"
            textSize = 11f
            setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
            setPadding(dp(2), dp(4), dp(2), 0)
        }
        passcodeContainer.addView(passcodeHelp)
        content.addView(passcodeContainer)

        fun updatePrivacySegment() {
            if (!isPrivateRoom) {
                publicOpt.setTextColor(Color.WHITE)
                publicOpt.background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(primary)
                }
                privateOpt.setTextColor(textColor)
                privateOpt.background = null
                privacyDesc.text = "Open to everyone in Explore. Anyone can join and study with you."
                passcodeContainer.visibility = View.GONE
            } else {
                publicOpt.setTextColor(textColor)
                publicOpt.background = null
                privateOpt.setTextColor(Color.WHITE)
                privateOpt.background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(if (isDark) 0xFF7C3AED.toInt() else 0xFF6D28D9.toInt())
                }
                privacyDesc.text = "Hidden from Explore. Only friends with your room code can join. Your study time counts on the leaderboard, but the room stays private."
                passcodeContainer.visibility = View.VISIBLE
            }
        }
        updatePrivacySegment()

        publicOpt.setOnClickListener {
            isPrivateRoom = false
            updatePrivacySegment()
        }
        privateOpt.setOnClickListener {
            isPrivateRoom = true
            updatePrivacySegment()
        }

        val btnRow = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(18), 0, 0)
        }

        val cancelBtn = TextView(host).apply {
            text = "Cancel"
            textSize = 14f
            setTextColor(textColor)
            alpha = 0.7f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setOnClickListener { dialog.dismiss() }
        }

        val createBtn = TextView(host).apply {
            text = "Create Room"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(primary)
            }
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    AppToast.show(host, "Please enter a room name")
                    return@setOnClickListener
                }

                it.isEnabled = false
                it.alpha = 0.5f

                val passcode = if (isPrivateRoom) passcodeInput.text.toString().trim().ifEmpty { null } else null

                CoroutineScope(Dispatchers.Main).launch {
                    val result = StudyRoomManager.createRoom(
                        context = host,
                        name = name,
                        category = selectedCat,
                        isPrivate = isPrivateRoom,
                        passCode = passcode
                    )
                    if (result.isSuccess) {
                        val newRoom = result.getOrNull()
                        AppToast.show(host, "Created & joined ${newRoom?.name}!")
                        dialog.dismiss()
                        onCreated()
                    } else {
                        it.isEnabled = true
                        val errMsg = result.exceptionOrNull()?.message ?: "Failed to create room"
                        AppToast.show(host, errMsg)
                    }
                }
            }
        }

        btnRow.addView(cancelBtn)
        btnRow.addView(createBtn)
        content.addView(btnRow)

        dialog.setContentView(content)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun showMemberProfileDialog(m: StudyRoomMember) {
        val dialog = Dialog(host)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val isSelf = m.isSelf
        val isDark = themeCoordinator.isDarkMode()
        val textColor = themeCoordinator.textColor
        val primary = themeCoordinator.primaryColor

        val dialogRoot = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(26f)
            setPadding(dp(22), dp(22), dp(22), dp(20))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 1. Centered Avatar Frame (72dp)
        val avatarBox = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }

        if (isSelf && LocalAvatarManager.hasCustomAvatar(host)) {
            val customBitmap = LocalAvatarManager.getCircularAvatarBitmap(host, dp(72))
            if (customBitmap != null) {
                val iv = ImageView(host).apply {
                    setImageBitmap(customBitmap)
                    layoutParams = FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(dp(1), primary)
                    }
                }
                avatarBox.addView(iv)
            } else {
                avatarBox.addView(createDefaultAvatar(m, dp(72), primary))
            }
        } else if (isSelf) {
            val remoteUrl = ProfileManager.getEffectiveAvatarUrl(host)
            if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                val container = FrameLayout(host).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER)
                }
                val base = createDefaultAvatar(m, dp(72), primary)
                container.addView(base)
                val img = ImageView(host).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(1), primary)
                    }
                    visibility = View.GONE
                }
                container.addView(img)
                CoroutineScope(Dispatchers.IO).launch {
                    val bmp = LocalAvatarManager.getCircularBitmapFromUrl(host, remoteUrl, dp(72))
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            img.setImageBitmap(bmp)
                            img.visibility = View.VISIBLE
                            base.visibility = View.GONE
                        }
                    }
                }
                avatarBox.addView(container)
            } else {
                avatarBox.addView(createDefaultAvatar(m, dp(72), primary))
            }
        } else {
            avatarBox.addView(createDefaultAvatar(m, dp(72), primary))
        }
        dialogRoot.addView(avatarBox)

        // 2. Student Display Name (High contrast, neutral textColor - NO green)
        val cleanName = m.displayName.replace("(You)", "").trim()
        val displayName = if (isSelf) "$cleanName (You)" else cleanName
        val nameView = TextView(host).apply {
            text = displayName
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        dialogRoot.addView(nameView)

        // 3. Target Exam / Focus Track Badge
        val initialExam = if (isSelf) {
            ProfileManager.getProfile(host).targetExam.ifBlank { "Self-Study" }
        } else {
            "Self-Study"
        }
        val trackPill = TextView(host).apply {
            text = "🎯 $initialExam"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(primary)
            background = themeCoordinator.createGlassChip(host.tintedColor(primary, 35), 12f)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        dialogRoot.addView(trackPill)

        // 4. Student Bio / Motto (if present)
        val initialBio = if (isSelf) ProfileManager.getProfile(host).bio else ""
        val mottoView = TextView(host).apply {
            text = if (initialBio.isNotBlank()) "\"$initialBio\"" else ""
            textSize = 12.5f
            alpha = 0.8f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
            visibility = if (initialBio.isNotBlank()) View.VISIBLE else View.GONE
        }
        dialogRoot.addView(mottoView)

        // If peer student, fetch real profile from Supabase asynchronously
        if (!isSelf && m.id.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val pubProfile = LeaderboardManager.fetchUserProfile(host, m.id)
                if (pubProfile != null) {
                    withContext(Dispatchers.Main) {
                        if (pubProfile.targetExam.isNotBlank()) {
                            trackPill.text = "🎯 ${pubProfile.targetExam}"
                        }
                        if (pubProfile.bio.isNotBlank()) {
                            mottoView.text = "\"${pubProfile.bio}\""
                            mottoView.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        // 5. Bento Stats Summary Grid: [ TODAY | SUBJECT | STATUS | STREAK ]
        val statsCard = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            background = themeCoordinator.createGlassChip(host.tintedColor(textColor, 20), 14f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(16))
            }
        }

        // Col 1: TODAY
        val colTime = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
        }
        colTime.addView(TextView(host).apply {
            text = "TODAY"
            textSize = 9.5f
            alpha = 0.6f
            setTextColor(textColor)
        })
        val durationH = m.elapsedMinutes / 60
        val durationM = m.elapsedMinutes % 60
        val durStr = if (durationH > 0) "${durationH}h ${durationM}m" else "${durationM}m"
        colTime.addView(TextView(host).apply {
            text = durStr
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        })
        statsCard.addView(colTime)

        // Col 2: SUBJECT
        val colSub = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
        }
        colSub.addView(TextView(host).apply {
            text = "SUBJECT"
            textSize = 9.5f
            alpha = 0.6f
            setTextColor(textColor)
        })
        val rawSub = m.currentSubject?.takeIf { it.isNotBlank() } ?: "Focusing"
        val displaySub = if (rawSub.length > 9) rawSub.take(8) + "…" else rawSub
        colSub.addView(TextView(host).apply {
            text = displaySub
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(textColor)
        })
        statsCard.addView(colSub)

        // Col 3: STATUS (Clean text, no emojis)
        val colStatus = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
        }
        colStatus.addView(TextView(host).apply {
            text = "STATUS"
            textSize = 9.5f
            alpha = 0.6f
            setTextColor(textColor)
        })
        colStatus.addView(TextView(host).apply {
            text = if (m.isStudying) "Studying" else "Resting"
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (m.isStudying) primary else (if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()))
        })
        statsCard.addView(colStatus)

        // Col 4: STREAK
        val effectiveStreak = if (isSelf) {
            host.getSharedPreferences("StudyTimerPrefs", Context.MODE_PRIVATE).getInt("current_streak", 0)
        } else {
            0
        }
        val colStreak = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
        }
        colStreak.addView(TextView(host).apply {
            text = "STREAK"
            textSize = 9.5f
            alpha = 0.6f
            setTextColor(textColor)
        })
        val streakText = TextView(host).apply {
            text = if (effectiveStreak > 0) "🔥 ${effectiveStreak}d" else "--"
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (effectiveStreak > 0) Color.parseColor("#F59E0B") else textColor)
        }
        colStreak.addView(streakText)
        statsCard.addView(colStreak)

        dialogRoot.addView(statsCard)

        // 6. Action Buttons
        if (isSelf) {
            val editProfileBtn = Button(host).apply {
                text = "Edit Profile"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = themeCoordinator.createButtonBackground(primary)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)).apply {
                    setMargins(0, 0, 0, dp(10))
                }
                setOnClickListener {
                    dialog.dismiss()
                    host.currentSettingsTab = AppSettingsTab.PROFILE
                    host.navigateToPanel(AppPanel.SETTINGS)
                }
            }
            dialogRoot.addView(editProfileBtn)
        }

        if (!isSelf) {
            val cheerLabel = TextView(host).apply {
                text = "Cheer On Study Partner"
                textSize = 11.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(primary)
                setPadding(0, 0, 0, dp(8))
            }
            dialogRoot.addView(cheerLabel)

            val cheerRow = LinearLayout(host).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(14)
                }
            }

            fun addCheerBtn(label: String, cheerText: String) {
                val btn = TextView(host).apply {
                    text = label
                    textSize = 12f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(textColor)
                    background = themeCoordinator.createGlassChip(host.tintedColor(primary, 30), 10f)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(3), 0, dp(3), 0)
                    }
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        dialog.dismiss()
                        AppToast.show(host, "$cheerText sent to ${m.displayName}! 🎉")
                    }
                }
                cheerRow.addView(btn)
            }

            addCheerBtn("👏 High Five", "High five")
            addCheerBtn("🔥 Keep Going", "Motivation")
            addCheerBtn("☕ Coffee", "Coffee break cheer")
            dialogRoot.addView(cheerRow)
        }

        val closeBtn = Button(host).apply {
            text = "Close"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = themeCoordinator.createButtonBackground(if (isSelf) (if (isDark) 0xFF334155.toInt() else 0xFF475569.toInt()) else primary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
            setOnClickListener { dialog.dismiss() }
        }
        dialogRoot.addView(closeBtn)

        dialog.setContentView(dialogRoot)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            setLayout((host.resources.displayMetrics.widthPixels * 0.88f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun createDefaultAvatar(m: StudyRoomMember, sizePx: Int, accentColor: Int): View {
        val initial = m.avatarEmoji.ifBlank { m.displayName.take(1).uppercase().ifEmpty { "S" } }
        return TextView(host).apply {
            text = initial
            textSize = (sizePx / density * 0.40f).coerceAtLeast(11f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(LocalAvatarManager.getGoogleAvatarColor(m.displayName))
            }
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER)
        }
    }
    private fun refreshMembersList(
        room: StudyRoom,
        membersContainer: LinearLayout,
        membersLabel: TextView,
        activeCountBadge: TextView,
        forceRefresh: Boolean = false
    ) {
        val isDark = themeCoordinator.isDarkMode()
        val textColor = themeCoordinator.textColor

        CoroutineScope(Dispatchers.Main).launch {
            val result = StudyRoomManager.fetchRoomMembers(host, room, forceRefresh = forceRefresh)
            val rawMembers = result.getOrElse { emptyList() }
            val members = rawMembers.sortedWith(
                compareByDescending<StudyRoomMember> { it.isSelf }
                    .thenByDescending { it.isStudying }
            )
            val studyingCount = members.count { it.isStudying }
            membersLabel.text = "Study Partners (${members.size})"
            if (studyingCount > 0) {
                activeCountBadge.text = "$studyingCount Studying"
                activeCountBadge.visibility = View.VISIBLE
            } else {
                activeCountBadge.visibility = View.GONE
            }

            membersContainer.removeAllViews()

            for (m in members) {
                val memberRow = LinearLayout(host).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    val outVal = android.util.TypedValue()
                    host.theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
                    setBackgroundResource(outVal.resourceId)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        showMemberProfileDialog(m)
                    }
                }

                val avatar = TextView(host).apply {
                    text = m.avatarEmoji
                    textSize = 14f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(if (isDark) Color.WHITE else 0xFF1E293B.toInt())
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (isDark) 0xFF334155.toInt() else 0xFFE2E8F0.toInt())
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                        rightMargin = dp(10)
                    }
                }
                memberRow.addView(avatar)

                val infoCol = LinearLayout(host).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                infoCol.addView(TextView(host).apply {
                    val cleanName = m.displayName.replace("(You)", "").trim()
                    text = if (m.isSelf) "$cleanName (You)" else cleanName
                    textSize = 13.5f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setTextColor(textColor)
                })
                infoCol.addView(TextView(host).apply {
                    val durationH = m.elapsedMinutes / 60
                    val durationM = m.elapsedMinutes % 60
                    val durStr = if (durationH > 0) "${durationH}h ${durationM}m" else "${durationM}m"
                    val sub = m.currentSubject ?: "Focusing"
                    text = "$durStr today • $sub"
                    textSize = 11.5f
                    setTextColor(if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt())
                })
                memberRow.addView(infoCol)

                val statusDot = TextView(host).apply {
                    text = if (m.isStudying) "Studying" else "Resting"
                    textSize = 11f
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setTextColor(if (m.isStudying) 0xFF10B981.toInt() else (if (isDark) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()))
                    background = themeCoordinator.createGlassChip(
                        if (m.isStudying) 0x2210B981.toInt() else host.tintedColor(textColor, 14),
                        8f
                    )
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                }
                memberRow.addView(statusDot)
                membersContainer.addView(memberRow)
            }
        }
    }
}
