package com.madeby.JAI

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Community Hub displaying the Live Global & Friends Leaderboard.
 */
class CommunityPanelBuilder(private val host: MainActivity) {

    companion object {
        fun cancelActivePolling() {
            // Study room polling removed
        }
    }

    enum class CommunitySegment {
        ROOMS,
        LEADERBOARD
    }

    private val density = host.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    fun switchSegment(segment: CommunitySegment) {
        // Study rooms deprecated; Community displays Leaderboard directly
    }

    fun build(target: ViewGroup = host.panelContainer, initialSegment: CommunitySegment = CommunitySegment.LEADERBOARD) {
        host.recalculateStreak()

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

        // 2. Main Content: Leaderboard
        val leaderboardContainer = FrameLayout(host).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(0, 0, 0, dp(80))
        }
        LeaderboardPanelBuilder(host).build(leaderboardContainer, showBackButton = false)
        root.addView(leaderboardContainer)

        FontStyleManager.applyRecursively(root)
        target.removeAllViews()
        target.addView(root)
    }
}
