# Manual test checklist

Run before tagging each release, on an emulator (API 35/36) or real device.

## M0 (v0.0.1)
- [ ] App installs and launches without crash
- [ ] Home shows empty state with app bar
- [ ] Settings opens; theme switches between System/Light/Dark and persists across restart
- [ ] Dark theme uses tonal near-black surfaces (not pure black), lightened primary
- [ ] `./gradlew lint testDebugUnitTest assembleDebug` green
- [ ] Native libs in APK are 16KB page aligned (CI check)

## Later milestones
Checklists are appended as features land: viewing (M1–M2), annotations round-trip
including opening annotated output in a third-party viewer (M3–M4), page ops (M5),
creation (M6), encryption (M7).
