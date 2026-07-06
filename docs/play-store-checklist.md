# Google Play release checklist

Steps only the account owner (you) can do. The app itself is release-ready:
signed AAB is attached to every GitHub release.

## One-time account setup
- [ ] Google Play Console account (one-time $25), identity verification complete
- [ ] Create app: name **OpenPDF — PDF Reader & Editor**, default language
      English, type App, Free

## Package & signing
- applicationId: `app.openpdf.foss` (permanent — matches the AAB)
- [ ] Enroll in **Play App Signing**. Upload key = the release keystore at
      `%USERPROFILE%\openpdf-signing\openpdf-release.jks` (also in GitHub
      secrets). **Back this up — losing it means no more updates.**

## Store listing
- Short description: see `fastlane/metadata/android/en-US/short_description.txt`
- Full description: see `fastlane/metadata/android/en-US/full_description.txt`
- [ ] App icon 512×512 (derive from `ic_launcher` — navy #2C3E50 sheet)
- [ ] Feature graphic 1024×500
- [ ] ≥2 phone screenshots (recommend 8: viewer light/dark, annotate, fill &
      sign, organize, scan/create, search, home). Capture on a device/emulator.

## Policy
- [ ] **Privacy policy URL:** https://tonmoybishwas.github.io/foss.openpdf.app/privacy-policy.html
- [ ] **Data safety form:** "No data collected", "No data shared". No INTERNET
      permission → nothing to declare for transit encryption.
- [ ] Content rating questionnaire → Everyone
- [ ] Ads declaration: **No ads**
- [ ] Target audience: 13+ (avoid child-directed obligations)
- [ ] Permissions: only optional `CAMERA` (declared `required=false`). No
      sensitive/background permissions to justify.

## Release
- [ ] Upload the `OpenPDF-vX.Y.Z.aab` from the GitHub release to a track
- [ ] **Closed testing first:** personal accounts created after Nov 2023 must
      run a closed test with **≥12 testers for 14 continuous days** before
      production access. Start recruiting now.
- [ ] targetSdk is 36 (meets Play's 2026 requirement)

## F-Droid (later, optional)
- `fastlane/metadata/android/en-US/` is already populated. Submit a merge
  request to `fdroiddata` referencing the GPL-3.0 license and the tagged
  releases. No proprietary dependencies are present.
