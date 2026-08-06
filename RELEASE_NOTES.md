# StudyTimer 1.3.2 (versionCode 7)

## Fixes
- Fixed a crash (`ClassCastException`) that occurred when the app read an old stored preference as an integer — could happen when the auto-update check ran after the app had been opened 5 times. Preference reads are now type-safe.

## What's New (from 1.3.0/1.3.1)
- **Background Battery Permission** — the app asks you to run in the background so long study sessions aren't paused by the system; re-prompted every few app opens until allowed, with a clearer permission dialog.
- **Smarter Update Checks** — the auto-update check now runs every 5th time the app is opened, including when you just minimize and return.
- **Pomodoro Progress Ring** — a circular progress ring on the Countdown screen that drains as your session counts down; break fills it; theme-aware colors.
- **Streak Tied to Daily Goal** — new toggle: streaks count against your own daily goal instead of the fixed 45-minute threshold.
- **Improved Focus Pattern** — 7d/30d toggle and tap-any-block detail.
- Smoother pause animation, Activity Result API migration for backups, dead-code cleanup, lint-clean build.

## Compatibility
- Android 9 (API 28) through Android 16 (API 36)
- Signed with the JAI Labs release key

## Installation
Install `StudyTimer-release.apk` (v1.3.2, ~3.0 MB). Updating preserves all study logs, streaks, and settings.
