package com.madeby.JAI

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.telephony.TelephonyManager
import java.util.Locale
import java.util.TimeZone

data class UserRegionInfo(
    val countryIso: String,       // ISO 3166-1 alpha-2, e.g. "IN", "US", "GB"
    val countryName: String,      // e.g. "India", "United States"
    val languageCode: String,     // ISO 639-1, e.g. "en", "hi", "es"
    val languageName: String,     // e.g. "English", "Hindi"
    val localeTag: String         // e.g. "en-IN", "en-US"
)

/**
 * Privacy-first, 100% permission-free country and language detector.
 *
 * Fully compliant with Google Play Store Policies:
 * - Does NOT request ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION.
 * - Uses telephony network/SIM country ISO and device system Locale.
 * - Failsafe language detection that never crashes and always returns valid ISO codes.
 */
object CountryDetector {

    private const val PREFS_NAME = "StudyTimerPrefs"
    const val KEY_DETECTED_COUNTRY_ISO = "detected_country_iso"
    const val KEY_USER_OVERRIDE_COUNTRY_ISO = "user_override_country_iso"
    const val KEY_DETECTED_LANGUAGE_CODE = "detected_language_code"
    const val KEY_USER_OVERRIDE_LANGUAGE_CODE = "user_override_language_code"

    private val TIMEZONE_COUNTRY_HINTS = mapOf(
        "Asia/Kolkata" to "IN",
        "Asia/Calcutta" to "IN",
        "America/New_York" to "US",
        "America/Chicago" to "US",
        "America/Denver" to "US",
        "America/Los_Angeles" to "US",
        "America/Anchorage" to "US",
        "America/Toronto" to "CA",
        "America/Vancouver" to "CA",
        "Europe/London" to "GB",
        "Europe/Berlin" to "DE",
        "Europe/Paris" to "FR",
        "Europe/Rome" to "IT",
        "Europe/Madrid" to "ES",
        "Asia/Dubai" to "AE",
        "Asia/Riyadh" to "SA",
        "Asia/Karachi" to "PK",
        "Asia/Dhaka" to "BD",
        "Asia/Jakarta" to "ID",
        "Asia/Singapore" to "SG",
        "Asia/Tokyo" to "JP",
        "Asia/Seoul" to "KR",
        "Australia/Sydney" to "AU",
        "Australia/Melbourne" to "AU"
    )

    /**
     * Resolves the primary Locale with backward and forward Android version compatibility.
     * Guaranteed never to fail or return null.
     */
    fun getPrimaryLocale(): Locale {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val list = LocaleList.getDefault()
                if (!list.isEmpty) list.get(0) else Locale.getDefault()
            } else {
                Locale.getDefault()
            } ?: Locale.US
        } catch (_: Exception) {
            Locale.US
        }
    }

    /**
     * Detects language code (e.g. "en", "hi", "es").
     * Checks user manual override first, then falls back safely to device system Locale.
     * Guaranteed never to fail, never return blank or "und", always fallback safely to "en".
     */
    fun detectLanguageCode(context: Context? = null): String {
        try {
            if (context != null) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val override = prefs.getString(KEY_USER_OVERRIDE_LANGUAGE_CODE, null)?.trim()?.lowercase(Locale.US)
                if (!override.isNullOrBlank() && override != "auto" && override.length >= 2) {
                    return override
                }
            }
            val locale = getPrimaryLocale()
            val rawLang = locale.language.lowercase(Locale.US).trim()
            if (rawLang.isNotBlank() && rawLang != "und" && rawLang.length >= 2) {
                return rawLang
            }
        } catch (_: Exception) {}
        return "en"
    }

    data class SupportedLanguage(
        val code: String,
        val nativeName: String,
        val englishName: String
    ) {
        val displayName: String
            get() = if (nativeName.equals(englishName, ignoreCase = true)) englishName else "$nativeName ($englishName)"
    }

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("en", "English", "English"),
        SupportedLanguage("hi", "हिन्दी", "Hindi"),
        SupportedLanguage("es", "Español", "Spanish"),
        SupportedLanguage("fr", "Français", "French"),
        SupportedLanguage("de", "Deutsch", "German"),
        SupportedLanguage("ar", "العربية", "Arabic"),
        SupportedLanguage("bn", "বাংলা", "Bengali"),
        SupportedLanguage("mr", "मराठी", "Marathi"),
        SupportedLanguage("te", "తెలుగు", "Telugu"),
        SupportedLanguage("ta", "தமிழ்", "Tamil"),
        SupportedLanguage("gu", "ગુજરાતી", "Gujarati"),
        SupportedLanguage("kn", "ಕನ್ನಡ", "Kannada"),
        SupportedLanguage("ml", "മലയാളം", "Malayalam"),
        SupportedLanguage("pa", "ਪੰਜਾਬੀ", "Punjabi"),
        SupportedLanguage("ur", "اردو", "Urdu"),
        SupportedLanguage("pt", "Português", "Portuguese"),
        SupportedLanguage("ru", "Русский", "Russian"),
        SupportedLanguage("ja", "日本語", "Japanese"),
        SupportedLanguage("ko", "한국어", "Korean"),
        SupportedLanguage("zh", "中文", "Chinese"),
        SupportedLanguage("id", "Bahasa Indonesia", "Indonesian"),
        SupportedLanguage("it", "Italiano", "Italian"),
        SupportedLanguage("nl", "Nederlands", "Dutch"),
        SupportedLanguage("pl", "Polski", "Polish"),
        SupportedLanguage("tr", "Türkçe", "Turkish"),
        SupportedLanguage("vi", "Tiếng Việt", "Vietnamese"),
        SupportedLanguage("th", "ไทย", "Thai"),
        SupportedLanguage("fa", "فارسی", "Persian"),
        SupportedLanguage("uk", "Українська", "Ukrainian")
    )

    /**
     * Detects human-readable language name (e.g. "English", "हिन्दी (Hindi)").
     */
    fun detectLanguageName(context: Context? = null): String {
        val code = detectLanguageCode(context)
        val matched = SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
        if (matched != null) return matched.displayName
        return try {
            val loc = Locale(code)
            val native = loc.getDisplayLanguage(loc).trim().replaceFirstChar { it.uppercase() }
            val en = loc.getDisplayLanguage(Locale.US).trim()
            if (native.isNotBlank() && !native.equals(en, ignoreCase = true)) {
                "$native ($en)"
            } else if (en.isNotBlank()) {
                en
            } else {
                "English"
            }
        } catch (_: Exception) {
            "English"
        }
    }

    fun getSystemLanguageName(): String {
        return try {
            val loc = getPrimaryLocale()
            val matched = SUPPORTED_LANGUAGES.find { it.code.equals(loc.language, ignoreCase = true) }
            if (matched != null) return matched.displayName
            val native = loc.getDisplayLanguage(loc).trim().replaceFirstChar { it.uppercase() }
            val en = loc.getDisplayLanguage(Locale.US).trim()
            if (native.isNotBlank() && !native.equals(en, ignoreCase = true)) {
                "$native ($en)"
            } else if (en.isNotBlank()) {
                en
            } else {
                "English"
            }
        } catch (_: Exception) {
            "English"
        }
    }

    fun setUserLanguageOverride(context: Context, langCode: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (langCode.isNullOrBlank() || langCode.equals("AUTO", ignoreCase = true)) {
            prefs.edit().remove(KEY_USER_OVERRIDE_LANGUAGE_CODE).apply()
        } else {
            prefs.edit().putString(KEY_USER_OVERRIDE_LANGUAGE_CODE, langCode.lowercase(Locale.US).trim()).apply()
        }
    }

    fun isUserLanguageOverrideActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_USER_OVERRIDE_LANGUAGE_CODE, null).isNullOrBlank()
    }

    /**
     * Detects country ISO code using the permissionless 4-tier waterfall:
     * 1. User manual override (if set in settings)
     * 2. TelephonyManager SIM Country ISO (cellular SIM)
     * 3. TelephonyManager Network Country ISO (connected cellular tower)
     * 4. System Locale country
     * 5. TimeZone region mapping
     * Fallback: "IN"
     */
    fun detectCountryIso(context: Context): String {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // 1. User override takes top priority
            val userOverride = prefs.getString(KEY_USER_OVERRIDE_COUNTRY_ISO, null)?.trim()?.uppercase(Locale.US)
            if (!userOverride.isNullOrBlank() && userOverride.length == 2) {
                return userOverride
            }

            // 2. SIM Card ISO
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = tm?.simCountryIso?.trim()?.uppercase(Locale.US)
            if (!simCountry.isNullOrBlank() && simCountry.length == 2) {
                cacheDetectedCountry(prefs, simCountry)
                return simCountry
            }

            // 3. Network Country ISO
            val netCountry = tm?.networkCountryIso?.trim()?.uppercase(Locale.US)
            if (!netCountry.isNullOrBlank() && netCountry.length == 2) {
                cacheDetectedCountry(prefs, netCountry)
                return netCountry
            }

            // 4. Device Locale Country
            val locale = getPrimaryLocale()
            val localeCountry = locale.country?.trim()?.uppercase(Locale.US)
            if (!localeCountry.isNullOrBlank() && localeCountry.length == 2) {
                cacheDetectedCountry(prefs, localeCountry)
                return localeCountry
            }

            // 5. Timezone hint
            val tzId = TimeZone.getDefault().id
            val tzCountry = TIMEZONE_COUNTRY_HINTS[tzId]
            if (!tzCountry.isNullOrBlank()) {
                cacheDetectedCountry(prefs, tzCountry)
                return tzCountry
            }
        } catch (_: Exception) {}

        return "IN"
    }

    private fun cacheDetectedCountry(prefs: android.content.SharedPreferences, iso: String) {
        if (prefs.getString(KEY_DETECTED_COUNTRY_ISO, null) != iso) {
            prefs.edit().putString(KEY_DETECTED_COUNTRY_ISO, iso).apply()
        }
    }

    /**
     * Resolves human-readable country name from ISO code.
     */
    fun getCountryName(countryIso: String): String {
        return try {
            val loc = Locale("", countryIso.uppercase(Locale.US))
            val name = loc.getDisplayCountry(Locale.US).trim()
            if (name.isNotBlank()) name else countryIso
        } catch (_: Exception) {
            countryIso
        }
    }

    /**
     * Sets manual country override (e.g. from Settings).
     * Pass null or empty string to reset to automatic detection.
     */
    fun setUserCountryOverride(context: Context, iso: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (iso.isNullOrBlank() || iso.equals("AUTO", ignoreCase = true)) {
            prefs.edit().remove(KEY_USER_OVERRIDE_COUNTRY_ISO).apply()
        } else {
            prefs.edit().putString(KEY_USER_OVERRIDE_COUNTRY_ISO, iso.uppercase(Locale.US).trim()).apply()
        }
    }

    fun isUserOverrideActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_USER_OVERRIDE_COUNTRY_ISO, null).isNullOrBlank()
    }

    /**
     * Returns full aggregated region info.
     */
    fun getRegionInfo(context: Context): UserRegionInfo {
        val iso = detectCountryIso(context)
        val countryName = getCountryName(iso)
        val langCode = detectLanguageCode(context)
        val langName = detectLanguageName(context)
        val localeTag = "$langCode-$iso"

        return UserRegionInfo(
            countryIso = iso,
            countryName = countryName,
            languageCode = langCode,
            languageName = langName,
            localeTag = localeTag
        )
    }
}

