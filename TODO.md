# 📋 Pre-Release & Store Publishing Checklist (TODO)

> [!CAUTION]
> **READ BEFORE PUBLISHING TO GOOGLE PLAY STORE OR F-DROID**  
> Submitting the app to Google Play Store in its current GitHub-release configuration **will cause policy rejections or account strikes**. Complete the action items below before submitting.

---

## 🚨 1. Google Play Store Pre-Submission Blockers

### A. In-App APK Download / External Update Checks
* **Current Status in Repo:**
  * [`UpdateChecker.kt`](app/src/main/java/com/hrshd1eux/expensetracker/util/UpdateChecker.kt) queries GitHub Releases API.
  * When an update is detected, clicking "Download" launches `Intent.ACTION_VIEW` targeting the direct `.apk` file (`browser_download_url`).
* **The Policy Violation on Google Play:**
  * **Google Play DDA (Section 4.5) & Device and Network Abuse Policy:** An app distributed via Google Play **must never** facilitate, prompt, or download an external APK to update itself. Doing so triggers an instant policy strike / suspension.
  * **Play Protect / Anti-Virus Heuristics:** Prompting users to download `.apk` binaries directly from the web can trigger false-positive "Downloader" heuristic warnings.
* **Action Required Before Play Store Release:**
  * **Option 1 (Recommended):** Change the update button action in [`SettingsScreen.kt`](app/src/main/java/com/hrshd1eux/expensetracker/presentation/settings/SettingsScreen.kt) to open the Play Store listing:
    ```kotlin
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.hrshd1eux.expensetracker")).apply {
        setPackage("com.android.vending")
    }
    context.startActivity(intent)
    ```
  * **Option 2:** Set up Gradle build flavors (`github` vs `playstore`). Enable `UpdateChecker` exclusively for the `github` flavor and disable it completely in `playstore`.

---

### B. `REQUEST_INSTALL_PACKAGES` Permission (Strictly Prohibited)
* **Current Status in Repo:**
  * **NOT PRESENT.** (The app correctly does NOT have this permission).
* **Warning:**
  * **DO NOT** add `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />` under any circumstances.
  * Google Play strictly reserves this permission for enterprise device managers or certified third-party app stores. Any utility or expense app requesting this permission will face **instant automatic rejection**.

---

### C. Network Permission (`android.permission.INTERNET`)
* **Current Status in Repo:**
  * Present in [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) solely for the GitHub release check.
* **Play Store Recommendation:**
  * If you remove the GitHub update check for the Google Play build, **remove `android.permission.INTERNET` completely**.
  * A financial app with **literal zero network permissions** in Android OS settings builds massive user trust and eliminates Data Safety form compliance risks.

---

## 🔒 2. Google Play Console Setup & Declarations

### A. Data Safety Form
* **Does your app collect or share user data?** Select **NO**.
* **Is all user data encrypted in transit?** Select **N/A** (no data is collected or transmitted).
* **Do you provide a way for users to request data deletion?** Select **N/A** (no accounts or remote servers exist; deleting the app deletes all data).

### B. Cryptography / Export Compliance Declaration (SQLCipher)
* Google Play asks: *"Does your app use, contain, or incorporate encryption?"*
* Select **YES**.
* When asked if it qualifies for an exemption under US EAR (Export Administration Regulations):
  * Select **YES** (Mass-market open-source software publicly available under License Exception TSU / ENC, 15 CFR § 740.13(e)).

### C. Build Format (.aab)
* Google Play requires an **Android App Bundle (.aab)** for new apps, not an `.apk`.
* Generate using:
  ```bash
  ./gradlew bundleRelease
  ```
  The bundle will be generated at `app/build/outputs/bundle/release/app-release.aab`.

---

## 🤖 3. F-Droid Publishing Checklist

* [ ] Ensure all code and dependencies are 100% open source (Apache 2.0 / BSD).
* [ ] Disable the external `.apk` updater (F-Droid flags binary updaters as `AntiFeatures: NonFreeAdd` / `UpstreamNonFree`).
* [ ] Verify that reproducible builds pass with standard F-Droid build scripts.

---

## 📝 4. General App Readiness Tasks

- [x] Defuse SQLCipher key-regeneration data-loss bug.
- [x] Protect against Android Keystore biometric invalidation crashes.
- [x] Sanitize README and website comparison tables from competitor trademarks.
- [x] Add Apache License 2.0 (`LICENSE`) and third-party notices (`NOTICE`).
- [x] Add comprehensive Privacy Policy (`PRIVACY_POLICY.md`).
- [x] Add in-app Open Source Licenses dialog and Financial Disclaimer in Settings.
- [ ] Create Google Play Store listing graphics (512x512 app icon, 1024x500 feature graphic, screenshots).
- [ ] Test `.aab` signing and upload to Google Play Internal Testing track.
