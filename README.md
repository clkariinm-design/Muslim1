# Nuur Qur’an (`com.nuur.muslim`)

Modern **Islamic Luxury** (Dark Emerald & Gold) Android Qur’an application built with **Kotlin + Jetpack Compose + Material 3**, MVVM architecture, Room local database, DataStore Preferences, and Media3 ExoPlayer.

## Core Features
- **Complete Verified Qur’an (Offline-First):** 114 surahs and 6,236 ayahs (`quran_bundle.bin`) verified on startup with SHA-256 (`data_manifest.json`).
- **Uthmani Arabic & Tajweed:** Verified `quran-uthmani` text with optional color-coded `quran-tajweed` overlay (17 rules) and rule guide.
- **Translations & Tafsir:** Bundled Somali (*Cabduh*) and English (*Saheeh International*), bundled Arabic *Tafsir Al-Muyassar*, and optional HTTPS-cached *Tafsir Ibn Kathir (Abridged)* via Quran.com API v4.
- **Audio Recitation & Offline Downloads:** Media3 ExoPlayer (`MediaSessionService`) background playback, Ayah/Surah repeat, reciter switching, atomic `.tmp` -> `.mp3` offline surah downloads, and download cancellation.
- **Study Tools:** Full-text & `surah:ayah` search, local bookmarks, personal notes, last-read tracking, Khatam progress, and reading plans.
- **Worship Tools:** Astronomically calculated prayer times (MWL, Umm al-Qura, Egypt, Karachi, ISNA) with local verification notice, curated cities + optional `ACCESS_COARSE_LOCATION`, smoothed Qibla compass, Hijri calendar, Qur’anic Rabbana duas, and 34 verified Morning & Evening Adhkar.
- **Multilingual & Adaptive UI:** Somali (default), Arabic (RTL), and English localizations; Dark Emerald, Parchment Light, and System themes; `NavigationBar` on compact screens and `NavigationRail` on expanded screens.

## Verified Data Sources & Licenses
- **Uthmani Arabic, Tajweed, Somali (`so.abduh`), English (`en.sahih`), Tafsir (`ar.muyassar`):** AlQuran Cloud / Tanzil datasets (`CC BY 3.0` / Tanzil attribution).
- **Morning & Evening Adhkar:** `Seen-Arabic/Morning-And-Evening-Adhkar-DB` (`MIT License`, Arabic & English).
- **Audio Streams:** Islamic Network CDN (`https://cdn.islamic.network/quran/audio/...`).
- **Online Tafsir (Optional):** Quran.com API v4 (`https://api.quran.com/api/v4/tafsirs/169/...`).

## Privacy & Security Notes
- Bookmarks, notes, reading plans, and settings stay local in Room & DataStore.
- `android:usesCleartextTraffic="false"` enforces HTTPS for network calls.
- `ACCESS_COARSE_LOCATION` is strictly optional; manual city selection works without permissions.
- `POST_NOTIFICATIONS` is requested optionally at runtime on Android 13+ when audio playback starts.
- **Draft Privacy Policy Notice:** The privacy policy in Settings is currently labeled as a **DRAFT**. Before Play Store publication, add an official support email and hosted privacy policy URL.

## Build, Test, and APK Generation
Run with a system Gradle installation (Gradle 9.3.1+ compatible with `gradle/wrapper/gradle-wrapper.properties`) or Android Studio:

```bash
gradle :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

- **Debug APK Output:** `app/build/outputs/apk/debug/app-debug.apk`
- **Release Builds:** Release builds require a local upload keystore (`my-upload-key.jks` or `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) and enabling `isMinifyEnabled = true` in `app/build.gradle.kts` if code shrinking is desired prior to Play Store release.


## Legal & Google Play Release Documents
- [Proprietary software notice](LICENSE) — applies only to original project material; third-party content remains under its own licence.
- [Privacy Policy draft](PRIVACY_POLICY.md) — must be completed, audited, and hosted at a public HTTPS URL before release.
- [Third-party notices](THIRD_PARTY_NOTICES.md) — source and licence verification checklist for Qur’an datasets, translations, tafsir, adhkar, audio, and dependencies.
- [Google Play release checklist](PLAY_STORE_RELEASE_CHECKLIST.md) — privacy, signing, testing, store listing, and release tasks.

**Release warning:** These documents are preparation drafts, not legal certification or proof of compliance. Replace all placeholders and verify actual shipped-app behavior and all content licences before publishing or selling the app.
