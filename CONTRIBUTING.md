# Contributing

Thanks for your interest in improving ClockIn!

## Building

You need **JDK 17** and the **Android SDK (API 34)** — or just open the project in Android Studio.

```bash
./gradlew assembleDebug     # build a debug APK
```

The debug APK lands in `app/build/outputs/apk/debug/`. Testing NFC requires real hardware — the
emulator has no NFC — so keep a spare NFC-capable phone and a few NTAG21x tags around.

## Guidelines

- **Keep it dependency-light.** The app intentionally has no DI framework, no network stack, and no
  analytics. PRs that add heavy dependencies for small wins will be asked to slim down.
- **Never add network permissions.** Offline-only is a core promise of this project.
- **Mind the data invariants** (see [ARCHITECTURE.md](ARCHITECTURE.md)): `timestamp` must encode the
  scan moment, and stored date/time strings must remain strict ASCII `yyyy-MM-dd` / `HH:mm:ss`
  regardless of device locale.
- **Schema changes need a Room migration** and a version bump in `AppDatabase`.
- Match the existing code style (Kotlin official style, Compose single-activity patterns, explicit
  imports).
- CI must pass: every PR builds a debug APK via GitHub Actions.

## Reporting issues

Please include the Android version, device model, the NFC tag type (e.g. NTAG213) if relevant, and
steps to reproduce. Attendance data never leaves your device, so logs/screenshots are on you to
sanitize before posting.
