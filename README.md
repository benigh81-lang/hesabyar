# خرج‌یار — Android app prototype

Personal, offline-first SMS expense-review prototype. The app requests SMS access only after tapping its scan button, shows likely purchase messages for explicit review, and keeps approved entries locally using Android SharedPreferences. No SMS is transmitted to a server.

## Features
- Persian RTL interface and dark Material UI
- User-triggered SMS scan, capped at 250 recent messages
- First-pass purchase parser with Persian/Arabic digit normalization
- Approve/reject review cards
- Locally persisted approved items (simple JSON in SharedPreferences)
- GitHub Actions workflow builds a debug APK on pushes to `main` and manual runs

## Build locally
Android Studio, JDK 17, Android SDK 35. Open project and run Gradle sync, or run `gradle assembleDebug`.

## Build in GitHub Actions
Upload this project to a separate GitHub repository. The workflow `.github/workflows/build-apk.yml` will build and attach `app-debug.apk` as an Actions artifact. From GitHub: Actions → Build Kharjyar APK → run workflow (or wait for push) → open successful run → download `kharjyar-debug-apk` artifact.

## Limitations
Prototype only: parser is not universal and approved list is not yet a full ledger/category/report system. The package targets API 28 for sideload-oriented SMS access; Google Play SMS permission policy may prohibit distribution. Android permission and device compatibility vary.
