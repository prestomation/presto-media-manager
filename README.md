# Presto Media Manager

A personal, sideloaded Android app for rapidly triaging the pile of videos a pair
of Meta glasses (or any capture device) dumps into a folder every day. Review a
looping feed, one-tap **Delete / Later / Archive / Review**, trim + frame-crop in
an editor, and export with optional audio removal and downscale-for-sharing.

> Not a Play Store app. Built debug-signed and installed over ADB.

## Core workflow

1. **Review feed** — a vertical, swipeable feed. Each video loops. Act with one tap:
   - **Delete** — remove from the input folder.
   - **Later** — keep it, decide another day (still counts toward auto-delete).
   - **Archive** — prompts for a label, copies the **full-resolution original** into
     the archive folder as `YYYY-MM-DD_label.mp4`, removes it from input.
   - **Review** — open the editor.
2. **Editor** — frame-accurate scrub bar with draggable **start/end trim handles**, a
   drag/resize **crop rectangle**, and a zoom-inspect toggle. **Save** prompts for a
   label and options:
   - **Remove audio** on/off.
   - **Share resolution** (1080p / 720p / 480p).
   - **Archive** → full-res edited copy to the archive folder.
   - **Share + Archive** → full-res to archive **and** a downscaled copy to the share
     folder (sharing always archives, too). Both files share the `YYYY-MM-DD_label`
     stem; the date is the video's **capture** date.
3. **Settings** — pick the input / archive / share folders (Storage Access Framework),
   toggle **auto-delete** of unreviewed videos after a configurable number of days, and
   set export defaults.
4. **Badge** — an ongoing notification publishes the "videos to review" count so the
   launcher shows a badge. (Android has no direct icon-number API; the exact rendering
   depends on your launcher.)

## Architecture

- **Kotlin + Jetpack Compose** (Material 3), single-activity navigation.
- **Media3 ExoPlayer** for looping playback; **Media3 Transformer** for trim, spatial
  crop, downscale, and audio removal.
- **Storage Access Framework** with persistable URI permissions for all folder access
  (`SafManager`). No broad storage permission.
- **Room** mirrors the input folder and tracks each video's status (`MediaRepository`).
- **DataStore** for settings; **WorkManager** for the periodic folder scan, daily
  auto-delete, and badge updates.

```
app/src/main/java/com/presto/mediamanager/
  data/        model, Room db, settings (DataStore), SAF, repository
  media/       Transformer export pipeline + options
  work/        ScanWorker, AutoDeleteWorker, BadgeManager, WorkScheduler
  ui/          theme, feed, editor, settings, nav, components
```

## Build & install (ADB)

```bash
# One-time: install the Android SDK in a headless environment (skip on a normal
# Android Studio setup, which already has it).
bash scripts/setup.sh

# Build the debug APK.
./gradlew :app:assembleDebug

# Install on a connected device.
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch, open **Settings** and pick the three folders, then grant the
notification permission for the review badge.

## Testing

Two layers, both run in CI:

### JVM screenshot + behavior tests (no emulator)

Roborazzi + Robolectric render the screens to PNG and assert behavior entirely on the
JVM, so they run in any headless container.

```bash
# Re-render the golden screenshots after any UI change, then commit them.
./gradlew :app:recordRoborazziDebug

# Verify committed goldens match (this is the CI gate).
./gradlew :app:verifyRoborazziDebug
```

Golden images live in [`/screenshots`](screenshots) and are committed, so **every UI
change shows up as an image diff in the PR** (GitHub renders before/after). CI fails
`verifyRoborazziDebug` until the goldens are updated — you cannot land a UI change
without its screenshots.

### Emulator E2E (CI, blocking)

`AppLaunchTest` runs on a real emulator via `connectedDebugAndroidTest` to smoke-test
startup. Real video playback/export is validated manually on-device.

## CI

- **Build & Test** (`.github/workflows/build-and-test.yml`) — assembles the APK, runs
  the JVM tests + `verifyRoborazziDebug`, uploads the APK artifact, and posts a
  screenshot gallery comment on PRs. Blocking.
- **Emulator E2E** (`.github/workflows/emulator-e2e.yml`) — runs instrumented tests on
  a KVM-accelerated emulator. Blocking.

## Auto-install the SDK in Claude Code sessions

[`.claude/settings.json`](.claude/settings.json) registers a `SessionStart` hook that
runs `scripts/setup.sh`, so the Android SDK is installed automatically whenever a
Claude Code (local or web) session starts and the project can build/test immediately.
