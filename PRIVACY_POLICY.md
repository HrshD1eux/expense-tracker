# Privacy Policy for Expense Tracker

**Last Updated:** October 2026

**Expense Tracker** ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy describes how your information is handled by the Expense Tracker Android application.

---

## 1. 100% Offline Architecture & Local Storage
- **Zero Account Creation:** Expense Tracker does not require registration, login, phone numbers, email addresses, or account setup.
- **On-Device Data Storage:** All financial records, expense entries, budgets, notes, and custom categories created within the app are stored exclusively on your physical device in an encrypted SQLite database managed via SQLCipher (256-bit AES encryption) and the Android hardware Keystore.
- **No Cloud Synchronization or Remote Servers:** We do not operate any backend servers or cloud databases for personal financial data. Your data never leaves your device unless you explicitly export an encrypted backup file.

---

## 2. Network Usage & Permissions
The app requests the `android.permission.INTERNET` permission for one purpose only:
- **User-Initiated Update Check:** When you tap "Check for Updates" in Settings, the app connects directly to the public GitHub API (`https://api.github.com/repos/HrshD1eux/expense-tracker/releases/latest`) to check if a newer release tag exists.
- **No Telemetry or Analytics:** No personal information, financial data, device identifiers, or analytics are transmitted during this check.
- **No Background Network Activity:** The app does not ping remote servers in the background.

---

## 3. Crash Diagnostics
- The app includes an offline crash logging utility that writes fatal unhandled exception stack traces to internal private app storage.
- Crash logs are never transmitted automatically to any remote service (zero Firebase Crashlytics, zero Sentry, zero third-party SDKs).
- You may inspect, copy, or purge these crash logs at any time via **Settings → Diagnostics & Error Logs**.

---

## 4. Biometric & Security Credentials
- The app supports Android Biometric authentication (fingerprint/face unlock) and local PIN/Pattern/Password locks.
- Authentication is handled entirely by the Android OS framework (`androidx.biometric.BiometricPrompt`). The app never has access to raw biometric data or hardware sensor outputs.

---

## 5. Third-Party Services & Advertising
- The app contains **zero advertisements**, zero ad tracking networks (no Google AdMob, Unity, etc.), and zero marketing analytics.

---

## 6. Children's Privacy
Because Expense Tracker does not collect any personal data, it complies with the Children's Online Privacy Protection Act (COPPA), GDPR, and applicable privacy regulations worldwide.

---

## 7. Legal Disclaimer & No Financial Advice
- Expense Tracker is a personal organizational and record-keeping utility provided on an **"AS IS"** basis under the Apache License 2.0.
- The app and its budget metrics (including "Daily Safe-to-Spend" and velocity calculations) are automated mathematical estimates and do not constitute financial, investment, accounting, tax, or legal advice.
- Users are solely responsible for maintaining external backups of their data.

---

## 8. Contact & Inquiries
If you have any questions or concerns regarding this policy, you may open an issue on the official project repository:
[https://github.com/HrshD1eux/expense-tracker](https://github.com/HrshD1eux/expense-tracker)
