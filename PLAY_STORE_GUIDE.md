# Apex Dusk — Google Play Store Publishing Guide

Everything in this Android project is configured for Google Play Store submission.

---

## 1. App Identity & Technical Configuration
- **App Name:** `Apex Dusk` (9 characters — compliant with Play Console 30-char limit, no buzzwords or emojis)
- **Package Name (`applicationId`):** `com.aistudio.apexdusk.rzqmxp`
- **Version:** `versionCode = 1`, `versionName = "1.0.0"`
- **SDK Levels:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`
- **Category Metadata:** `android:appCategory="game"` and `android:isGame="true"` in `AndroidManifest.xml`
- **Release Optimization:** R8 minification (`isMinifyEnabled = true`) + resource shrinking (`isShrinkResources = true`) with crash stack-trace line-number preservation in `app/proguard-rules.pro`
- **Permissions:** **Zero runtime or dangerous permissions** required (100% offline gameplay)
- **Adaptive & Store Icons:** Adaptive launcher icons (`ic_launcher`, `ic_launcher_round`) plus Play Store assets in `play_store_assets/`:
  - `play_store_assets/play_store_icon_512.png` (512 × 512 px)
  - `play_store_assets/feature_graphic_1024x500.png` (1024 × 500 px)

---

## 2. How to Generate Your Signed Release `.aab` (Android App Bundle)
Google Play requires an `.aab` signed with your upload key:
1. In **AI Studio**, open the **Settings menu** and choose **Generate AAB** (or export the project ZIP to sign locally in Android Studio via **Build → Generate Signed Bundle / APK → Android App Bundle**).
2. If signing locally with your own Play Upload Keystore, set `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables before running `gradle :app:bundleRelease`.

---

## 3. Ready-to-Use Google Play Console Listing Copy

### App Name (Max 30 chars)
`Apex Dusk`

### Short Description (Max 80 chars)
`Race an endless 3D dusk highway, weave through traffic, and chain near-misses.`

### Full Description
```text
Hit the open twilight highway in Apex Dusk — a 3D endless sportbike racer built for smooth 60 FPS arcade speed.

Weave through 4 lanes of dusk traffic under glowing streetlamps, shifting atmospheric mist, and light rain while chaining high-speed near-misses to charge your Nitro Boost.

KEY FEATURES
• Dynamic Dusk Highway: Twilight sky dome, low-hanging sunset glow, city skyline silhouettes, and a random weather system that transitions smoothly between Clear Dusk, Light Mist, and Light Rain.
• Custom Sportbikes: Choose from 4 racing liveries with leaning rider physics, spinning spoked wheels, brake-reactive LED taillights, and dual-exhaust nitro flames.
• Near-Miss & Combo System: Thread past cars and box trucks at high speed to build up to a 10x combo multiplier and refill Nitro.
• Daily Missions: Complete daily challenges—such as achieving 5 near-misses, riding 3.0 KM, or hitting 280 KM/H—to unlock up to a 2.00x bonus score multiplier.
• Real-Time Synthesized Audio: Responsive engine harmonics, nitro turbine whoosh, combo chimes, and crash impacts generated directly on-device.
• Touch & Keyboard Ready: Ergonomic multi-touch steering, gas, brake, and nitro pads plus full hardware keyboard support.
• 100% Offline & Private: No ads, no tracking, and no account required.
```

---

## 4. Play Console Questionnaire Answers

### App Category & Tags
- **App or Game:** `Game`
- **Category:** `Racing`

### Data Safety Form
- **Does your app collect or share any of the required user data types?** → **No**
- **Reason:** Apex Dusk runs 100% offline. High scores, daily mission progress, bike livery, and mute preferences are stored strictly locally on-device via Android DataStore.
- *(Note: Google Play Console still asks for a Privacy Policy URL on store listings if applicable; you can host a simple static page stating that Apex Dusk collects zero user data, matching the in-app **About & Privacy** screen.)*

### Content Rating (IARC)
- **Violence:** None (arcade traffic collision reset with sparks; no blood, gore, or weapons)
- **Ads / In-App Purchases:** None
- **Expected Rating:** `Everyone` (ESRB) / `PEGI 3`

### Target Audience
- General / All Ages (or 13+ if you prefer not to opt into the Families program).
