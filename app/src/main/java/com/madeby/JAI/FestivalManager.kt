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
    val messageTemplate: String,
    val hiTitleTemplate: String? = null,
    val hiMessageTemplate: String? = null
) {
    fun format(userName: String?, languageCode: String? = null): Pair<String, String> {
        val useHindi = languageCode?.equals("hi", ignoreCase = true) == true &&
                !hiTitleTemplate.isNullOrBlank() && !hiMessageTemplate.isNullOrBlank()
        val tTemplate = if (useHindi) hiTitleTemplate!! else titleTemplate
        val mTemplate = if (useHindi) hiMessageTemplate!! else messageTemplate
        val nameSuffix = if (!userName.isNullOrBlank()) ", ${userName.trim()}" else ""
        val resolvedTitle = tTemplate.replace("{name}", nameSuffix)
        val resolvedMessage = mTemplate.replace("{name}", userName?.trim() ?: "")
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
    val countryCodes: List<String> = listOf("ALL"),
    val greetings: List<FestivalGreeting>
) {
    DIWALI(
        key = "diwali",
        displayName = "Diwali",
        defaultTitle = "Happy Diwali{name}!",
        countryCodes = listOf("IN"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Diwali{name}!",
                messageTemplate = "May this festival of lights illuminate your goals and bring happiness to you and your family. Enjoy the celebrations and keep moving towards your dreams!",
                hiTitleTemplate = "दीपावली की हार्दिक शुभकामनाएँ{name}!",
                hiMessageTemplate = "दीपों का यह पावन पर्व आपके जीवन में नई उमंग, सुख और सफलता लेकर आए। अपने लक्ष्यों की ओर निरंतर बढ़ते रहें!"
            ),
            FestivalGreeting(
                titleTemplate = "Wishing you a Happy Diwali{name}!",
                messageTemplate = "Celebrate the joy, sweets, and lights today! Wishing you great focus, happiness, and continuous success in everything you strive for.",
                hiTitleTemplate = "शुभ दीपावली{name}!",
                hiMessageTemplate = "उमंग, मिठास और दीपों का यह पर्व आपके अध्ययन और जीवन में सुख, शांति और प्रगति लाए।"
            ),
            FestivalGreeting(
                titleTemplate = "Shubh Deepavali{name}!",
                messageTemplate = "May your journey ahead shine bright. Enjoy the festivities with your loved ones and stay inspired!",
                hiTitleTemplate = "शुभ दीपावली{name}!",
                hiMessageTemplate = "ईश्वर से प्रार्थना है कि आपकी मेहनत और लगन आपको उच्चतम मुकाम तक पहुँचाए। उत्सव का आनंद लें!"
            )
        )
    ),

    HOLI(
        key = "holi",
        displayName = "Holi",
        defaultTitle = "Happy Holi{name}!",
        countryCodes = listOf("IN"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Holi{name}!",
                messageTemplate = "Wishing you a vibrant day filled with colors, laughter, and sweet moments with family and friends. Enjoy every moment of the celebration!",
                hiTitleTemplate = "होली की हार्दिक शुभकामनाएँ{name}!",
                hiMessageTemplate = "रंगों का यह पावन त्यौहार आपके जीवन में खुशियाँ, नई ऊर्जा और सफलता लेकर आए। पूरे उत्साह से मनाएँ!"
            ),
            FestivalGreeting(
                titleTemplate = "Wishing you a joyful Holi{name}!",
                messageTemplate = "May this colorful day bring fresh energy and positivity to your life and learning journey. Have a wonderful celebration!",
                hiTitleTemplate = "आनंदमयी होली की शुभकामनाएँ{name}!",
                hiMessageTemplate = "यह रंगोत्सव आपके अध्ययन पथ पर सकारात्मकता, नई प्रेरणा और नई उपलब्धियाँ लेकर आए।"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Holi{name}!",
                messageTemplate = "Celebrate with colors, joy, and good cheer today. Here is to new energy, happiness, and success ahead!",
                hiTitleTemplate = "होली की हार्दिक बधाई{name}!",
                hiMessageTemplate = "रंगों, उल्लास और मिठास के साथ आज का दिन मनाएँ और नए जोश के साथ अपने सपनों को पूरा करें!"
            )
        )
    ),

    EID_UL_FITR(
        key = "eid_ul_fitr",
        displayName = "Eid-ul-Fitr",
        defaultTitle = "Eid Mubarak{name}!",
        countryCodes = listOf("IN"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Eid Mubarak{name}!",
                messageTemplate = "Wishing you and your family peace, happiness, and prosperity on this blessed occasion. Enjoy the celebrations and stay inspired!",
                hiTitleTemplate = "ईद मुबारक{name}!",
                hiMessageTemplate = "ईद-उल-फ़ित्र के इस मुबारक मौके पर आपको और आपके परिवार को दिली मुबारकबाद। सुख, शांति और तरक्की की दुआ!"
            ),
            FestivalGreeting(
                titleTemplate = "Eid Mubarak{name}!",
                messageTemplate = "May the joy and blessings of this day fill your heart and inspire your journey ahead. Have a wonderful time with your loved ones!",
                hiTitleTemplate = "ईद की दिली मुबारकबाद{name}!",
                hiMessageTemplate = "यह पावन पर्व आपके जीवन में खुशियाँ, बरकत और नई ऊर्जा भरे। अपनों के साथ जश्न मनाएँ!"
            ),
            FestivalGreeting(
                titleTemplate = "Warm Eid Wishes{name}!",
                messageTemplate = "Celebrating dedication, patience, and joy. Wishing you success in all your goals and a wonderful day of festivities!",
                hiTitleTemplate = "ईद मुबारक{name}!",
                hiMessageTemplate = "सब्र, निष्ठा और समर्पण की भावना आपके लक्ष्यों को नई ऊँचाइयों तक पहुँचाए। ईद की शुभकामनाएँ!"
            )
        )
    ),

    EID_UL_ADHA(
        key = "eid_ul_adha",
        displayName = "Eid-ul-Adha",
        defaultTitle = "Eid-ul-Adha Mubarak{name}!",
        countryCodes = listOf("IN"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Eid-ul-Adha Mubarak{name}!",
                messageTemplate = "May this special day bring peace, gratitude, and happiness to your home. Enjoy the celebration with family and friends!",
                hiTitleTemplate = "बकरीद मुबारक{name}!",
                hiMessageTemplate = "ईद-उल-अज़हा के इस पावन पर्व पर आपके जीवन में अमन, भाईचारा और खुशियाँ हमेशा बनी रहें।"
            ),
            FestivalGreeting(
                titleTemplate = "Eid Mubarak{name}!",
                messageTemplate = "Wishing you joy, togetherness, and renewed inspiration. Have a memorable celebration with your loved ones!",
                hiTitleTemplate = "ईद मुबारक{name}!",
                hiMessageTemplate = "त्याग, विश्वास और समर्पण का यह त्यौहार आपके जीवन में नई रोशनी और उमंग लाए।"
            ),
            FestivalGreeting(
                titleTemplate = "Blessed Eid-ul-Adha{name}!",
                messageTemplate = "May your dedication and perseverance lead to great achievements. Enjoy the festive moments today!",
                hiTitleTemplate = "ईद-उल-अज़हा की मुबारकबाद{name}!",
                hiMessageTemplate = "आपकी मेहनत, लगन और एकाग्रता आपको सफलता के सर्वोच्च शिखर तक पहुँचाए। मुबारकबाद!"
            )
        )
    ),

    CHRISTMAS(
        key = "christmas",
        displayName = "Christmas",
        defaultTitle = "Merry Christmas{name}!",
        countryCodes = listOf("ALL"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Merry Christmas{name}!",
                messageTemplate = "Wishing you a wonderful holiday season filled with warmth, peace, and cheer. Enjoy the celebration with your loved ones!",
                hiTitleTemplate = "क्रिसमस की हार्दिक शुभकामनाएँ{name}!",
                hiMessageTemplate = "क्रिसमस का यह पावन पर्व आपके और आपके परिवार के जीवन में शांति, सुख और खुशहाली लेकर आए।"
            ),
            FestivalGreeting(
                titleTemplate = "Christmas Greetings{name}!",
                messageTemplate = "May your Christmas season be joyful and inspiring. Take time to relax, recharge, and look forward to new achievements ahead!",
                hiTitleTemplate = "क्रिसमस की ढेरों शुभकामनाएँ{name}!",
                hiMessageTemplate = "यह त्यौहार आपके जीवन में नई ऊर्जा, प्रेरणा और नए लक्ष्यों की ओर बढ़ने का उत्साह भरे।"
            ),
            FestivalGreeting(
                titleTemplate = "Merry Christmas{name}!",
                messageTemplate = "Wishing you happiness, good health, and bright beginnings for all your learning goals!",
                hiTitleTemplate = "मैरी क्रिसमस{name}!",
                hiMessageTemplate = "उत्सव का आनंद लें और अपने सपनों की ओर पूरे विश्वास के साथ आगे बढ़ते रहें!"
            )
        )
    ),

    DUSSEHRA(
        key = "dussehra",
        displayName = "Dussehra",
        defaultTitle = "Happy Dussehra{name}!",
        countryCodes = listOf("IN"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Dussehra{name}!",
                messageTemplate = "Celebrating the victory of wisdom, perseverance, and dedication. May you overcome every challenge and achieve your highest goals!",
                hiTitleTemplate = "दशहरे की हार्दिक शुभकामनाएँ{name}!",
                hiMessageTemplate = "बुराई पर अच्छाई और अज्ञान पर ज्ञान की विजय का यह पर्व आपके जीवन में नई प्रेरणा और दृढ़ संकल्प लेकर आए!"
            ),
            FestivalGreeting(
                titleTemplate = "Shubh Vijayadashami{name}!",
                messageTemplate = "May focus and hard work lead you to continuous success. Enjoy the festivities and keep striving forward with confidence!",
                hiTitleTemplate = "शुभ विजयादशमी{name}!",
                hiMessageTemplate = "कड़ी मेहनत और एकाग्रता से आप हर कठिनाई पर विजय प्राप्त करें। विजयादशमी की हार्दिक बधाई!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Dussehra{name}!",
                messageTemplate = "A celebration of strength, focus, and triumph. Wishing you great confidence in everything you set out to achieve!",
                hiTitleTemplate = "दशहरा पर्व की शुभकामनाएँ{name}!",
                hiMessageTemplate = "आत्मविश्वास, संकल्प और अनुशासन से अपने लक्ष्यों को हासिल करें और निरंतर आगे बढ़ें!"
            )
        )
    ),

    NEW_YEAR(
        key = "new_year",
        displayName = "New Year's Day",
        defaultTitle = "Happy New Year{name}!",
        countryCodes = listOf("ALL"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy New Year{name}!",
                messageTemplate = "A brand new year with fresh opportunities and goals. Wishing you consistency, focus, and great success in the year ahead!",
                hiTitleTemplate = "नव वर्ष की हार्दिक शुभकामनाएँ{name}!",
                hiMessageTemplate = "यह नया साल आपके लिए नई उम्मीदें, अटूट संकल्प और शानदार उपलब्धियाँ लेकर आए। निरंतर आगे बढ़ते रहें!"
            ),
            FestivalGreeting(
                titleTemplate = "Welcome to a New Year{name}!",
                messageTemplate = "Every day is a step toward your dreams. May this year be your most rewarding, focused, and accomplished year yet!",
                hiTitleTemplate = "नूतन वर्ष की शुभकामनाएँ{name}!",
                hiMessageTemplate = "हर दिन अपने सपनों को साकार करने का एक नया अवसर है। यह वर्ष आपके अध्ययन और करियर में मील का पत्थर साबित हो!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy New Year{name}!",
                messageTemplate = "Make this year count with strong habits and clear goals. Wishing you continuous growth and achievement!",
                hiTitleTemplate = "नव वर्ष मंगलमय हो{name}!",
                hiMessageTemplate = "मजबूत आदतों और स्पष्ट लक्ष्यों के साथ इस वर्ष को खास बनाएँ। सफलता और प्रगति आपके कदम चूमे!"
            )
        )
    ),

    REPUBLIC_DAY(
        key = "republic_day",
        displayName = "Republic Day",
        defaultTitle = "Happy Republic Day{name}!",
        countryCodes = listOf("IN"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Republic Day{name}!",
                messageTemplate = "Celebrating the values, progress, and unity of our nation. Wishing you pride, inspiration, and excellence in your learning!",
                hiTitleTemplate = "गणतंत्र दिवस की हार्दिक शुभकामनाएँ{name}!",
                hiMessageTemplate = "राष्ट्र के गौरव, एकता और प्रगति के इस पावन पर्व पर ज्ञान और निष्ठा के साथ अपने लक्ष्यों की ओर अग्रसर रहें!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Republic Day{name}!",
                messageTemplate = "Knowledge and dedication shape a better tomorrow. Keep learning and striving for excellence!",
                hiTitleTemplate = "गणतंत्र दिवस की बधाई{name}!",
                hiMessageTemplate = "सच्ची लगन और ज्ञान ही राष्ट्र निर्माण की शक्ति है। निरंतर अध्ययन करें और उत्कृष्टता हासिल करें!"
            )
        )
    ),

    INDEPENDENCE_DAY(
        key = "independence_day",
        displayName = "Independence Day",
        defaultTitle = "Happy Independence Day{name}!",
        countryCodes = listOf("IN"),
        greetings = listOf(
            FestivalGreeting(
                titleTemplate = "Happy Independence Day{name}!",
                messageTemplate = "Celebrating the spirit of freedom, vision, and growth. Wishing you great inspiration and success on your path ahead!",
                hiTitleTemplate = "स्वतंत्रता दिवस की हार्दिक शुभकामनाएँ{name}!",
                hiMessageTemplate = "स्वाधीनता, एकता और प्रगति के इस पावन पर्व पर अपने लक्ष्यों को हासिल करने का नया संकल्प लें!"
            ),
            FestivalGreeting(
                titleTemplate = "Happy Independence Day{name}!",
                messageTemplate = "Knowledge brings true freedom and strength. Keep learning, growing, and reaching for your goals!",
                hiTitleTemplate = "स्वतंत्रता दिवस की बधाई{name}!",
                hiMessageTemplate = "ज्ञान और मेहनत ही हमें सच्ची आत्मनिर्भरता और शक्ति देते हैं। अपने सपनों की ऊँचाइयों को छुएँ!"
            )
        )
    );

    fun isApplicableForCountry(countryIso: String?): Boolean {
        if (countryCodes.contains("ALL")) return true
        if (countryIso.isNullOrBlank() || countryIso.equals("ALL", ignoreCase = true) || countryIso.equals("GLOBAL", ignoreCase = true)) return true
        return countryCodes.any { it.equals(countryIso.trim(), ignoreCase = true) }
    }

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
        "2024-10-12" to FestivalType.DUSSEHRA,
        "2024-10-31" to FestivalType.DIWALI,
        "2024-11-01" to FestivalType.DIWALI,

        // --- 2025 ---
        "2025-03-14" to FestivalType.HOLI,
        "2025-03-31" to FestivalType.EID_UL_FITR,
        "2025-06-07" to FestivalType.EID_UL_ADHA,
        "2025-10-02" to FestivalType.DUSSEHRA,
        "2025-10-20" to FestivalType.DIWALI,

        // --- 2026 ---
        "2026-03-04" to FestivalType.HOLI,
        "2026-03-20" to FestivalType.EID_UL_FITR,
        "2026-05-27" to FestivalType.EID_UL_ADHA,
        "2026-10-20" to FestivalType.DUSSEHRA,
        "2026-11-08" to FestivalType.DIWALI,

        // --- 2027 ---
        "2027-03-10" to FestivalType.EID_UL_FITR,
        "2027-03-23" to FestivalType.HOLI,
        "2027-05-16" to FestivalType.EID_UL_ADHA,
        "2027-10-09" to FestivalType.DUSSEHRA,
        "2027-10-29" to FestivalType.DIWALI,

        // --- 2028 ---
        "2028-02-27" to FestivalType.EID_UL_FITR,
        "2028-03-11" to FestivalType.HOLI,
        "2028-05-05" to FestivalType.EID_UL_ADHA,
        "2028-09-27" to FestivalType.DUSSEHRA,
        "2028-10-17" to FestivalType.DIWALI,

        // --- 2029 ---
        "2029-02-15" to FestivalType.EID_UL_FITR,
        "2029-03-01" to FestivalType.HOLI,
        "2029-04-24" to FestivalType.EID_UL_ADHA,
        "2029-10-16" to FestivalType.DUSSEHRA,
        "2029-11-05" to FestivalType.DIWALI,

        // --- 2030 ---
        "2030-02-05" to FestivalType.EID_UL_FITR,
        "2030-03-20" to FestivalType.HOLI,
        "2030-04-13" to FestivalType.EID_UL_ADHA,
        "2030-10-06" to FestivalType.DUSSEHRA,
        "2030-10-26" to FestivalType.DIWALI,

        // --- 2031 ---
        "2031-01-24" to FestivalType.EID_UL_FITR,
        "2031-03-09" to FestivalType.HOLI,
        "2031-04-02" to FestivalType.EID_UL_ADHA,
        "2031-10-25" to FestivalType.DUSSEHRA,
        "2031-11-14" to FestivalType.DIWALI,

        // --- 2032 ---
        "2032-01-14" to FestivalType.EID_UL_FITR,
        "2032-02-26" to FestivalType.HOLI,
        "2032-03-22" to FestivalType.EID_UL_ADHA,
        "2032-10-14" to FestivalType.DUSSEHRA,
        "2032-11-02" to FestivalType.DIWALI,

        // --- 2033 ---
        "2033-01-02" to FestivalType.EID_UL_FITR,
        "2033-03-11" to FestivalType.EID_UL_ADHA,
        "2033-03-16" to FestivalType.HOLI,
        "2033-10-03" to FestivalType.DUSSEHRA,
        "2033-10-22" to FestivalType.DIWALI,
        "2033-12-23" to FestivalType.EID_UL_FITR,

        // --- 2034 ---
        "2034-03-01" to FestivalType.EID_UL_ADHA,
        "2034-03-06" to FestivalType.HOLI,
        "2034-10-22" to FestivalType.DUSSEHRA,
        "2034-11-10" to FestivalType.DIWALI,
        "2034-12-12" to FestivalType.EID_UL_FITR,

        // --- 2035 ---
        "2035-02-18" to FestivalType.EID_UL_ADHA,
        "2035-03-24" to FestivalType.HOLI,
        "2035-10-11" to FestivalType.DUSSEHRA,
        "2035-10-30" to FestivalType.DIWALI,
        "2035-12-01" to FestivalType.EID_UL_FITR,

        // --- 2036 ---
        "2036-02-07" to FestivalType.EID_UL_ADHA,
        "2036-03-12" to FestivalType.HOLI,
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

        // Filter festival by detected or user-selected country
        val userCountry = CountryDetector.detectCountryIso(context)
        if (!isTest && !festival.isApplicableForCountry(userCountry)) {
            return false
        }

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
        val userLanguage = CountryDetector.detectLanguageCode(context)
        val (title, message) = greetingTemplate.format(firstName, userLanguage)

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
