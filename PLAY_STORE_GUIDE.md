# Apex Dusk — Google Play Store Publishing & Testing Guide

Everything in this Android project is configured for Google Play Store production and testing tracks.

---

## 1. Why Google Play Showed "Requested URL was not found on this server" (404)

Seeing the Play Store 404 page (`play.google.com/store/apps/details?id=com.aistudio.apexdusk.rzqmxp`) happens when **you open the public Store URL before the app is live in Production** or **before joining the test via the Opt-In URL**.

### Checklist to Fix the 404 Testing Link Immediately:
1. **Use the Tester Opt-In URL (not the public Store URL):**
   - In Google Play Console, go to **Testing → Internal testing** (or **Closed testing**) → **Testers** tab.
   - Click **Copy link** under **"How testers join your test"**. It looks like:
     `https://play.google.com/apps/testing/com.aistudio.apexdusk.rzqmxp`
2. **Add & Save Your Tester Email List:**
   - Under **Testers**, check the box next to your email list (make sure your Google account email, e.g. `fsferdows04@gmail.com`, is in that list) and click **Save changes** at the bottom-right.
3. **Accept the Opt-In First:**
   - Open `https://play.google.com/apps/testing/com.aistudio.apexdusk.rzqmxp` on your phone/browser while signed into that exact Google account, tap **Become a tester**, and then tap **"download it on Google Play"**.
4. **Verify Track Status & Country Availability:**
   - Make sure the release shows **Available to internal testers** (Internal testing goes live in ~15–60 minutes; Closed/Production tracks require Google review of 1–7 days).
   - Under **Countries / regions**, confirm your country is enabled.
5. **Note for New Personal Play Console Accounts (Closed Testing Rule):**
   - Google requires personal developer accounts to run a **Closed test with at least 12 opted-in testers continuously for 14 days** before unlocking **Apply for production**. Until Production is approved and rolled out to 100%, the public store URL returns 404.

---

## 2. App Identity & Technical Configuration
- **App Name:** `Apex Dusk` (9 chars — compliant with Play Console 30-char limit, no buzzwords or emojis)
- **Package Name (`applicationId`):** `com.aistudio.apexdusk.rzqmxp`
- **Current Version:** `versionCode = 2`, `versionName = "2.0"`
- **SDK Levels:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`
- **Category Metadata:** `android:appCategory="game"` and `android:isGame="true"` in `AndroidManifest.xml`
- **Release Optimization:** R8 code shrinking (`isMinifyEnabled = true`) + resource shrinking (`isShrinkResources = true`) with crash stack-trace line-number preservation in `app/proguard-rules.pro`
- **Permissions:** **Zero runtime or dangerous permissions** (100% offline gameplay)

---

## 3. Ready-to-Upload Store Graphics (`play_store_assets/`)

All required Google Play Console Store Listing graphics are prepared in `play_store_assets/`:

| Play Console Slot | File | Dimensions |
|---|---|---|
| **App Icon** | `play_store_assets/play_store_icon_512.png` | `512 × 512` px (full-bleed RGB PNG) |
| **Feature Graphic** | `play_store_assets/feature_graphic_1024x500.png` | `1024 × 500` px (RGB PNG) |
| **Phone & Tablet Screenshot 1** | `play_store_assets/screenshot_1_dusk_highway.png` | `1920 × 1080` px (16:9 PNG) |
| **Phone & Tablet Screenshot 2** | `play_store_assets/screenshot_2_nitro_rain.png` | `1920 × 1080` px (16:9 PNG) |
| **Phone & Tablet Screenshot 3** | `play_store_assets/screenshot_3_garage_showroom.png` | `1920 × 1080` px (16:9 PNG) |
| **Privacy Policy HTML** | `play_store_assets/privacy_policy.html` | Static HTML ready to host on GitHub Pages / Google Sites / Firebase Hosting |

*(Tip: You can upload the 3 `1920 × 1080` screenshots to Phone, 7-inch Tablet, and 10-inch Tablet screenshot slots in Play Console.)*

---

## 4. Generating & Uploading Your Signed `.aab`

1. In **AI Studio**, open the **Settings menu** → **Generate AAB** (or export the ZIP and run Android Studio → **Build → Generate Signed Bundle / APK → Android App Bundle**).
2. Always upload the **`.aab`** signed with the **same Upload Key** registered in Play Console (**App integrity → Play App Signing**), and increment `versionCode` for each new release.
3. Also upload `app/build/outputs/mapping/release/mapping.txt` to **Play Console → Release → App bundle explorer → Downloads → ReTrace mapping file** if not bundled automatically (it is already embedded in `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`).

---

## 5. Ready-to-Use Google Play Console Listing Copy

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

### Release Notes (`v2.0`)
```text
- Added Race Showcase gallery & game emblem branding on the start screen
- Dynamic weather transitions (Clear Dusk, Light Mist, Light Rain)
- Daily Missions system with stackable score multipliers up to 2.00x
- R8 release size & performance optimizations for Google Play
```

---

## 6. Play Console App Content Questionnaires
- **Category:** `Game` → `Racing`
- **Privacy Policy:** Host `play_store_assets/privacy_policy.html` (e.g., on GitHub Pages) and paste its public HTTPS URL into **App content → Privacy policy**.
- **Data Safety:** **Does your app collect or share user data?** → **No** (100% offline, local DataStore only).
- **Ads:** **No, my app does not contain ads.**
- **App Access:** **All functionality is available without special access** (no login required).
- **Content Rating (IARC):** No violence/blood/gambling/user-generated content → Expected rating: **Everyone / PEGI 3**.
- **Target Audience:** General / 13+ (or All Ages if opting into Families).
