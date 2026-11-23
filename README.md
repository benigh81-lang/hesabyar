# خرج‌یار (Kharjyar) — Android prototype

An initial Kotlin/Compose prototype for reviewing likely purchase SMS messages. **Nothing is automatically booked or entered into a ledger:** SMS items are candidates and require an explicit approve/reject action.

## Features in this starter
- Persian RTL screen
- Explicit Android SMS permission request
- Scans up to 250 recent SMS messages after the user taps the scan button
- Conservative purchase keyword parser and Persian/Arabic digit normalization
- Review cards with approve/reject controls

## Build
Open this folder in Android Studio, use JDK 17, install Android SDK 35, then sync Gradle and run the `app` configuration on an Android phone/emulator. This starter does not include a Gradle wrapper yet.

## Important limitations of this initial prototype
- Approval is only held in in-memory UI state; persistent Room storage is the next milestone.
- The amount parser is intentionally conservative and needs real sample messages to refine. It is not a reliable universal bank parser yet.
- The parser does not infer currency/unit or category.
- SMS permissions and use/distribution policies vary by Android version and store. This is a sideload-oriented personal prototype, not a Play Store-ready app.
- No SMS content is uploaded or sent to a server.
