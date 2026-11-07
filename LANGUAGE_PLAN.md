# StudyTimer Language Localization & Multi-Language Architecture Plan

## Executive Overview
This document outlines the step-by-step, phased engineering roadmap to transform StudyTimer into a fully localized, multi-language application.

Currently, user-facing text across StudyTimer is constructed programmatically in Kotlin UI builders (`FocusPanelBuilder`, `SettingsPanelBuilder`, `PlannerPanelBuilder`, `StatsPanelBuilder`, etc.) using hardcoded string literals. This plan provides a clean, backward-compatible migration path that decouples UI logic from display text without breaking existing themes, glassmorphism styling, or animations.

---

## Architectural Principles
1. **Zero Layout Distortion:** Localized text length varies across languages (e.g. German text is ~25–35% longer than English). All UI text containers must support flexible sizing (`wrap_content`), single-line truncations with `TextUtils.TruncateAt.END`, or multi-line wraps.
2. **Backward & Forward Compatibility:** Full support from Android 8.0 (API 26) through Android 15+ (API 35), utilizing Android 13's official per-app language API (`AppCompatDelegate.setApplicationLocales`) with automatic fallback to `createConfigurationContext`.
3. **Graceful Fallbacks:** If a specific phrase or token is not yet translated in a target language, Android will automatically fall back to the default English string in `res/values/strings.xml`.

---

## Phased Execution Roadmap

### Phase 1: Dynamic Locale Engine & Lifecycle Foundation
*Objective: Build the underlying engine that changes the app's running locale instantly without requiring an app reinstall or device reboot.*

1. **Android 13+ Per-App Language Standard:**
   - Integrate `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))` for Android 13 (API 33)+.
   - Declare `localeConfig` in `res/xml/locales_config.xml` to seamlessly integrate with Android system settings.
2. **Android 8–12 Context Wrapper (`attachBaseContext`):**
   - Update `attachBaseContext` in `MainActivity.kt`, `SplashActivity.kt`, and `StudyTimerApp.kt`:
     ```kotlin
     override fun attachBaseContext(newBase: Context) {
         val langCode = CountryDetector.detectLanguageCode(newBase)
         val locale = if (langCode == "auto") CountryDetector.getPrimaryLocale() else Locale(langCode)
         val config = Configuration(newBase.resources.configuration)
         config.setLocale(locale)
         config.fontScale = config.fontScale.coerceIn(0.85f, 1.05f)
         super.attachBaseContext(newBase.createConfigurationContext(config))
     }
     ```
3. **Smooth Live Re-creation:**
   - When the user selects a new language in Settings, persist `user_override_language_code` in `StudyTimerPrefs` and invoke `host.recreate()` with smooth transition animations.

---

### Phase 2: String Extraction & Resource Modularization
*Objective: Extract ~800+ hardcoded string literals into standard `res/values/strings.xml` using a structured naming convention.*

#### Naming Conventions
* Actions & Buttons: `action_pause`, `action_finish`, `action_cancel`, `action_save`
* Panel Headers & Tabs: `title_timer`, `title_settings`, `tab_overview`, `tab_history`
* Settings Rows & Subtitles: `setting_theme_title`, `setting_theme_desc`, `setting_country_title`
* Notifications & Toasts: `toast_session_saved`, `notif_goal_achieved_title`
* Dialog Messages: `dialog_delete_subject_msg`, `dialog_confirm_exit`

#### Extraction Batches (by Panel)
1. **Batch 2.1 — Global Navigation & Focus Timer:**
   - Bottom navigation tab labels (`Focus`, `Planner`, `Stats`, `Community`, `Settings`).
   - Focus timer controls (`Pause`, `Finish`, `Break`, `Double-tap to pause`, `Portrait / Landscape`).
2. **Batch 2.2 — Settings Hub & Student Profile:**
   - Settings category headers and descriptions.
   - Profile editor fields (`Display Name`, `Focus Track`, `Bio & Motto`, `Country & Region`).
3. **Batch 2.3 — Planner, Goals & Timetable:**
   - Goal setup dialogs, reminders, class timetable, subject editor.
4. **Batch 2.4 — Insights, Heatmap & Leaderboard:**
   - Trend comparisons (`vs yesterday`), streak banners, filter pills (`7 Days`, `30 Days`, `All Time`).

---

### Phase 3: Translation Pipeline & Target Languages
*Objective: Generate production-quality localized string files for target markets.*

#### Language Tiers
* **Tier 1 (High Priority / Immediate Launch):**
  - **Hindi (`hi`)**: `res/values-hi/strings.xml` (Primary domestic user base)
  - **Spanish (`es`)**: `res/values-es/strings.xml`
  - **French (`fr`)**: `res/values-fr/strings.xml`
  - **German (`de`)**: `res/values-de/strings.xml`
* **Tier 2 (Indian Regional Languages):**
  - Bengali (`bn`), Marathi (`mr`), Telugu (`te`), Tamil (`ta`), Gujarati (`gu`), Kannada (`kn`), Punjabi (`pa`), Urdu (`ur`).
* **Tier 3 (Global Expansion):**
  - Portuguese (`pt`), Russian (`ru`), Japanese (`ja`), Korean (`ko`), Arabic (`ar`), Indonesian (`id`), Vietnamese (`vi`), Simplified Chinese (`zh`).

#### Resource File Architecture
```
app/src/main/res/
  ├── values/strings.xml        (English - Source of Truth)
  ├── values-hi/strings.xml     (Hindi)
  ├── values-es/strings.xml     (Spanish)
  ├── values-fr/strings.xml     (French)
  ├── values-de/strings.xml     (German)
  └── values-<lang>/strings.xml (...)
```

---

### Phase 4: UI Quality Assurance & RTL Support
*Objective: Guarantee that fonts, button sizes, and alignments remain pixel-perfect across all languages.*

1. **Text Container Flexibility:**
   - Replace fixed-width `TextView` elements with dynamic wrap rules or `maxLines` + `ellipsize`.
2. **Right-to-Left (RTL) Layouts (Arabic & Urdu):**
   - Ensure `android:supportsRtl="true"` is declared in `AndroidManifest.xml`.
   - Use `marginStart`/`marginEnd` and `paddingStart`/`paddingEnd` instead of `left`/`right`.
3. **Font Rendering:**
   - Ensure custom fonts selected in `FontStyleManager` support non-Latin glyphs (Devanagari, Cyrillic, Kanji, Arabic script) or fall back gracefully to Android's system font.

---

### Phase 5: Re-enabling the User Settings UI
*Objective: Reactivate the in-app Language Selection picker for users.*

1. **Re-activate Language Row:**
   - In `SettingsPanelBuilder.kt` (under `AppSettingsTab.THEME`), uncomment the `langRow` and its divider.
2. **Activate Dialog & Testing:**
   - Reconnect `showLanguageSelectionDialog` to update preferences, notify `AppAnalytics.onRegionUpdated`, and trigger `host.recreate()`.
3. **Test Matrix:**
   - Verify immediate translation update across all panels upon selection.
   - Verify system default detection ("Automatic (Recommended)").
   - Verify persistent locale upon app cold-boot and phone restart.

