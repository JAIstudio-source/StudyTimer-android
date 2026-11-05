package com.madeby.JAI

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Supernova Particle Bloom Celebration Engine.
 * 
 * 100% original programmatic canvas animations with zero external raster images.
 * Generates dynamic 4-pointed radiant stars, geometric diamonds, expanding rings,
 * and luminous sparks with radial explosion physics, accompanied by celebratory audio
 * and tactile haptics.
 */
object CelebrationEngine {

    private const val PREFS_NAME = "studytimer_celebration_prefs"
    private const val KEY_LAST_CELEBRATED_STREAK = "last_celebrated_streak"
    private const val KEY_LAST_CELEBRATED_GOAL_DATE = "last_celebrated_goal_date"
    private const val KEY_LAST_CELEBRATED_STREAK_DATE = "last_celebrated_streak_date"

    enum class BloomShape {
        STAR_4,
        DIAMOND,
        SPARK_CIRCLE,
        HALO_RING,
        LIGHT_STREAK
    }

    private class SupernovaParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var rotation: Float,
        var rotSpeed: Float,
        var size: Float,
        val color: Int,
        val shape: BloomShape,
        var alpha: Float = 1f,
        var scale: Float = 1f
    )

    fun checkAndCelebrate(
        activity: MainActivity,
        todayTotalSecs: Long,
        dailyGoalSecs: Long,
        streakTargetSecs: Long,
        currentStreak: Int
    ) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        val goalReachedToday = todayTotalSecs >= dailyGoalSecs && dailyGoalSecs > 0
        val lastGoalDate = prefs.getString(KEY_LAST_CELEBRATED_GOAL_DATE, null)
        val isNewGoalAchieved = goalReachedToday && lastGoalDate != todayStr

        val streakReachedToday = todayTotalSecs >= streakTargetSecs && streakTargetSecs > 0
        val lastStreakDate = prefs.getString(KEY_LAST_CELEBRATED_STREAK_DATE, null)
        val lastStreak = prefs.safeInt(KEY_LAST_CELEBRATED_STREAK, 0)
        val isStreakIncreased = streakReachedToday && currentStreak > 0 && lastStreakDate != todayStr
        val isNewStreakMilestone = isStreakIncreased && (currentStreak in listOf(7, 14, 21, 28, 50, 75, 100) || currentStreak % 50 == 0)

        if (isNewGoalAchieved || isStreakIncreased) {
            if (isNewGoalAchieved) {
                prefs.edit().putString(KEY_LAST_CELEBRATED_GOAL_DATE, todayStr).apply()
            }
            if (isStreakIncreased) {
                prefs.edit()
                    .putString(KEY_LAST_CELEBRATED_STREAK_DATE, todayStr)
                    .putInt(KEY_LAST_CELEBRATED_STREAK, currentStreak)
                    .apply()
            }

            val oldStreak = if (lastStreak > 0 && lastStreak < currentStreak) lastStreak else (currentStreak - 1).coerceAtLeast(0)

            activity.runOnUiThread {
                if (isNewGoalAchieved && isStreakIncreased) {
                    val onStreakDismiss: () -> Unit = {
                        showCelebrationDialog(activity, isGoalAchieved = true, streak = currentStreak)
                    }
                    if (isNewStreakMilestone) {
                        showCelebrationDialog(activity, isGoalAchieved = false, streak = currentStreak, onDismiss = onStreakDismiss)
                    } else {
                        StreakUpAnimationDialog.show(activity, oldStreak = oldStreak, newStreak = currentStreak, onDismiss = onStreakDismiss)
                    }
                } else if (isNewStreakMilestone) {
                    showCelebrationDialog(activity, isGoalAchieved = false, streak = currentStreak)
                } else if (isStreakIncreased) {
                    StreakUpAnimationDialog.show(activity, oldStreak = oldStreak, newStreak = currentStreak)
                } else if (isNewGoalAchieved) {
                    showCelebrationDialog(activity, isGoalAchieved = true, streak = currentStreak)
                }
            }
        }
    }

    private data class StreakMilestoneInfo(
        val icon: String,
        val title: String,
        val subtitle: String,
        val accentColor: Int,
        val particles: IntArray
    )

    private fun getStreakMilestoneInfo(streak: Int, primaryColor: Int): StreakMilestoneInfo {
        return when (streak) {
            7 -> StreakMilestoneInfo(
                icon = "🥉",
                title = "7-Day Bronze Streak!",
                subtitle = "1 Week Strong! Fantastic work maintaining consistency for a full week.",
                accentColor = Color.parseColor("#CD7F32"),
                particles = intArrayOf(0xFFCD7F32.toInt(), 0xFFFFA726.toInt(), 0xFFFFD700.toInt(), 0xFFFFFFFF.toInt())
            )
            14 -> StreakMilestoneInfo(
                icon = "🥈",
                title = "14-Day Silver Streak!",
                subtitle = "2 Weeks Unstoppable! Two straight weeks of focus and dedication.",
                accentColor = Color.parseColor("#C0C0C0"),
                particles = intArrayOf(0xFFE0E0E0.toInt(), 0xFF38BDF8.toInt(), 0xFF818CF8.toInt(), 0xFFFFFFFF.toInt())
            )
            21 -> StreakMilestoneInfo(
                icon = "🥇",
                title = "21-Day Gold Streak!",
                subtitle = "Habit Formed! 21 consecutive days of learning. Your streak is legendary!",
                accentColor = Color.parseColor("#FFD700"),
                particles = intArrayOf(0xFFFFD700.toInt(), 0xFFF59E0B.toInt(), 0xFFFF4081.toInt(), 0xFFFFF275.toInt())
            )
            28 -> StreakMilestoneInfo(
                icon = "💎",
                title = "28-Day Diamond Streak!",
                subtitle = "4 Weeks of Focus! A full month of unmatched study discipline.",
                accentColor = Color.parseColor("#00E5FF"),
                particles = intArrayOf(0xFF38BDF8.toInt(), 0xFF818CF8.toInt(), 0xFFA855F7.toInt(), 0xFFE0F2FE.toInt())
            )
            50 -> StreakMilestoneInfo(
                icon = "🔥",
                title = "50-Day Fire Streak!",
                subtitle = "Half Century Milestone! 50 straight days of unstoppable study momentum.",
                accentColor = Color.parseColor("#EF4444"),
                particles = intArrayOf(0xFFEF4444.toInt(), 0xFFF59E0B.toInt(), 0xFFFF4081.toInt(), 0xFFFFD700.toInt())
            )
            100 -> StreakMilestoneInfo(
                icon = "👑",
                title = "100-Day Crown Streak!",
                subtitle = "Century Legend! 100 days of elite, master-level dedication!",
                accentColor = Color.parseColor("#A855F7"),
                particles = intArrayOf(0xFFA855F7.toInt(), 0xFFFFD700.toInt(), 0xFFEC4899.toInt(), 0xFFFFFFFF.toInt())
            )
            else -> StreakMilestoneInfo(
                icon = "🏆",
                title = "$streak-Day Study Streak!",
                subtitle = "Incredible consistency! You are on a $streak-day study streak. Keep it up!",
                accentColor = primaryColor,
                particles = intArrayOf(primaryColor, 0xFFFFD700.toInt(), 0xFF10B981.toInt(), 0xFF38BDF8.toInt())
            )
        }
    }

    /**
     * Builds and displays the milestone celebration dialog with radial Supernova particle explosion.
     */
    internal fun showCelebrationDialog(
        activity: MainActivity,
        isGoalAchieved: Boolean,
        streak: Int,
        onDismiss: (() -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val primaryColor = activity.themeCoordinator.primaryColor
            val density = activity.resources.displayMetrics.density
            val dp = { v: Int -> (v * density).toInt() }
            val targetWidth = (activity.resources.displayMetrics.widthPixels * 0.86f).toInt().coerceAtMost(dp(340))

            val milestoneInfo = getStreakMilestoneInfo(streak, primaryColor)
            val strokeColor = if (isGoalAchieved) primaryColor else milestoneInfo.accentColor

            // Multi-sensory rewards: Sound + Haptics
            StudyAudioManager.playCelebrationSound(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }

            val cardBg = GradientDrawable().apply {
                val isDark = activity.themeCoordinator.isDarkMode()
                val bgCol = if (isDark) Color.parseColor("#0F1420") else Color.parseColor("#FFFFFF")
                setColor(bgCol)
                cornerRadius = 28f * density
                setStroke((2f * density).toInt(), (strokeColor and 0x00FFFFFF) or 0x66000000)
            }

            val container = object : LinearLayout(activity) {
                private val particles = ArrayList<SupernovaParticle>()
                private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                private val starPath = Path()
                private var animator: ValueAnimator? = null
                private val colors = if (isGoalAchieved) {
                    intArrayOf(
                        primaryColor, 0xFFFFD700.toInt(), 0xFF38BDF8.toInt(),
                        0xFF10B981.toInt(), 0xFFFF4081.toInt(), 0xFFFFFFFF.toInt()
                    )
                } else {
                    milestoneInfo.particles
                }

                init {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    background = cardBg
                    clipToOutline = true
                    setPadding(dp(22), dp(22), dp(22), dp(20))

                    val shapes = BloomShape.values()
                    for (i in 0 until 55) {
                        val shape = shapes[i % shapes.size]
                        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                        val speed = (Random.nextFloat() * 12f + 4f) * density
                        val size = when (shape) {
                            BloomShape.STAR_4 -> (Random.nextFloat() * 7f + 6f) * density
                            BloomShape.DIAMOND -> (Random.nextFloat() * 6f + 4f) * density
                            BloomShape.HALO_RING -> (Random.nextFloat() * 8f + 5f) * density
                            BloomShape.LIGHT_STREAK -> (Random.nextFloat() * 9f + 5f) * density
                            BloomShape.SPARK_CIRCLE -> (Random.nextFloat() * 4f + 2.5f) * density
                        }

                        particles.add(
                            SupernovaParticle(
                                x = 0f,
                                y = 0f,
                                vx = cos(angle) * speed,
                                vy = sin(angle) * speed - (3f * density), // upward bias
                                rotation = Random.nextFloat() * 360f,
                                rotSpeed = (Random.nextFloat() - 0.5f) * 16f,
                                size = size,
                                color = colors[Random.nextInt(colors.size)],
                                shape = shape
                            )
                        )
                    }
                }

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    if (w <= 0 || h <= 0) return
                    val cx = w / 2f
                    val cy = h * 0.28f
                    for (p in particles) {
                        p.x = cx + (Random.nextFloat() - 0.5f) * 20f
                        p.y = cy + (Random.nextFloat() - 0.5f) * 20f
                    }

                    animator?.cancel()
                    animator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 2400L
                        interpolator = DecelerateInterpolator(1.2f)
                        addUpdateListener { va ->
                            val fraction = va.animatedFraction
                            for (p in particles) {
                                p.x += p.vx
                                p.y += p.vy
                                p.vy += 0.22f * density // gentle gravity
                                p.vx *= 0.965f // air drag
                                p.rotation += p.rotSpeed
                                p.alpha = (1f - fraction * 0.98f).coerceIn(0f, 1f)
                                p.scale = (1f - fraction * 0.35f).coerceIn(0.2f, 1f)
                            }
                            invalidate()
                        }
                        start()
                    }
                }

                override fun dispatchDraw(canvas: Canvas) {
                    super.dispatchDraw(canvas)
                    for (p in particles) {
                        if (p.alpha <= 0.01f) continue
                        val alphaInt = (p.alpha * 255).toInt().coerceIn(0, 255)

                        canvas.save()
                        canvas.translate(p.x, p.y)
                        canvas.rotate(p.rotation)
                        canvas.scale(p.scale, p.scale)

                        when (p.shape) {
                            BloomShape.STAR_4 -> {
                                fillPaint.color = p.color
                                fillPaint.alpha = alphaInt
                                starPath.reset()
                                starPath.moveTo(0f, -p.size)
                                starPath.quadTo(0f, 0f, p.size, 0f)
                                starPath.quadTo(0f, 0f, 0f, p.size)
                                starPath.quadTo(0f, 0f, -p.size, 0f)
                                starPath.quadTo(0f, 0f, 0f, -p.size)
                                starPath.close()
                                canvas.drawPath(starPath, fillPaint)
                            }
                            BloomShape.DIAMOND -> {
                                fillPaint.color = p.color
                                fillPaint.alpha = alphaInt
                                val half = p.size / 2f
                                canvas.drawRect(-half, -half, half, half, fillPaint)
                            }
                            BloomShape.SPARK_CIRCLE -> {
                                fillPaint.color = p.color
                                fillPaint.alpha = alphaInt
                                canvas.drawCircle(0f, 0f, p.size / 2f, fillPaint)
                            }
                            BloomShape.HALO_RING -> {
                                strokePaint.color = p.color
                                strokePaint.alpha = (alphaInt * 0.85f).toInt()
                                strokePaint.strokeWidth = 1.6f * density
                                canvas.drawCircle(0f, 0f, p.size / 2f, strokePaint)
                            }
                            BloomShape.LIGHT_STREAK -> {
                                strokePaint.color = p.color
                                strokePaint.alpha = alphaInt
                                strokePaint.strokeWidth = 1.5f * density
                                canvas.drawLine(0f, -p.size / 2f, 0f, p.size / 2f, strokePaint)
                            }
                        }
                        canvas.restore()
                    }
                }

                override fun onDetachedFromWindow() {
                    super.onDetachedFromWindow()
                    animator?.cancel()
                }
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    targetWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Milestone Badge / Icon
            val iconTv = TextView(activity).apply {
                text = if (isGoalAchieved) "🎉" else milestoneInfo.icon
                textSize = 44f
                gravity = Gravity.CENTER
            }

            val titleTv = TextView(activity).apply {
                text = if (isGoalAchieved) "Daily Goal Achieved!" else milestoneInfo.title
                textSize = 19f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(activity.themeCoordinator.textColor)
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(6))
            }

            val subtitleTv = TextView(activity).apply {
                text = if (isGoalAchieved) {
                    "Outstanding focus! You hit your daily study target for today."
                } else {
                    milestoneInfo.subtitle
                }
                textSize = 13f
                setTextColor(activity.themeCoordinator.textColor)
                alpha = 0.65f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(18))
            }

            val btnBg = GradientDrawable().apply {
                setColor(if (isGoalAchieved) primaryColor else milestoneInfo.accentColor)
                cornerRadius = 14f * density
            }

            val actionBtn = Button(activity).apply {
                text = "Keep Moving Forward"
                setTextColor(activity.themeCoordinator.bgColor)
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = btnBg
                setPadding(dp(22), dp(10), dp(22), dp(10))
                setOnClickListener {
                    dialog.dismiss()
                }
            }

            container.addView(iconTv)
            container.addView(titleTv)
            container.addView(subtitleTv)
            container.addView(actionBtn)

            dialog.setContentView(container)
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(targetWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.CENTER)
            }

            // Entrance Scaling
            container.scaleX = 0.6f
            container.scaleY = 0.6f
            container.alpha = 0f
            container.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(400L)
                .setInterpolator(OvershootInterpolator(1.6f))
                .start()

            // Icon Pulse
            iconTv.scaleX = 0.7f
            iconTv.scaleY = 0.7f
            iconTv.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .rotation(12f)
                .setDuration(340L)
                .withEndAction {
                    iconTv.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .rotation(0f)
                        .setDuration(220L)
                        .start()
                }
                .start()

            if (onDismiss != null) {
                dialog.setOnDismissListener { onDismiss.invoke() }
            }

            dialog.show()
        } catch (_: Exception) {}
    }

    /**
     * Spawns an ephemeral, non-blocking Supernova Particle Bloom overlay on top of any view
     * (e.g. when finishing a study session or completing a key planner task).
     */
    fun triggerSupernovaBurst(
        parent: ViewGroup,
        originX: Float = parent.width / 2f,
        originY: Float = parent.height / 2f,
        palette: IntArray? = null
    ) {
        val context = parent.context
        val density = context.resources.displayMetrics.density
        val colors = palette ?: intArrayOf(0xFF8B5CF6.toInt(), 0xFFFFD700.toInt(), 0xFF38BDF8.toInt(), 0xFFFFFFFF.toInt())

        lateinit var overlayView: View
        overlayView = object : View(context) {
            private val particles = ArrayList<SupernovaParticle>()
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
            private val starPath = Path()
            private var animator: ValueAnimator? = null

            init {
                val shapes = BloomShape.values()
                for (i in 0 until 40) {
                    val shape = shapes[i % shapes.size]
                    val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                    val speed = (Random.nextFloat() * 14f + 3f) * density
                    val size = (Random.nextFloat() * 6f + 3f) * density

                    particles.add(
                        SupernovaParticle(
                            x = originX,
                            y = originY,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed - (2f * density),
                            rotation = Random.nextFloat() * 360f,
                            rotSpeed = (Random.nextFloat() - 0.5f) * 14f,
                            size = size,
                            color = colors[Random.nextInt(colors.size)],
                            shape = shape
                        )
                    )
                }

                animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 1800L
                    interpolator = DecelerateInterpolator(1.2f)
                    addUpdateListener { va ->
                        val fraction = va.animatedFraction
                        for (p in particles) {
                            p.x += p.vx
                            p.y += p.vy
                            p.vy += 0.20f * density
                            p.vx *= 0.965f
                            p.rotation += p.rotSpeed
                            p.alpha = (1f - fraction * 0.98f).coerceIn(0f, 1f)
                            p.scale = (1f - fraction * 0.35f).coerceIn(0.2f, 1f)
                        }
                        invalidate()
                    }
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            parent.removeView(overlayView)
                        }
                    })
                    start()
                }
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                for (p in particles) {
                    if (p.alpha <= 0.01f) continue
                    val alphaInt = (p.alpha * 255).toInt().coerceIn(0, 255)

                    canvas.save()
                    canvas.translate(p.x, p.y)
                    canvas.rotate(p.rotation)
                    canvas.scale(p.scale, p.scale)

                    when (p.shape) {
                        BloomShape.STAR_4 -> {
                            fillPaint.color = p.color
                            fillPaint.alpha = alphaInt
                            starPath.reset()
                            starPath.moveTo(0f, -p.size)
                            starPath.quadTo(0f, 0f, p.size, 0f)
                            starPath.quadTo(0f, 0f, 0f, p.size)
                            starPath.quadTo(0f, 0f, -p.size, 0f)
                            starPath.quadTo(0f, 0f, 0f, -p.size)
                            starPath.close()
                            canvas.drawPath(starPath, fillPaint)
                        }
                        BloomShape.DIAMOND -> {
                            fillPaint.color = p.color
                            fillPaint.alpha = alphaInt
                            val half = p.size / 2f
                            canvas.drawRect(-half, -half, half, half, fillPaint)
                        }
                        BloomShape.SPARK_CIRCLE -> {
                            fillPaint.color = p.color
                            fillPaint.alpha = alphaInt
                            canvas.drawCircle(0f, 0f, p.size / 2f, fillPaint)
                        }
                        BloomShape.HALO_RING -> {
                            strokePaint.color = p.color
                            strokePaint.alpha = (alphaInt * 0.85f).toInt()
                            strokePaint.strokeWidth = 1.5f * density
                            canvas.drawCircle(0f, 0f, p.size / 2f, strokePaint)
                        }
                        BloomShape.LIGHT_STREAK -> {
                            strokePaint.color = p.color
                            strokePaint.alpha = alphaInt
                            strokePaint.strokeWidth = 1.4f * density
                            canvas.drawLine(0f, -p.size / 2f, 0f, p.size / 2f, strokePaint)
                        }
                    }
                    canvas.restore()
                }
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                animator?.cancel()
            }
        }

        overlayView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        parent.addView(overlayView)
    }
}
