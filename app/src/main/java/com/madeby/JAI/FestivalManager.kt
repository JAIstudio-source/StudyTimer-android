package com.madeby.JAI

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data model for a festival greeting template.
 * Supports {name} replacement for personalized greetings without awkward punctuation.
 */
data class FestivalGreeting(
    val titleTemplate: String,
    val messageTemplate: String
) {
    fun format(userName: String?): Pair<String, String> {
        val nameSuffix = if (!userName.isNullOrBlank()) ", ${userName.trim()}" else ""
        val resolvedTitle = titleTemplate.replace("{name}", nameSuffix)
        val resolvedMessage = messageTemplate.replace("{name}", userName?.trim() ?: "")
        return Pair(resolvedTitle, resolvedMessage)
    }
}

/**
 * Strictly typed Festival definition.
 * Each festival maintains its own dedicated greetings list to prevent any possible mismatch.
 * Contains no emojis; clean, motivating, and enjoying.
 */
enum class FestivalType(
    val key: String,
    val displayName: String,
    val defaultTitle: String,
    val greetings: List<FestivalGreeting>
) {
    DIWALI(
        key = "diwali",
        displayName = "Diwali",
        defaultTitle = "Happy Diwali{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Diwali{name}!",
                messageTemplate = "May this festival of lights illuminate your goals and bring happiness to you and your family. Enjoy the celebrations and keep moving towards your dreams!"
            ),
            FestivalGreeting(
                titleTemplate = "Wishing you a Happy Diwali{name}!",
                messageTemplate = "Celebrate the joy, sweets, and lights today! Wishing you great focus, happiness, and continuous success in everything you strive for."
            ),
            FestivalGreeting(
                titleTemplate = "Shubh Deepavali{name}!",
                messageTemplate = "May your journey ahead shine bright. Enjoy the festivities with your loved ones and stay inspired!"
            )
        )
    ),

    HOLI(
        key = "holi",
        displayName = "Holi",
        defaultTitle = "Happy Holi{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Holi{name}!",
                messageTemplate = "Wishing you a vibrant day filled with colors, laughter, and sweet moments with family and friends. Enjoy every moment of the celebration!"
            ),
            FestivalGreeting(
                titleTemplate = "Wishing you a joyful Holi{name}!",
                messageTemplate = "May this colorful day bring fresh energy and positivity to your life and learning journey. Have a wonderful celebration!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Holi{name}!",
                messageTemplate = "Celebrate with colors, joy, and good cheer today. Here is to new energy, happiness, and success ahead!"
            )
        )
    ),

    EID_UL_FITR(
        key = "eid_ul_fitr",
        displayName = "Eid-ul-Fitr",
        defaultTitle = "Eid Mubarak{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Eid Mubarak{name}!",
                messageTemplate = "Wishing you and your family peace, happiness, and prosperity on this blessed occasion. Enjoy the celebrations and stay inspired!"
            ),
            FestivalGreeting(
                titleTemplate = "Eid Mubarak{name}!",
                messageTemplate = "May the joy and blessings of this day fill your heart and inspire your journey ahead. Have a wonderful time with your loved ones!"
            ),
            FestivalGreeting(
                titleTemplate = "Warm Eid Wishes{name}!",
                messageTemplate = "Celebrating dedication, patience, and joy. Wishing you success in all your goals and a wonderful day of festivities!"
            )
        )
    ),

    EID_UL_ADHA(
        key = "eid_ul_adha",
        displayName = "Eid-ul-Adha",
        defaultTitle = "Eid-ul-Adha Mubarak{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Eid-ul-Adha Mubarak{name}!",
                messageTemplate = "May this special day bring peace, gratitude, and happiness to your home. Enjoy the celebration with family and friends!"
            ),
            FestivalGreeting(
                titleTemplate = "Eid Mubarak{name}!",
                messageTemplate = "Wishing you joy, togetherness, and renewed inspiration. Have a memorable celebration with your loved ones!"
            ),
            FestivalGreeting(
                titleTemplate = "Blessed Eid-ul-Adha{name}!",
                messageTemplate = "May your dedication and perseverance lead to great achievements. Enjoy the festive moments today!"
            )
        )
    ),

    CHRISTMAS(
        key = "christmas",
        displayName = "Christmas",
        defaultTitle = "Merry Christmas{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Merry Christmas{name}!",
                messageTemplate = "Wishing you a wonderful holiday season filled with warmth, peace, and cheer. Enjoy the celebration with your loved ones!"
            ),
            FestivalGreeting(
                titleTemplate = "Christmas Greetings{name}!",
                messageTemplate = "May your Christmas season be joyful and inspiring. Take time to relax, recharge, and look forward to new achievements ahead!"
            ),
            FestivalGreeting(
                titleTemplate = "Merry Christmas{name}!",
                messageTemplate = "Wishing you happiness, good health, and bright beginnings for all your learning goals!"
            )
        )
    ),

    DUSSEHRA(
        key = "dussehra",
        displayName = "Dussehra",
        defaultTitle = "Happy Dussehra{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Dussehra{name}!",
                messageTemplate = "Celebrating the victory of wisdom, perseverance, and dedication. May you overcome every challenge and achieve your highest goals!"
            ),
            FestivalGreeting(
                titleTemplate = "Shubh Vijayadashami{name}!",
                messageTemplate = "May focus and hard work lead you to continuous success. Enjoy the festivities and keep striving forward with confidence!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Dussehra{name}!",
                messageTemplate = "A celebration of strength, focus, and triumph. Wishing you great confidence in everything you set out to achieve!"
            )
        )
    ),

    RAKSHA_BANDHAN(
        key = "raksha_bandhan",
        displayName = "Raksha Bandhan",
        defaultTitle = "Happy Raksha Bandhan{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Raksha Bandhan{name}!",
                messageTemplate = "Celebrating the bond of love, care, and encouragement. Have a wonderful day of togetherness with your family!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Raksha Bandhan{name}!",
                messageTemplate = "May the encouragement and support of your loved ones inspire you on your journey. Enjoy every moment today!"
            )
        )
    ),

    MAKAR_SANKRANTI(
        key = "makar_sankranti",
        displayName = "Makar Sankranti and Pongal",
        defaultTitle = "Happy Makar Sankranti{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Makar Sankranti{name}!",
                messageTemplate = "May your goals and aspirations soar high. Wishing you and your family happiness, sweetness, and great beginnings!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Pongal and Sankranti{name}!",
                messageTemplate = "Wishing you good health, prosperity, and renewed energy for all your studies ahead. Enjoy the celebrations!"
            )
        )
    ),

    NEW_YEAR(
        key = "new_year",
        displayName = "New Year's Day",
        defaultTitle = "Happy New Year{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy New Year{name}!",
                messageTemplate = "A brand new year with fresh opportunities and goals. Wishing you consistency, focus, and great success in the year ahead!"
            ),
            FestivalGreeting(
                titleTemplate = "Welcome to a New Year{name}!",
                messageTemplate = "Every day is a step toward your dreams. May this year be your most rewarding, focused, and accomplished year yet!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy New Year{name}!",
                messageTemplate = "Make this year count with strong habits and clear goals. Wishing you continuous growth and achievement!"
            )
        )
    ),

    REPUBLIC_DAY(
        key = "republic_day",
        displayName = "Republic Day",
        defaultTitle = "Happy Republic Day{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Republic Day{name}!",
                messageTemplate = "Celebrating the values, progress, and unity of our nation. Wishing you pride, inspiration, and excellence in your learning!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Republic Day{name}!",
                messageTemplate = "Knowledge and dedication shape a better tomorrow. Keep learning and striving for excellence!"
            )
        )
    ),

    INDEPENDENCE_DAY(
        key = "independence_day",
        displayName = "Independence Day",
        defaultTitle = "Happy Independence Day{name}!",
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Independence Day{name}!",
                messageTemplate = "Celebrating the spirit of freedom, vision, and growth. Wishing you great inspiration and success on your path ahead!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Independence Day{name}!",
                messageTemplate = "Knowledge brings true freedom and strength. Keep learning, growing, and reaching for your goals!"
            )
        )
    );

    /**
     * Retrieves a greeting template strictly belonging to this festival.
     * Uses year / index to rotate through variations without any possibility of cross-festival mismatch.
     */
    fun getGreeting(variationIndex: Int? = null, year: Int = Calendar.getInstance().get(Calendar.YEAR)): FestivalGreeting {
        if (greetings.isEmpty()) {
            return FestivalGreeting(defaultTitle, "Wishing you a wonderful celebration!")
        }
        val idx = if (variationIndex != null && variationIndex in greetings.indices) {
            variationIndex
        } else {
            Math.abs(year xor key.hashCode()) % greetings.size
        }
        return greetings[idx]
    }

    companion object {
        fun fromKey(key: String): FestivalType? {
            return values().firstOrNull { it.key.equals(key, ignoreCase = true) }
        }
    }
}

/**
 * Manages festival calendar lookup, personalized greeting formatting, and notification dispatch.
 * Fully autonomous with multi-year lookup tables from 2024 through 2036+.
 */
object FestivalManager {

    const val PREF_FESTIVAL_GREETINGS_ENABLED = "festival_greetings_enabled"
    private const val PREFS_NAME = "StudyTimerPrefs"
    private const val PREF_LAST_NOTIFIED_DATE = "last_festival_notified_date"
    private const val PREF_LAST_NOTIFIED_KEY = "last_festival_notified_key"

    /**
     * Precomputed astronomical / lunar festival dates (YYYY-MM-DD -> FestivalType).
     * Accurate for 2024 to 2036.
     */
    private val MOVABLE_FESTIVAL_CALENDAR = mapOf(
        // --- 2024 ---
        "2024-03-25" to FestivalType.HOLI,
        "2024-04-10" to FestivalType.EID_UL_FITR,
        "2024-04-11" to FestivalType.EID_UL_FITR,
        "2024-06-17" to FestivalType.EID_UL_ADHA,
        "2024-08-19" to FestivalType.RAKSHA_BANDHAN,
        "2024-10-12" to FestivalType.DUSSEHRA,
        "2024-10-31" to FestivalType.DIWALI,
        "2024-11-01" to FestivalType.DIWALI,

        // --- 2025 ---
        "2025-03-14" to FestivalType.HOLI,
        "2025-03-31" to FestivalType.EID_UL_FITR,
        "2025-06-07" to FestivalType.EID_UL_ADHA,
        "2025-08-09" to FestivalType.RAKSHA_BANDHAN,
        "2025-10-02" to FestivalType.DUSSEHRA,
        "2025-10-20" to FestivalType.DIWALI,

        // --- 2026 ---
        "2026-03-04" to FestivalType.HOLI,
        "2026-03-20" to FestivalType.EID_UL_FITR,
        "2026-05-27" to FestivalType.EID_UL_ADHA,
        "2026-08-28" to FestivalType.RAKSHA_BANDHAN,
        "2026-10-20" to FestivalType.DUSSEHRA,
        "2026-11-08" to FestivalType.DIWALI,

        // --- 2027 ---
        "2027-03-10" to FestivalType.EID_UL_FITR,
        "2027-03-23" to FestivalType.HOLI,
        "2027-05-16" to FestivalType.EID_UL_ADHA,
        "2027-08-17" to FestivalType.RAKSHA_BANDHAN,
        "2027-10-09" to FestivalType.DUSSEHRA,
        "2027-10-29" to FestivalType.DIWALI,

        // --- 2028 ---
        "2028-02-27" to FestivalType.EID_UL_FITR,
        "2028-03-11" to FestivalType.HOLI,
        "2028-05-05" to FestivalType.EID_UL_ADHA,
        "2028-08-05" to FestivalType.RAKSHA_BANDHAN,
        "2028-09-27" to FestivalType.DUSSEHRA,
        "2028-10-17" to FestivalType.DIWALI,

        // --- 2029 ---
        "2029-02-15" to FestivalType.EID_UL_FITR,
        "2029-03-01" to FestivalType.HOLI,
        "2029-04-24" to FestivalType.EID_UL_ADHA,
        "2029-08-23" to FestivalType.RAKSHA_BANDHAN,
        "2029-10-16" to FestivalType.DUSSEHRA,
        "2029-11-05" to FestivalType.DIWALI,

        // --- 2030 ---
        "2030-02-05" to FestivalType.EID_UL_FITR,
        "2030-03-20" to FestivalType.HOLI,
        "2030-04-13" to FestivalType.EID_UL_ADHA,
        "2030-08-13" to FestivalType.RAKSHA_BANDHAN,
        "2030-10-06" to FestivalType.DUSSEHRA,
        "2030-10-26" to FestivalType.DIWALI,

        // --- 2031 ---
        "2031-01-24" to FestivalType.EID_UL_FITR,
        "2031-03-09" to FestivalType.HOLI,
        "2031-04-02" to FestivalType.EID_UL_ADHA,
        "2031-08-02" to FestivalType.RAKSHA_BANDHAN,
        "2031-10-25" to FestivalType.DUSSEHRA,
        "2031-11-14" to FestivalType.DIWALI,

        // --- 2032 ---
        "2032-01-14" to FestivalType.EID_UL_FITR,
        "2032-02-26" to FestivalType.HOLI,
        "2032-03-22" to FestivalType.EID_UL_ADHA,
        "2032-08-20" to FestivalType.RAKSHA_BANDHAN,
        "2032-10-14" to FestivalType.DUSSEHRA,
        "2032-11-02" to FestivalType.DIWALI,

        // --- 2033 ---
        "2033-01-02" to FestivalType.EID_UL_FITR,
        "2033-03-11" to FestivalType.EID_UL_ADHA,
        "2033-03-16" to FestivalType.HOLI,
        "2033-08-10" to FestivalType.RAKSHA_BANDHAN,
        "2033-10-03" to FestivalType.DUSSEHRA,
        "2033-10-22" to FestivalType.DIWALI,
        "2033-12-23" to FestivalType.EID_UL_FITR,

        // --- 2034 ---
        "2034-03-01" to FestivalType.EID_UL_ADHA,
        "2034-03-06" to FestivalType.HOLI,
        "2034-08-29" to FestivalType.RAKSHA_BANDHAN,
        "2034-10-22" to FestivalType.DUSSEHRA,
        "2034-11-10" to FestivalType.DIWALI,
        "2034-12-12" to FestivalType.EID_UL_FITR,

        // --- 2035 ---
        "2035-02-18" to FestivalType.EID_UL_ADHA,
        "2035-03-24" to FestivalType.HOLI,
        "2035-08-18" to FestivalType.RAKSHA_BANDHAN,
        "2035-10-11" to FestivalType.DUSSEHRA,
        "2035-10-30" to FestivalType.DIWALI,
        "2035-12-01" to FestivalType.EID_UL_FITR,

        // --- 2036 ---
        "2036-02-07" to FestivalType.EID_UL_ADHA,
        "2036-03-12" to FestivalType.HOLI,
        "2036-08-07" to FestivalType.RAKSHA_BANDHAN,
        "2036-09-29" to FestivalType.DUSSEHRA,
        "2036-10-18" to FestivalType.DIWALI,
        "2036-11-20" to FestivalType.EID_UL_FITR
    )

    /**
     * Resolves the user's display first name for personalized greetings.
     */
    fun getUserFirstName(context: Context): String? {
        val effective = ProfileManager.getEffectiveDisplayName(context).trim()
        val resolved = if (effective.isNotBlank() && !effective.equals("Student", ignoreCase = true)) {
            effective
        } else {
            AuthManager.getUserName(context)?.trim()
        }
        if (resolved.isNullOrBlank() || resolved.equals("Student", ignoreCase = true)) return null
        return resolved.split(" ").firstOrNull { it.isNotBlank() } ?: resolved
    }

    /**
     * Resolves the festival for a given date in "yyyy-MM-dd" format.
     * Checks both the lunar lookup table and fixed solar annual dates.
     */
    fun getFestivalForDate(dateStr: String): FestivalType? {
        // 1. Check movable / lunar calendar first
        MOVABLE_FESTIVAL_CALENDAR[dateStr]?.let { return it }

        // 2. Check fixed solar dates (MM-DD)
        if (dateStr.length >= 10) {
            val mmDd = dateStr.substring(5, 10)
            when (mmDd) {
                "01-01" -> return FestivalType.NEW_YEAR
                "01-14" -> return FestivalType.MAKAR_SANKRANTI
                "01-26" -> return FestivalType.REPUBLIC_DAY
                "08-15" -> return FestivalType.INDEPENDENCE_DAY
                "12-25" -> return FestivalType.CHRISTMAS
            }
        }
        return null
    }

    /**
     * Returns today's festival if today is an occasion, or null.
     */
    fun getTodayFestival(): FestivalType? {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return getFestivalForDate(todayStr)
    }

    /**
     * Checks if today is a festival and posts the notification if not already posted today.
     * Can also be called with forced parameters for previewing/testing in developer tools.
     */
    fun checkAndNotify(
        context: Context,
        forceFestival: FestivalType? = null,
        forceVariation: Int? = null,
        isTest: Boolean = false
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if user enabled festival greetings
        if (!isTest && !prefs.getBoolean(PREF_FESTIVAL_GREETINGS_ENABLED, true)) {
            return false
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val festival = forceFestival ?: getFestivalForDate(todayStr) ?: return false

        // Prevent duplicate notifications for the same festival on the same day unless it's an explicit test
        val festivalEventKey = "${festival.key}_$todayStr"
        if (!isTest) {
            val lastKey = prefs.getString(PREF_LAST_NOTIFIED_KEY, null)
            val lastDate = prefs.getString(PREF_LAST_NOTIFIED_DATE, null)
            if (festivalEventKey == lastKey || todayStr == lastDate) {
                return false
            }
        }

        val year = Calendar.getInstance().get(Calendar.YEAR)
        val greetingTemplate = festival.getGreeting(variationIndex = forceVariation, year = year)
        val firstName = getUserFirstName(context)
        val (title, message) = greetingTemplate.format(firstName)

        postNotification(context, festival, title, message, isTest)

        if (!isTest) {
            prefs.edit()
                .putString(PREF_LAST_NOTIFIED_KEY, festivalEventKey)
                .putString(PREF_LAST_NOTIFIED_DATE, todayStr)
                .apply()
        }
        return true
    }

    private fun postNotification(
        context: Context,
        festival: FestivalType,
        title: String,
        message: String,
        isTest: Boolean
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        NotificationHelper.createAllNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("opened_from_festival", true)
            putExtra("festival_key", festival.key)
        }
        val notifId = if (isTest) {
            NotificationHelper.NOTIFICATION_ID_FESTIVAL + (festival.ordinal * 10) + (System.currentTimeMillis() % 10).toInt()
        } else {
            NotificationHelper.NOTIFICATION_ID_FESTIVAL
        }
        val openPending = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_FESTIVALS)
            .setSmallIcon(R.drawable.ic_flame)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setAutoCancel(true)
            .setColor(0xFFF59E0B.toInt()) // Festive amber accent
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPending)
            .addAction(0, "Open StudyTimer", openPending)
            .build()

        nm.notify(notifId, notification)
    }
}
