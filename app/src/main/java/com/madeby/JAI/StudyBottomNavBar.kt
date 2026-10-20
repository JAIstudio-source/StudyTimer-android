package com.madeby.JAI

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Modern, tactile 4-tab Bottom Navigation Bar for StudyTimer.
 * Destinations: Focus (⏱️), Planner (📋), Insights (📊), Community (👥).
 * Hardware-accelerated sliding spring indicator pill with haptic micro-ticks.
 */
class StudyBottomNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).toInt()

    data class TabDestination(
        val panel: AppPanel,
        val label: String,
        val iconRes: Int
    )

    private val destinations = listOf(
        TabDestination(AppPanel.FOCUS, "Focus", R.drawable.ic_clock),
        TabDestination(AppPanel.PLANNER, "Planner", R.drawable.ic_check_circle),
        TabDestination(AppPanel.STATS, "Insights", R.drawable.ic_insights),
        TabDestination(AppPanel.COMMUNITY, "Community", R.drawable.ic_trophy_placeholder)
    )

    var onTabSelectedListener: ((AppPanel) -> Unit)? = null

    private var activeIndex = 0
    private var isBarVisible = true
    private var primaryAccentColor: Int = 0xFF6366F1.toInt()
    private var isDarkTheme: Boolean = true

    private val indicatorView: View
    private val tabsContainer: LinearLayout
    private val tabViews = mutableListOf<LinearLayout>()
    private val iconViews = mutableListOf<ImageView>()
    private val labelViews = mutableListOf<TextView>()

    private var indicatorAnimator: ValueAnimator? = null

    init {
        elevation = 16f * density
        clipToPadding = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            z = 16f * density
        }

        // AMOLED surface background with subtle stroke
        background = GradientDrawable().apply {
            cornerRadius = 28f * density
            setColor(Color.parseColor("#141620"))
            setStroke(dp(1), Color.parseColor("#26293A"))
        }

        // Smooth sliding spring indicator pill
        indicatorView = View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = 20f * density
                setColor(tintWithAlpha(primaryAccentColor, 0.18f))
                setStroke(dp(1), tintWithAlpha(primaryAccentColor, 0.45f))
            }
        }
        addView(indicatorView, LayoutParams(0, dp(44)).apply {
            gravity = Gravity.CENTER_VERTICAL
        })

        // Tabs container
        tabsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        addView(tabsContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        buildTabs()
    }

    private fun tintWithAlpha(color: Int, alphaRatio: Float): Int {
        val a = ((color ushr 24) * alphaRatio).toInt().coerceIn(0, 255)
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun applyTheme(themeCoordinator: ThemeCoordinator) {
        isDarkTheme = themeCoordinator.isDarkMode()
        primaryAccentColor = themeCoordinator.primaryColor

        // Background adapts to theme - Obsidian Frosted Floating Island
        val bgDrawable = GradientDrawable().apply {
            cornerRadius = 28f * density
            if (isDarkTheme) {
                if (themeCoordinator.activeBgMode == "ECLIPSE") {
                    setColor(0xF0141824.toInt())
                    setStroke((1f * density).toInt(), Color.argb(35, 255, 255, 255))
                } else if (themeCoordinator.activeBgMode == "OLED") {
                    setColor(0xF508090D.toInt())
                    setStroke((1f * density).toInt(), Color.argb(28, 255, 255, 255))
                } else {
                    setColor(0xF011141E.toInt())
                    setStroke((1f * density).toInt(), Color.argb(32, 255, 255, 255))
                }
            } else {
                setColor(0xF5FFFFFF.toInt())
                setStroke((1f * density).toInt(), 0xFFE2E8F0.toInt())
            }
        }
        background = bgDrawable

        // Indicator styling with glowing active pill
        (indicatorView.background as? GradientDrawable)?.apply {
            setColor(tintWithAlpha(primaryAccentColor, if (isDarkTheme) 0.22f else 0.14f))
            setStroke((1f * density).toInt(), tintWithAlpha(primaryAccentColor, if (isDarkTheme) 0.65f else 0.40f))
        }

        val customTf = themeCoordinator.getAppTypeface(isBold = true)
        for (lbl in labelViews) {
            lbl.typeface = customTf
        }

        updateTabStyles(activeIndex, animated = false)
    }

    private fun buildTabs() {
        tabViews.clear()
        iconViews.clear()
        labelViews.clear()
        tabsContainer.removeAllViews()

        for (i in destinations.indices) {
            val dest = destinations[i]

            val tabLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setPadding(0, dp(4), 0, dp(4))
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }

            val icon = ImageView(context).apply {
                setImageResource(dest.iconRes)
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                    bottomMargin = dp(2)
                }
            }

            val label = TextView(context).apply {
                text = dest.label
                textSize = 11f
                typeface = FontStyleManager.getBoldTypeface(context)
                gravity = Gravity.CENTER
            }

            tabLayout.addView(icon)
            tabLayout.addView(label)

            tabLayout.setOnClickListener {
                if (activeIndex != i) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    selectTabByIndex(i, animated = true)
                    onTabSelectedListener?.invoke(dest.panel)
                }
            }

            tabViews.add(tabLayout)
            iconViews.add(icon)
            labelViews.add(label)
            tabsContainer.addView(tabLayout)
        }

        post {
            positionIndicator(activeIndex, animated = false)
            updateTabStyles(activeIndex, animated = false)
        }
    }

    fun selectTab(panel: AppPanel, animated: Boolean = true) {
        val targetIndex = destinations.indexOfFirst { it.panel == panel }
        if (targetIndex >= 0) {
            selectTabByIndex(targetIndex, animated)
        }
    }

    private fun selectTabByIndex(index: Int, animated: Boolean) {
        if (index !in destinations.indices) return
        val prevIndex = activeIndex
        activeIndex = index
        positionIndicator(index, animated)
        updateTabStyles(index, animated)

        if (animated && prevIndex != index) {
            val activeTab = tabViews[index]
            activeTab.scaleX = 0.90f
            activeTab.scaleY = 0.90f
            activeTab.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }
    }

    private fun updateTabStyles(selectedIndex: Int, animated: Boolean) {
        val unselectedColor = if (isDarkTheme) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        for (i in destinations.indices) {
            val isSelected = i == selectedIndex
            val icon = iconViews.getOrNull(i) ?: continue
            val label = labelViews.getOrNull(i) ?: continue

            val targetColor = if (isSelected) primaryAccentColor else unselectedColor
            val targetAlpha = if (isSelected) 1.0f else 0.65f

            icon.setColorFilter(targetColor)
            label.setTextColor(targetColor)

            if (animated) {
                label.animate().alpha(targetAlpha).setDuration(160).start()
                icon.animate().alpha(targetAlpha).setDuration(160).start()
            } else {
                label.alpha = targetAlpha
                icon.alpha = targetAlpha
            }
        }
    }

    private fun positionIndicator(index: Int, animated: Boolean) {
        val tab = tabViews.getOrNull(index) ?: return
        if (tab.width == 0) {
            post { positionIndicator(index, animated) }
            return
        }

        val targetWidth = (tab.width - dp(6)).coerceAtLeast(dp(48))
        val targetX = tab.left + (tab.width - targetWidth) / 2f

        indicatorAnimator?.cancel()
        if (animated) {
            val startX = indicatorView.x
            val startW = indicatorView.layoutParams.width.takeIf { it > 0 }?.toFloat() ?: targetWidth.toFloat()

            indicatorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 260
                interpolator = OvershootInterpolator(1.08f)
                addUpdateListener { anim ->
                    val frac = anim.animatedFraction
                    indicatorView.x = startX + (targetX - startX) * frac
                    val curW = (startW + (targetWidth - startW) * frac).toInt()
                    indicatorView.layoutParams.width = curW
                    indicatorView.requestLayout()
                }
                start()
            }
        } else {
            indicatorView.x = targetX
            indicatorView.layoutParams.width = targetWidth
            indicatorView.requestLayout()
        }
    }

    /**
     * Maintains crisp 100% opacity for bottom navigation bar without unwanted translucency.
     */
    fun setStudyingDim(isStudying: Boolean) {
        alpha = 1.0f
    }

    /**
     * Toggles smooth entrance / exit of the bottom bar (e.g. fullscreen mode or landscape).
     */
    fun setBarVisibility(visible: Boolean, animated: Boolean = true) {
        if (isBarVisible == visible) return
        isBarVisible = visible

        val targetTranslationY = if (visible) 0f else (height.toFloat() + dp(28))
        val targetAlpha = if (visible) 1f else 0f

        if (animated) {
            animate()
                .translationY(targetTranslationY)
                .alpha(targetAlpha)
                .setDuration(200)
                .withEndAction {
                    visibility = if (visible) View.VISIBLE else View.GONE
                }
                .start()
        } else {
            translationY = targetTranslationY
            alpha = targetAlpha
            visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
