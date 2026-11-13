# Nuur Qur’an — Privacy Policy (Publication Draft)

**Effective date:** [INSERT PUBLICATION DATE]  
**App:** Nuur Qur’an  
**Android package:** `com.nuur.muslim`  
**Developer / data controller:** [INSERT LEGAL NAME OR BUSINESS NAME]  
**Privacy contact:** [INSERT MONITORED PRIVACY EMAIL]  
**Public policy URL:** [INSERT HTTPS URL]

> **Publication blocker:** This is a project-specific draft based on the current README and Android manifest. It is not ready to publish until every bracketed field is completed and the developer has checked the actual source code, SDKs, network requests, backup behavior, and Google Play Data safety answers. Do not publish this draft with placeholders.

## 1. Scope

This policy explains how the Nuur Qur’an Android application may process information when you use it. The app provides Qur’an reading and study tools, translations and tafsir, audio recitation, bookmarks, notes, reading progress, prayer-time calculations, Qibla direction, Adhkar, Tasbih, and related worship features.

## 2. Information stored on your device

According to the project documentation, the app stores app preferences and user-created study information locally using Android DataStore and Room. This may include:

- Bookmarks, personal notes, reading plans, last-read position, and reading progress.
- App preferences and selected settings.
- Downloaded audio or other content saved for offline use.
- Tasbih or worship progress, if persisted by the installed version.

The developer must verify the exact database entities, files, retention rules, and whether any of this information is included in Android device backup before publication. Do not enter sensitive personal information into notes unless you accept the risks of storing it on your device.

## 3. Location

The Android manifest declares the optional `ACCESS_COARSE_LOCATION` permission. The app documentation says manual city selection is available without granting location permission.

If you grant location access and the app uses it, approximate location may be processed on the device to select a location for prayer-time or Qibla calculations. You can deny or revoke location permission in Android settings and use manual city selection where available.

**Developer verification required:** Confirm whether coordinates or location-derived data are ever transmitted to a server or third party. This draft does not claim that location is never shared until network behavior has been audited.

## 4. Internet access and third-party services

The app declares internet and network-state permissions. Its documentation identifies the following online services:

- **Quran.com API v4:** optional online access to Tafsir Ibn Kathir, with HTTPS caching described in the project documentation.
- **Islamic Network CDN:** audio recitation streams and downloads.
- **AlQuran Cloud / Tanzil datasets:** Qur’an text and related datasets, as identified by the project documentation.

When your device connects to these services, the service provider may receive technical connection information such as your IP address, request details, and standard server logs under that provider's own privacy policy. The developer must verify the exact endpoints, request parameters, caching, and whether any identifiers are sent.

The app documentation describes local bookmarks, notes, reading plans, and settings. It does not document a developer-operated account system or developer backend; the developer must verify that this remains accurate in the shipped build.

## 5. Notifications, alarms, and audio

The app declares permissions related to notifications, foreground media playback, boot-completed events, exact alarms, and wake locks. These support audio playback and prayer reminders/alarm scheduling as described by the project.

Notification permission is requested at runtime on Android 13 and later, according to the project documentation. You can manage notification and alarm permissions through Android settings. Audio playback may use Android's media playback service and system media controls.

## 6. Sharing and sale of information

The developer must complete a code and SDK audit before making any statement about sale, sharing, advertising, analytics, or tracking. This draft makes no unsupported promise that the app collects no data, shares no data, or contains no third-party SDK data collection.

**Before publication, state accurately whether the app uses advertising, analytics, crash reporting, attribution, Firebase, or other SDKs, and describe any data those services process.** Update this policy and the Google Play Data safety form to match the shipped app.

## 7. Data retention and deletion

Information stored locally may remain on your device until you delete it within the app (if that feature is provided), clear the app's storage, or uninstall the app. Android backup settings may affect whether some app data is restored to a device.

The developer must verify actual deletion behavior, backup inclusion/exclusion rules, cached online tafsir and downloaded audio cleanup, and any server-side retention by third-party providers. Requests about data handled by third-party providers may need to be directed to those providers.

## 8. Children's privacy

The developer must determine the intended audience and whether the app is directed to children. Do not publish an age-related compliance statement until the app's audience, SDK behavior, and applicable legal requirements have been reviewed.

## 9. Security

The project manifest sets `android:usesCleartextTraffic="false"`, which is intended to disallow cleartext HTTP traffic for the app. This is one security measure and is not a guarantee that all information is secure. The developer should keep dependencies updated, protect signing keys and credentials, and review network and backup configuration before release.

## 10. Your choices

You can:
- Deny or revoke optional location permission in Android settings.
- Manage notifications and alarm permissions in Android settings.
- Use manual city selection where available.
- Remove locally stored app data by clearing app storage or uninstalling, subject to Android backup behavior.

For privacy questions or requests, contact: **[INSERT MONITORED PRIVACY EMAIL]**.

## 11. Changes to this policy

The developer may update this policy when app features, data practices, or legal requirements change. The effective date above will be updated when a new version is published.

## 12. Contact

**Developer / data controller:** [INSERT LEGAL NAME OR BUSINESS NAME]  
**Privacy email:** [INSERT MONITORED PRIVACY EMAIL]  
**Postal address (if legally required):** [INSERT BUSINESS ADDRESS OR REMOVE AFTER LEGAL REVIEW]

---

## Release owner sign-off (remove this section from the public policy)

Before publishing, the developer must:

1. Replace every bracketed placeholder and host this policy at a stable, publicly accessible HTTPS URL.
2. Audit all source code, dependencies, SDKs, API calls, analytics, crash reporting, ads, and Firebase configuration.
3. Verify location, notification, exact-alarm, audio, backup, deletion, and retention behavior on a release build.
4. Complete Google Play Data safety and app-content declarations accurately for the exact build being submitted.
5. Ensure the in-app privacy policy link and Play Console privacy policy URL point to this same policy.
6. Obtain appropriate legal review for the countries where the app will be distributed.
