# 💰 Expense Tracker

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Security](https://img.shields.io/badge/Security-SQLCipher%20256--bit%20AES-red?style=for-the-badge&logo=shield&logoColor=white)
![Offline](https://img.shields.io/badge/Privacy-100%25%20Offline%20%26%20Zero%20Telemetry-success?style=for-the-badge&logo=offline&logoColor=white)
[![CI/CD](https://img.shields.io/github/actions/workflow/status/HrshD1eux/expense-tracker/release.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=Build)](https://github.com/HrshD1eux/expense-tracker/actions)

<p align="center">
  <b>A modern, lightning-fast, and uncompromisingly private personal finance manager built for Android.</b><br>
  No accounts. No ads. No trackers. Zero network dependencies for financial data.
</p>

[**🌐 Live Website & Demo**](https://hrshd1eux.github.io/expense-tracker/) • [**📥 Direct Download APK**](https://github.com/HrshD1eux/expense-tracker/releases/latest/download/ExpenseTracker-v1.0.0-release.apk) • [**📦 All Releases**](https://github.com/HrshD1eux/expense-tracker/releases) • [**Developer Profile**](https://github.com/HrshD1eux)

</div>

---

## 🥊 Top 5 Alternatives vs. Expense Tracker

| Feature / Dimension | **Expense Tracker (This App)** | **Axio (Walnut)** | **Money Manager (Realbyte)** | **Spendee** | **Cashew** | **1Money** |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| **100% Offline (No Cloud Req)** | ✅ **YES (Zero Telemetry)** | ❌ Cloud Account Mandatory | ⚠️ Local + AdMob | ❌ Mandatory Cloud Sync | ⚠️ Local + Cloud Sync | ❌ Cloud Account Req |
| **Database Encryption** | ✅ **SQLCipher 256-Bit AES** | ❌ Plaintext SQLite Cache | ❌ Plaintext SQLite | ❌ Plaintext SQLite | ❌ Unencrypted Room DB | ❌ Plaintext SQLite |
| **Hardware Keystore (TEE)** | ✅ **Android Keystore** | ❌ None | ❌ None | ❌ None | ❌ None | ❌ None |
| **In-Line Math Keypad** | ✅ **Live (+, -, ×, ÷)** | ❌ None | ⚠️ Separate Popup | ❌ Simple Keypad | ⚠️ Basic Arithmetic | ⚠️ Separate Popup |
| **Multi-Mode App Locks** | ✅ **Biometric + Pattern + Pass + PIN** | ⚠️ Biometric/PIN only | ⚠️ 4-Digit PIN only | ⚠️ Biometric/PIN only | ⚠️ Biometric/PIN only | ⚠️ 4-Digit PIN only |
| **Adaptive Category Frequency** | ✅ **Dynamic Auto-Sort** | ❌ Static List | ❌ Manual Sort Only | ❌ Static List | ❌ Static List | ❌ Static List |
| **Ads & SMS Scraping** | ✅ **0 Ads & 0 Scraping** | ❌ Scrapes SMS & Loans | ❌ Banner/Interstitial Ads | ❌ Upsell & Trackers | ✅ Ad-free | ❌ Banner Ads in Free |
| **Pricing / Paywalls** | ✅ **100% Free Forever (FOSS)** | ⚠️ Free (Monetized via Data)| ⚠️ $5.99 for Ad-free | ❌ $14.99–$29.99 / Year | ⚠️ Free / Tips | ❌ $19.99 License |
| **Glance Home Widget** | ✅ **Jetpack Glance (Material 3)**| ❌ Legacy RemoteViews | ⚠️ Basic Widget | ❌ Subscription Paywall | ⚠️ Basic Widget | ⚠️ Basic Widget |
| **APK Footprint** | ✅ **~12.3 MB (R8 Minified)** | ❌ ~48 MB (SDK Bloat) | ⚠️ ~32 MB | ❌ ~44 MB | ⚠️ ~26 MB | ⚠️ ~24 MB |
| **Local Diagnostics** | ✅ **Local In-App Crash Logs** | ❌ Cloud Trackers | ❌ Firebase Crashlytics | ❌ Sentry / Firebase | ⚠️ Local / Sentry | ❌ Firebase Crashlytics |

---

## 🌟 Overview

**Expense Tracker** was built from the ground up for individuals who care deeply about financial privacy, speed, and clean design. Most commercial expense tracking apps harvest your transaction data, require cloud accounts, or bombard you with ads and unsolicited financial offers.

This app is **100% offline, hardware-encrypted, and open-source**. All your financial records remain strictly sandboxed on your physical device, protected by 256-bit AES encryption through SQLCipher and the hardware-backed Android Keystore.

---

## ✨ Key Features

### 🧮 In-line Smart Math Calculator
- Calculate split bills, tips, discounts, and itemized totals directly inside the amount input field (`+`, `-`, `×`, `÷`, `.`, `=`).
- Instant real-time evaluation with automatic cursor management for seamless continuous typing.
- All monetary arithmetic is computed in integer paise/cents, eliminating IEEE 754 floating-point rounding errors.

### 🛡️ Ironclad Security & Cryptography
- **SQLCipher 256-bit AES Database**: The entire Room database file is transparently encrypted at rest with hardware-backed keys.
- **Hardware Keystore Protected**: Database passphrases are generated via `MasterKey` inside the Android Keystore daemon.
- **Fail-Safe Integrity Protection**: Robust corruption recovery that automatically backs up existing data to sandboxed storage before recovery routines can run, preventing accidental data loss.
- **Biometric Invalidation Immunity**: Built to survive device lock screen alterations without corrupting existing database keys.
- **Window Security**: Optional `FLAG_SECURE` integration shields financial screens from Android app switchers and unauthorized screenshots.

### 🔐 Multi-Mode App Lock
- **Biometric Unlock**: Class 3 Biometric authentication (Fingerprint and Face Unlock) via AndroidX Biometrics.
- **Pattern Lock**: Smooth custom 3×3 grid vector pattern unlock.
- **Alphanumeric Password**: Salted and cryptographically hashed master passphrase.
- **4-Digit PIN**: Fast numerical lock with randomized keypad support.
- **Configurable Auto-Lock**: Instantly on background, 30 seconds, 1 minute, or 5 minutes.

### ⚡ Adaptive Category Intelligence
- Categories automatically adapt to your habits. The most frequently used categories dynamically surface to the top of your selection grid, minimizing taps.
- Custom categories with custom icons, color palettes, and budget allocation.

### 📊 Deep Analytics & "Safe to Spend"
- **Periodic Breakdown**: Comprehensive day, week, month, and year summaries.
- **Payment Method Split**: Separate analytics for UPI vs. Cash vs. Card.
- **Hourly Spending Heatmaps**: Pinpoint what times of day you spend the most.
- **Daily Safe-to-Spend**: Real-time contextual budget gauge indicating safe daily limits to prevent running out of money before month-end.
- **Edge-Case Hardened**: Calendar math properly handles leap years, month boundaries, and February end dates.

### 📱 Modern Jetpack Glance Home Screen Widget
- Compact and rich Glance widgets for your launcher.
- Monitor today's spending, view remaining monthly budget, and launch straight into the "Add Expense" screen with 1 tap.

### 📦 Seamless Backup & Portability
- Export full transaction history to encrypted backup files or standard CSV / JSON.
- Restore from existing backups with strict schema validation.

### 🚀 Ultra-Lean & Fast (R8 Optimized)
- Full R8 ProGuard code and resource shrinking.
- Native ABI splits (`arm64-v8a`, `armeabi-v7a`) reducing APK footprint by **~65%** (down to ~12 MB).

### 🛠️ In-App Offline Diagnostics & Updates
- **Built-in Error Logs**: Sandboxed crash reporter lets users inspect and copy crash stack traces directly from Settings without telemetry.
- **GitHub Release Checker**: Directly checks for newer releases from this repository and prompts for one-tap APK installation.

---

## 🏗️ Architecture & Tech Stack

The app follows **Modern Android Architecture (MVI / Clean Architecture)** with Unidirectional Data Flow (UDF):

```
app/
├── data/               # Room DB, SQLCipher, DAOs, Repositories, DataStore
├── domain/             # Business logic, Models, Domain Use Cases
├── presentation/       # UI Layer with Jetpack Compose & ViewModels
│   ├── home/           # Dashboard, quick stats, recent expenses
│   ├── addexpense/     # In-line math calculator, category grid
│   ├── analytics/      # Charts, hourly heatmaps, spending velocity
│   ├── security/       # Biometric, Pattern, PIN, and Password locks
│   ├── settings/       # Theme, exports, diagnostics, update checker
│   └── widget/         # Jetpack Glance Home Screen Widget
└── di/                 # Dependency Injection via Hilt / Dagger
```

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.0+ (100%) |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | Clean Architecture + MVVM / MVI + Repository Pattern |
| **Dependency Injection** | Dagger Hilt |
| **Local Database** | Room Database with SQLCipher (256-bit AES encryption) |
| **Asynchronous Engine** | Kotlin Coroutines & StateFlow / SharedFlow |
| **Key Management** | Android Keystore (`MasterKey`, AES-256 GCM) |
| **App Widget** | Jetpack Glance AppWidget |
| **Build & Optimization** | Gradle Kotlin DSL, R8 Full Mode, Resource Shrinker |
| **CI / CD** | GitHub Actions (Automated Linting, Unit Tests, Signed APK Release) |

---

## 🛠️ Building & Running Locally

### Prerequisites
- **Android Studio Ladybug (2024.2.1)** or newer.
- **JDK 17** (Temurin recommended).
- **Android SDK Platform 35 / 36**.

### 1. Clone the repository
```bash
git clone git@github.com:HrshD1eux/expense-tracker.git
cd expense-tracker
```

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
The resulting debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 4. Build Signed Release APK
To build a production release APK signed with your own keystore:
```bash
./gradlew assembleRelease \
  -PversionName=1.0.0 \
  -PversionCode=1 \
  -PKEYSTORE_PATH="keystore/release.keystore" \
  -PKEYSTORE_PASSWORD="your_store_password" \
  -PKEY_ALIAS="your_key_alias" \
  -PKEY_PASSWORD="your_key_password"
```

---

## 🤖 CI / CD Pipeline

This project includes fully automated GitHub Actions workflows:

1. **Android CI (`ci.yml`)**:
   - Triggers on every push and pull request to `main` and `master`.
   - Runs unit tests and builds the debug APK to catch regressions.
2. **Release Build & Publish (`release.yml`)**:
   - Triggers on tag pushes (`v*.*.*`) or via manual `workflow_dispatch`.
   - Decodes the secret signing keystore, runs test suites, and compiles an R8-minified release APK.
   - Automatically computes SHA-256 checksums, compiles release notes from commit history, and attaches the release APK to a new GitHub Release.

### Required Secrets for GitHub Actions:
If you are deploying your own automated releases, configure these secrets in your repository settings (`Settings -> Secrets and variables -> Actions`):
- `KEYSTORE_BASE64`: Base64-encoded `.keystore` or `.jks` file.
- `KEYSTORE_PASSWORD`: Password for the keystore.
- `KEY_ALIAS`: Alias of the signing key.
- `KEY_PASSWORD`: Password for the key.

*(Note: If no secrets are set, the workflow will automatically fall back to generating a self-signed key so releases never fail).*

---

## 🔒 Privacy Policy

- **No Remote Servers**: No user data, expenses, notes, or balances are ever uploaded to any server.
- **Zero Analytics**: No Google Firebase, Firebase Analytics, Crashlytics, Mixpanel, or third-party ad networks are included.
- **Hardware-Encrypted Database**: All persistent data resides locally in a 256-bit AES encrypted SQLCipher database.
- **No Unnecessary Permissions**: Only system permissions essential for local functions (e.g. biometrics, widget updates, notifications) are requested.

---

## 👨‍💻 Developer & Author

Developed with care by **HrshD1eux**.

- **GitHub Profile**: [@HrshD1eux](https://github.com/HrshD1eux)
- **Repository**: [https://github.com/HrshD1eux/expense-tracker](https://github.com/HrshD1eux/expense-tracker)

---

## 📄 License

This project is licensed under the **Apache License 2.0** or **MIT License** — see the [LICENSE](LICENSE) file for details.
