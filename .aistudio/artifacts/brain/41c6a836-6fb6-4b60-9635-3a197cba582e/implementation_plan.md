# SmartBoard Pro — Launcher Icon Visibility, Open Button Fix & Startup Verification Plan

This plan resolves the exact root cause why **SmartBoard Pro** appeared in Android `Settings > Manage apps` (`29.47 MB`) with the **Open** button disabled and no icon in the Home Screen launcher menu, adds a one-tap Home Screen shortcut utility, and verifies full launcher intent resolution and activity startup using automated Robolectric tests.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Root Cause Identified in `AndroidManifest.xml`:**
> Upon inspecting the manifest, `MainActivity`'s `<intent-filter>` declared:
> `<category android:name="android.intent.action.LAUNCHER" />` instead of `<category android:name="android.intent.category.LAUNCHER" />`.
>
> Because Android's `PackageManager.getLaunchIntentForPackage()` and all OEM Home Screen launchers (including Honor MagicOS) query strictly for `android.intent.category.LAUNCHER`, Android installed the APK (`29.47 MB`) in `Settings > Manage apps` but concluded the app had no launchable entry point—disabling the **Open** button and hiding the launcher icon.

- **Confirmed Decision 1 (Symptom Diagnosis)**: Confirmed that the `Open` button inside `Settings > Manage apps > SmartBoard Pro` is disabled/missing on the user's phone, matching the `android.intent.category.LAUNCHER` manifest category mismatch.
- **Confirmed Decision 2 (Fix Scope & Verification)**: Fix the launcher manifest declaration, add an in-app one-tap **Pin Shortcut to Home Screen** option inside Studio Settings, run automated Robolectric launcher/startup verification tests, and regenerate the downloadable `SmartBoard-Pro.apk` binary.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. Restores standard Android launcher visibility and enables the **Open** button in `Settings > Manage apps` by declaring `<category android:name="android.intent.category.LAUNCHER" />` and `<category android:name="android.intent.category.DEFAULT" />` with `android:enabled="true"` and `android:exported="true"` on `MainActivity`.
  2. Adds a **One-Tap Home Screen Shortcut (`হোম স্ক্রিনে শর্টকাট যোগ করুন / Add Home Screen Shortcut`)** button inside **Studio Settings** using Android's `ShortcutManager` API for OEM launchers (such as Honor MagicOS) that support pinned shortcuts.
  3. Wraps Firebase / Classroom initialization in defensive error guards so the whiteboard studio always boots into offline Room mode even on devices without active Google Play Services.
  4. Verifies via automated JVM/Robolectric tests that `PackageManager.getLaunchIntentForPackage()` returns a valid launch intent for `MainActivity` and that `MainActivity` renders its Compose UI without error.
- **Target Audience / Persona**: Teachers, students, and self-learners installing `SmartBoard Pro` via APK on physical Android phones and tablets (including Honor MagicOS, Samsung OneUI, Xiaomi HyperOS, and stock Android).
- **Key Value**: Immediate launcher icon visibility after APK installation, an enabled **Open** button in Android system settings, and verified startup stability.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **APK Installation & Direct Launch**: When the user installs the updated `SmartBoard-Pro.apk`, the Android package installer immediately shows an active **Open (ওপেন)** button on the installation completion screen, places the **SmartBoard Pro** adaptive icon on the Home Screen / App Drawer, and enables the **Open** button inside `Settings > Manage apps`.
  2. **In-App Home Screen Shortcut Pinning**: Inside the top studio bar's **Studio Settings (স্টুডিও সেটিংস)** dialog, users can tap **Add Shortcut to Home Screen (হোম স্ক্রিনে শর্টকাট যোগ করুন)** to request an instant pinned launcher icon on Android 8.0+ devices.
- **Visual Identity & Theme**:
  - Preserves the **Precision Tactile Studio** aesthetic with Deep Slate (`#0F172A`) chrome, Electric Cobalt (`#2563EB`) interactive accents, and clean bilingual (`বাংলা` / `EN`) typography.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Standard `CATEGORY_LAUNCHER` + `CATEGORY_DEFAULT` Manifest Resolution**
  - *Chosen Approach*: Replace the mistyped `android.intent.action.LAUNCHER` category with `android.intent.category.LAUNCHER` and `android.intent.category.DEFAULT`, explicitly set `android:enabled="true"`, and use the fully qualified class name `com.example.MainActivity`.
  - *Why*: Android's `LauncherApps` service and `PackageManager.getLaunchIntentForPackage()` require exact matching on `Intent.ACTION_MAIN` + `Intent.CATEGORY_LAUNCHER`.
- **Decision 2: Automated Robolectric PackageManager & Activity Boot Verification**
  - *Chosen Approach*: Add a dedicated Robolectric test suite that inspects the merged manifest through `PackageManager` and boots `MainActivity` through its full `onCreate` Compose lifecycle.
  - *Why*: Prevents any regression in launcher intent resolution or activity startup before exporting the APK for GitHub download.
- **Decision 3: Deterministic `SmartBoard-Pro.apk` Packaging**
  - *Chosen Approach*: Configure `exportGithubApk` to copy the freshly packaged `outputs/apk/debug/app-debug.apk` directly into `apk/SmartBoard-Pro.apk` on every build so pushing to GitHub immediately updates the downloadable file.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Android PackageManager & Launcher                    │
│  Intent(ACTION_MAIN) + Category(CATEGORY_LAUNCHER | CATEGORY_DEFAULT)   │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ Resolves non-null Launch Intent
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     MainActivity (enabled=true, exported=true)          │
│  ┌─────────────────────────────┐     ┌───────────────────────────────┐  │
│  │ Room SmartBoardDatabase     │     │ Defensive ClassroomRepository │  │
│  │ (Offline Auto-Save Canvas)  │     │ (Safe Firebase Init Guard)    │  │
│  └──────────────┬──────────────┘     └───────────────┬───────────────┘  │
│                 └─────────────────┬──────────────────┘                  │
│                                   ▼                                     │
│                         WhiteboardViewModel                             │
│                                   │                                     │
│         ┌─────────────────────────┼─────────────────────────┐           │
│         ▼                         ▼                         ▼           │
│  InteractiveWhiteboard    FloatingBottomDock       StudioSettingsDialog │
│  Canvas & Math Overlay    & Geometry Tools         (+ Pin Home Shortcut)│
└─────────────────────────────────────────────────────────────────────────┘
```

- **Interactive Component & State Mapping**:
  - **Launcher Intent Filter**: Maps `android.intent.action.MAIN` + `android.intent.category.LAUNCHER` directly to `com.example.MainActivity`.
  - **Pin Shortcut Handler (`StudioSettingsDialog`)**: Invokes Android's `ShortcutManager.requestPinShortcut()` with a `ShortcutInfo` targeting `MainActivity` (`Intent.ACTION_MAIN`) and displays a bilingual confirmation banner in `WhiteboardViewModel`.
  - **Robolectric Verification Suite**: Asserts `packageManager.getLaunchIntentForPackage(packageName) != null`, verifies `MainActivity` is present in `queryIntentActivities`, and launches `MainActivity` to idle state.
