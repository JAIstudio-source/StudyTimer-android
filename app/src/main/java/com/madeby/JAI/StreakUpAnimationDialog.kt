package com.madeby.JAI

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object StreakUpAnimationDialog {

    private class EmberParticle(
        var x: Float,
        var y: Float,
        var speedX: Float,
        var speedY: Float,
        var radius: Float,
        val color: Int,
        var alpha: Float = 1f
    )

    data class StreakTier(
        val name: String,
        val badgeTitle: String,
        val primaryColor: Int,
        val secondaryColor: Int,
        val particleColors: IntArray,
        val glowColor: Int
    )

    fun getStreakTier(streak: Int): StreakTier {
        return when {
            streak >= 100 -> StreakTier(
                name = "Century",
                badgeTitle = "CENTURY STREAK",
                primaryColor = Color.parseColor("#A855F7"), // Royal Purple
                secondaryColor = Color.parseColor("#FFD700"), // Gold
                particleColors = intArrayOf(
                    Color.parseColor("#A855F7"),
                    Color.parseColor("#C084FC"),
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#EC4899"),
                    Color.parseColor("#F472B6")
                ),
                glowColor = Color.argb(70, 168, 85, 247)
            )
            streak >= 50 -> StreakTier(
                name = "Fire",
                badgeTitle = "FIRE STREAK",
                primaryColor = Color.parseColor("#EF4444"), // Crimson Fire
                secondaryColor = Color.parseColor("#F59E0B"), // Amber
                particleColors = intArrayOf(
                    Color.parseColor("#EF4444"),
                    Color.parseColor("#F97316"),
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#DC2626")
                ),
                glowColor = Color.argb(70, 239, 68, 68)
            )
            streak >= 28 -> StreakTier(
                name = "Diamond",
                badgeTitle = "DIAMOND STREAK",
                primaryColor = Color.parseColor("#00E5FF"), // Diamond Cyan
                secondaryColor = Color.parseColor("#38BDF8"), // Sky
                particleColors = intArrayOf(
                    Color.parseColor("#00E5FF"),
                    Color.parseColor("#38BDF8"),
                    Color.parseColor("#818CF8"),
                    Color.parseColor("#E0F2FE"),
                    Color.parseColor("#A5F3FC")
                ),
                glowColor = Color.argb(70, 0, 229, 255)
            )
            streak >= 21 -> StreakTier(
                name = "Gold",
                badgeTitle = "GOLD STREAK",
                primaryColor = Color.parseColor("#FFD700"), // Brilliant Gold
                secondaryColor = Color.parseColor("#F59E0B"), // Amber
                particleColors = intArrayOf(
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#FEF08A"),
                    Color.parseColor("#FB923C"),
                    Color.parseColor("#FBBF24")
                ),
                glowColor = Color.argb(70, 255, 215, 0)
            )
            streak >= 14 -> StreakTier(
                name = "Silver",
                badgeTitle = "SILVER STREAK",
                primaryColor = Color.parseColor("#CBD5E1"), // Polished Silver
                secondaryColor = Color.parseColor("#94A3B8"),
                particleColors = intArrayOf(
                    Color.parseColor("#CBD5E1"),
                    Color.parseColor("#94A3B8"),
                    Color.parseColor("#E2E8F0"),
                    Color.parseColor("#38BDF8"),
                    Color.parseColor("#F8FAFC")
                ),
                glowColor = Color.argb(60, 203, 213, 225)
            )
            streak >= 7 -> StreakTier(
                name = "Bronze",
                badgeTitle = "BRONZE STREAK",
                primaryColor = Color.parseColor("#CD7F32"), // Warm Bronze
                secondaryColor = Color.parseColor("#D97706"),
                particleColors = intArrayOf(
                    Color.parseColor("#CD7F32"),
                    Color.parseColor("#D97706"),
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#B45309"),
                    Color.parseColor("#FBBF24")
                ),
                glowColor = Color.argb(65, 205, 127, 50)
            )
            else -> StreakTier(
                name = "Starter",
                badgeTitle = "DAILY STREAK",
                primaryColor = Color.parseColor("#F59E0B"), // Amber Flame
                secondaryColor = Color.parseColor("#EF4444"),
                particleColors = intArrayOf(
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#EF4444"),
                    Color.parseColor("#FFD700"),
                    Color.parseColor("#FF7043"),
                    Color.parseColor("#FFA726")
                ),
                glowColor = Color.argb(55, 245, 158, 11)
            )
        }
    }

    fun show(activity: MainActivity, oldStreak: Int, newStreak: Int, onDismiss: (() -> Unit)? = null) {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val density = activity.resources.displayMetrics.density
            val dp = { v: Float -> (v * density) }
            val dpInt = { v: Int -> (v * density).toInt() }
            val targetWidth = (activity.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dpInt(340))

            val tier = getStreakTier(newStreak)

            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#111625"))
                cornerRadius = dp(28f)
                setStroke(dp(2.5f).toInt(), tier.primaryColor)
            }

            val container = object : LinearLayout(activity) {
                private val particles = ArrayList<EmberParticle>()
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                private var animator: ValueAnimator? = null
                private val colors = tier.particleColors

                init {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    background = cardBg
                    clipToOutline = true
                    setPadding(dpInt(22), dpInt(22), dpInt(22), dpInt(20))

                    for (i in 0 until 35) {
                        particles.add(
                            EmberParticle(
                                x = 0f,
                                y = 0f,
                                speedX = (Random.nextFloat() - 0.5f) * dp(3f),
                                speedY = -(Random.nextFloat() * dp(5f) + dp(2f)),
                                radius = (Random.nextFloat() * dp(3.5f) + dp(2f)),
                                color = colors[Random.nextInt(colors.size)]
                            )
                        )
                    }
                }

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    if (w <= 0 || h <= 0) return
                    val cx = w / 2f
                    val cy = h * 0.40f
                    for (p in particles) {
                        p.x = cx + (Random.nextFloat() - 0.5f) * dp(60f)
                        p.y = cy + (Random.nextFloat() - 0.5f) * dp(20f)
                    }
                    animator?.cancel()
                    animator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 3200L
                        addUpdateListener { va ->
                            val fraction = va.animatedFraction
                            for (p in particles) {
                                p.x += p.speedX
                                p.y += p.speedY
                                p.speedX += (Random.nextFloat() - 0.5f) * dp(0.3f)
                                p.alpha = (1f - fraction * 0.95f).coerceIn(0f, 1f)
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
                        paint.color = p.color
                        paint.alpha = (p.alpha * 255).toInt()
                        canvas.drawCircle(p.x, p.y, p.radius, paint)
                    }
                }

                override fun onDetachedFromWindow() {
                    super.onDetachedFromWindow()
                    animator?.cancel()
                }
            }.apply {
                layoutParams = FrameLayout.LayoutParams(targetWidth, FrameLayout.LayoutParams.WRAP_CONTENT)
            }

            // Top Icon Box with Radial Glow Aura
            val flameBox = FrameLayout(activity).apply {
                layoutParams = LinearLayout.LayoutParams(dpInt(76), dpInt(76)).apply {
                    setMargins(0, 0, 0, dpInt(10))
                }
            }

            val glowView = View(activity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(tier.glowColor)
                }
                layoutParams = FrameLayout.LayoutParams(dpInt(76), dpInt(76), Gravity.CENTER)
            }
            flameBox.addView(glowView)

            val flameIcon = ImageView(activity).apply {
                setImageResource(R.drawable.ic_flame)
                setColorFilter(tier.primaryColor)
                layoutParams = FrameLayout.LayoutParams(dpInt(44), dpInt(44), Gravity.CENTER)
            }
            flameBox.addView(flameIcon)
            container.addView(flameBox)

            // Header Tag: Clean student-friendly badge title with matching glow chip
            val tagChip = TextView(activity).apply {
                text = tier.badgeTitle
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(tier.primaryColor)
                background = activity.themeCoordinator.createGlassChip(tier.glowColor, 10f)
                setPadding(dpInt(14), dpInt(5), dpInt(14), dpInt(5))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpInt(12))
                }
            }
            container.addView(tagChip)

            // Number Count-Up Display Container
            val numberContainer = FrameLayout(activity).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpInt(64)).apply {
                    setMargins(0, 0, 0, dpInt(12))
                }
            }

            val oldNumberTv = TextView(activity).apply {
                text = "$oldStreak"
                textSize = 42f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            val newNumberTv = TextView(activity).apply {
                text = "$newStreak"
                textSize = 46f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(tier.primaryColor)
                gravity = Gravity.CENTER
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            numberContainer.addView(oldNumberTv)
            numberContainer.addView(newNumberTv)
            container.addView(numberContainer)

            // Subtitle / Milestone Progress Description (Friendly language, no jargon, no emojis)
            val milestoneSubtitle = getMilestoneProgressText(newStreak)
            val subtitleTv = TextView(activity).apply {
                text = milestoneSubtitle
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(Color.parseColor("#9CA3AF"))
                gravity = Gravity.CENTER
                setPadding(dpInt(8), 0, dpInt(8), dpInt(18))
            }
            container.addView(subtitleTv)

            // Action Button
            val btnBg = GradientDrawable().apply {
                setColor(tier.primaryColor)
                cornerRadius = dp(14f)
            }

            val isDarkTextNeeded = tier.primaryColor == Color.parseColor("#00E5FF") ||
                                   tier.primaryColor == Color.parseColor("#FFD700") ||
                                   tier.primaryColor == Color.parseColor("#CBD5E1")

            val actionBtn = Button(activity).apply {
                text = "Keep Moving Forward"
                setTextColor(if (isDarkTextNeeded) Color.parseColor("#0F172A") else Color.WHITE)
                textSize = 13.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                background = btnBg
                setPadding(dpInt(22), dpInt(10), dpInt(22), dpInt(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpInt(44))
                setOnClickListener {
                    dialog.dismiss()
                }
            }
            container.addView(actionBtn)

            dialog.setContentView(container)
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(targetWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.CENTER)
            }

            // STAGE 1: Smooth Entrance Animation (520ms)
            container.scaleX = 0.5f
            container.scaleY = 0.5f
            container.alpha = 0f
            container.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(520L)
                .setInterpolator(OvershootInterpolator(1.4f))
                .start()

            // STAGE 2: Glow Ring Pulse (600ms)
            glowView.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .alpha(0.9f)
                .setDuration(600L)
                .withEndAction {
                    glowView.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.5f).setDuration(400L).start()
                }
                .start()

            // STAGE 3: Number Count-Up Transition (650ms delay, old shrinks out, new pops in)
            Handler(Looper.getMainLooper()).postDelayed({
                oldNumberTv.animate()
                    .scaleX(0.15f)
                    .scaleY(0.15f)
                    .alpha(0f)
                    .setDuration(380L)
                    .withEndAction {
                        oldNumberTv.visibility = View.GONE
                    }
                    .start()

                newNumberTv.animate()
                    .scaleX(1.25f)
                    .scaleY(1.25f)
                    .alpha(1.0f)
                    .setDuration(500L)
                    .setInterpolator(OvershootInterpolator(1.8f))
                    .withEndAction {
                        newNumberTv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300L).start()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                activity.window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } catch (_: Exception) {}
                        }
                    }
                    .start()

                // Flame Bounce Pulse
                flameIcon.animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .rotation(14f)
                    .setDuration(450L)
                    .withEndAction {
                        flameIcon.animate().scaleX(1.0f).scaleY(1.0f).rotation(0f).setDuration(300L).start()
                    }
                    .start()
            }, 650L)

            if (onDismiss != null) {
                dialog.setOnDismissListener { onDismiss.invoke() }
            }

            dialog.show()
        } catch (_: Exception) {}
    }

    private fun getMilestoneProgressText(streak: Int): String {
        return when {
            streak == 7 -> "7 days completed! You unlocked Bronze Streak!"
            streak == 14 -> "14 days completed! You unlocked Silver Streak!"
            streak == 21 -> "21 days completed! You unlocked Gold Streak!"
            streak == 28 -> "28 days completed! You unlocked Diamond Streak!"
            streak == 50 -> "50 days completed! Half-century milestone reached!"
            streak == 100 -> "100 days completed! Century legend milestone reached!"
            streak in 8..13 -> "${14 - streak} more days to reach Silver Streak"
            streak in 15..20 -> "${21 - streak} more days to reach Gold Streak"
            streak in 22..27 -> "${28 - streak} more days to reach Diamond Streak"
            streak in 29..49 -> "${50 - streak} more days to reach Fire Streak"
            streak in 51..99 -> "${100 - streak} more days to reach Century Legend"
            streak > 100 -> "Incredible $streak days of daily study! Keep it up!"
            streak == 1 -> "Great start! 6 more days to reach Bronze Streak"
            else -> "${7 - streak} more days to reach Bronze Streak"
        }
    }
}
