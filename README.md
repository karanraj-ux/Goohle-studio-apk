# 🛡️ Shield

**A privacy-first, 100% offline SMS and Call automation engine.**

![Open Source](https://img.shields.io/badge/Open%20Source-100%25-green)
![No Telemetry](https://img.shields.io/badge/Telemetry-None-blue)
![Offline First](https://img.shields.io/badge/Offline-First-orange)

Most call blockers and SMS tools harvest your contacts and sell your data. **Shield is built differently.** It is a hyper-secure personal boundary management tool that keeps your sensitive data exactly where it belongs: on your device.

**About the Developer:**
Hi! I'm a 12th-pass student from Bihar, and I built this advanced automation engine using AI before even starting college. My goal is to create powerful, private, and accessible tools for everyone.

## 🚀 Features

* **Ghost Mode & Shield:** Automatically reject unknown numbers, block spam, and mute distractions without lifting a finger.
* **Silent Bypass:** Ensure your VIPs can always reach you, even when your phone is in Do Not Disturb mode.
* **Auto-Responder:** Automatically reply to missed calls or texts from specific contacts when you are busy or asleep.
* **Local Data Retention:** Built-in auto-cleanup ensures your database stays lean, preventing memory crashes and bloated storage over time.

## 🔒 Security Architecture

We don't just promise privacy; we engineered it.

* **SQLCipher Encrypted Database:** Your SMS logs, rules, and configurations are secured at rest using AES-256-GCM encryption backed by the Android Hardware Keystore. Even if your device is rooted, your data is unreadable.
* **Zero Telemetry:** The app contains NO Crashlytics, NO Google Analytics, and NO third-party ad trackers.
* **Strictly Local:** This app makes zero network requests. Everything executes securely on-device.

## 🔐 Why We Need Permissions

To function properly, Shield requires the following permissions. We believe in total transparency about how they are used:

* `RECEIVE_SMS` & `READ_SMS`: Required to intercept incoming messages and apply your custom Auto-Responder and filtering rules.
* `READ_CALL_LOG` & `READ_CONTACTS`: Required to identify VIP contacts so they can bypass Ghost Mode, and to block unknown/spam callers.
* `ANSWER_PHONE_CALLS`: Used by our local threat engine to instantly reject identified spam calls.

## 🛠️ Build it Yourself

```bash
git clone https://github.com/karanraj-ux/Goohle-studio-apk.git
cd Goohle-studio-apk
# Open in Android Studio, sync Gradle, and run.
```

---
*Shield is licensed under the MIT License.*
