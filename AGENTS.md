# Project Conventions

## Release builds
- After any `assembleRelease`, copy the fresh APK from `app/build/outputs/apk/release/app-release.apk` to the project root as `StudyTimer-release.apk` (overwrite the old one).
- Also update `RELEASE_NOTES.md` to match the new version.
- Build command: `./gradlew assembleRelease` (signing uses `app/keystore.p12` + passwords from env or `local.properties`).
