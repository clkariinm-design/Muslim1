# Google Play Release Checklist — Nuur Qur’an

Use this checklist against the exact release candidate, not just the source branch.

## Legal, ownership, and privacy

- [ ] Replace the copyright-holder placeholder in `LICENSE` with the actual legal rights holder.
- [ ] Confirm the developer owns or has permission for all original code, artwork, fonts, icons, translations, audio, and bundled datasets.
- [ ] Verify each third-party licence and complete `THIRD_PARTY_NOTICES.md` with exact versions and required notices.
- [ ] Obtain permission for audio recordings and any content whose commercial redistribution rights are unclear.
- [ ] Complete all placeholders in `PRIVACY_POLICY.md`; publish it at a stable public HTTPS URL.
- [ ] Audit source code and all SDKs for collection, sharing, analytics, crash reporting, advertising, identifiers, and network transmission.
- [ ] Verify Android backup and data deletion behavior.
- [ ] Complete the Google Play Data safety form and app-content declarations to match the submitted binary.
- [ ] Add the same privacy policy URL in the app and Play Console.
- [ ] Provide a monitored support/privacy contact.

## Android release engineering

- [ ] Build and test `lintRelease`, unit tests, and instrumentation tests.
- [ ] Test on supported Android versions and representative screen sizes.
- [ ] Test offline Qur’an reading, downloaded audio, cache behavior, and network failure states.
- [ ] Test location denied/granted, manual city selection, prayer reminders, reboot/time-zone changes, and exact-alarm permission behavior.
- [ ] Verify audio playback, notification controls, background restrictions, and download cancellation.
- [ ] Verify RTL Arabic layout, Somali and English localization, accessibility labels, and text scaling.
- [ ] Review exported components, permissions, backup rules, and network security configuration.
- [ ] Confirm release versionCode/versionName and target API requirements against current Play Console requirements.
- [ ] Create and securely back up the upload keystore; keep passwords and signing material out of Git.
- [ ] Build a signed Android App Bundle (AAB), then test the signed artifact.
- [ ] Enable R8/minification only after validating required keep rules and testing the minified release.
- [ ] Check the final merged manifest and dependency/licence inventory.

## Store listing and operations

- [ ] Prepare app name, short/full descriptions, screenshots, icon, feature graphic, and category.
- [ ] Complete content rating, target audience, ads declaration, app access, and other Play Console forms accurately.
- [ ] Provide a support website/email and a process for handling privacy requests.
- [ ] Run an internal/closed testing track and resolve pre-launch report issues.
- [ ] Verify all claims in the listing, including offline functionality, translations, tafsir, and prayer-time accuracy.
- [ ] Publish a versioned release and monitor crashes, ANRs, reviews, and privacy requests.

## Current known blockers from repository documentation

- The README says the in-app privacy policy is still a draft.
- A public privacy policy URL and support/privacy email have not been supplied in the repository information reviewed.
- Release signing requires a locally protected upload keystore and environment variables.
- Third-party audio redistribution permissions and exact dataset licence notices need verification.
- The release build currently has minification disabled; this is not by itself a Play Store blocker, but release testing and a deliberate shrinking decision are required.
