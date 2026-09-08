# JARVIS v3 command-center upgrade

The existing OpenJarvis desktop/chat functionality was preserved. The main route now opens a touch-first **J.A.R.V.I.S v3 command center** while the existing chat remains available at `/chat`.

## Added

- Responsive JARVIS home dashboard with privacy-first visual language.
- Voice console using browser speech recognition when available, with graceful fallback to the existing chat route.
- Typed command launcher that hands commands to the existing chat screen.
- Quick action deck for Ghost Call/automation, memory, translation, privacy, and voice controls.
- System status strip, smart suggestions, focus mode, local-memory indicator, live telemetry/settings navigation.
- Light/dark toggle using the existing persisted settings store.
- Mobile breakpoints for narrow Android-sized screens.

## Validation

- `npm run build` passes.
- `npm test` passes: **68 tests in 12 files**.
- Build was run with `npm install --engine-strict=false` because this sandbox has Node 22.13/npm 10 while the repository declares Node >=22.22/npm >=11.19.

## Android packaging note

This repository is currently a Tauri desktop project (`frontend/src-tauri`) rather than an Android-native project. An APK cannot be produced in this environment because the Rust/Cargo and Android SDK/NDK toolchain are not installed. The verified browser/PWA build is included in the deliverable ZIP and can be opened on Android or wrapped with the project's future Android toolchain.
