package com.madeby.JAI

import java.util.UUID

enum class AppPanel { FOCUS, STATS, SETTINGS, HEATMAP, LEADERBOARD }
enum class TimerState { IDLE, STUDYING, BREAK, PAUSED, LECTURE_ENDED }
enum class AppStatsTab { OVERVIEW, TIMELINE, PLANNER }
enum class LeaderboardPeriod { DAILY, WEEKLY, MONTHLY }

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val avatarUrl: String,
    val totalSeconds: Int,
    val isStudying: Boolean,
    val currentSubject: String = "",
    val subjectColor: String = "#3b82f6",
    val lastActiveAt: String = "",
    val examTarget: String = "",
    val bio: String = "",
    val streakDays: Int = 0
)
enum class AppSettingsTab { 
    HUB, 
    TIMER, 
    AMBIENCE, 
    ANALYTICS, 
    CLOUD, 
    THEME, 
    PROFILE, 
    DEVELOPER,
    SIMPLE // Settings tab page & navigation target
}

data class PlannerGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val targetMinutes: Int = 0,
    val completed: Boolean = false,
    val checkedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val subjectId: String? = null
)

/**
 * Typealias representing an individual goal in active sessions and stats tracking.
 * Intentionally aliases [PlannerGoal] so goal instances can seamlessly bridge planner,
 * timeline, and session tracking pipelines without manual object conversion.
 */
typealias SessionGoal = PlannerGoal

data class PlannerGoalSnapshot(
    val goalId: String,
    val title: String,
    val targetMinutes: Int,
    val completed: Boolean,
    val checkedAt: Long = 0L,
    val isAchieved: Boolean = true,
    val subjectId: String? = null,
    val note: String = ""
)

data class PlannerDayRecord(
    val date: String,
    val goalSnapshots: List<PlannerGoalSnapshot>
)

data class LectureScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: String, // "HH:mm" e.g. "10:00"
    val endTime: String,   // "HH:mm" e.g. "11:00"
    val enabled: Boolean = true,
    val subjectId: String = "general"
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)
