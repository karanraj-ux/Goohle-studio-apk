# 🛡️ Shield

**A privacy-first, 100% offline SMS and Call automation engine.**

![Open Source](https://img.shields.io/badge/Open%20Source-100%25-green)
![No Telemetry](https://img.shields.io/badge/Telemetry-None-blue)
![Offline First](https://img.shields.io/badge/Offline-First-orange)

Most SMS forwarders and call blockers harvest your OTPs, upload your contacts, and sell your data. **Shield is built differently.** It is a hyper-secure automation engine that keeps your sensitive data exactly where it belongs: on your device.

## 🚀 Features
* **Ghost Mode (Call/SMS Filtering):** Automatically reject unknown numbers, block spam, and mute distractions without lifting a finger.
* **Smart Forwarding (Webhooks):** Forward important SMS messages (like OTPs or bank alerts) to your own custom endpoints (Discord, Slack, Zapier). *Strictly enforces HTTPS to prevent man-in-the-middle attacks.*
* **Local Data Retention:** Built-in auto-cleanup ensures your database stays lean, preventing memory crashes and bloated storage over time.

## 🔒 Security Architecture
We don't just promise privacy; we engineered it.
* **SQLCipher Encrypted Database:** Your SMS logs, rules, and configurations are secured at rest using AES-256-GCM encryption backed by the Android Hardware Keystore. Even if your device is rooted, your data is unreadable.
* **Zero Telemetry:** The app contains NO Crashlytics, NO Google Analytics, and NO third-party ad trackers.
* **Strictly Local:** Unless you manually configure an HTTPS Webhook, this app makes zero network requests.

## 🔐 Why We Need Permissions
To function properly, Shield requires the following permissions. We believe in total transparency about how they are used:
* `RECEIVE_SMS` & `READ_SMS`: Required to intercept incoming messages, extract OTPs, and apply your custom automation rules.
* `READ_CALL_LOG` & `READ_CONTACTS`: Required to identify VIP contacts so they can bypass Ghost Mode, and to block unknown/spam callers.
* `ANSWER_PHONE_CALLS`: Used by our local threat engine to instantly reject identified spam calls.

## 🛠️ Build it Yourself
```bash
git clone https://github.com/akhilesh844102/shield-forward.git
cd shield-forward
# Open in Android Studio, sync Gradle, and run.
```

## ❤️ Support & Donate
If Shield helps you take back your privacy, consider supporting the development!
* [Buy Me a Coffee](#)
* [LiberaPay](#)

---
*Shield is licensed under the MIT License.*
