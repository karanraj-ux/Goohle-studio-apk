# ⏸️ Pause
> **The autonomous, offline call gatekeeper & missed-call SMS auto-responder for Android.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Zero Telemetry](https://img.shields.io/badge/Telemetry-None-brightgreen.svg)](#privacy--security-model)
[![100% Offline](https://img.shields.io/badge/Network-100%25%20Offline-orange.svg)](#architecture)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](#)

---

## 🛑 The Problem

1. **The Do Not Disturb Dilemma:** Stock Android DND is an all-or-nothing hammer. If you mute everything, your family cannot reach you in a true emergency. If you keep the ringer on, telemarketers and random pings shatter your focus and sleep.
2. **Missing Important Calls While Busy:** When you can't pick up because you are driving, in class, or in deep work, people keep calling repeatedly because they have no idea why you aren't answering.
3. **The Cloud Surveillance Trap:** Commercial call blockers (Truecaller, cloud screeners) demand your entire contact book, upload your private call logs, read your OTPs, and sell your network to data brokers.

---

## 💡 The Solution

**Pause** gives you total, sovereign control over your incoming communications. It operates as an **on-device personal gatekeeper**:

* **Automated Missed-Call & Busy SMS:** Sends intelligent, tier-based text replies instantly when you can't pick up (with support for multi-part messages, location links, and file URLs).
* **Emergency DND Bypass:** Lets your starred VIPs break through quiet hours while dropping or muting low-priority distractions.
* **One-Tap Desktop Controls:** Home screen widgets to instantly toggle Ghost Mode, kill all rules with a Master Switch, or trigger a **1-Hour Temporary Pause**.
* **Zero Cloud. Zero Servers:** Runs 100% on your device silicon with hardware-level AES-256 database encryption.

---

## ⚡ Core Features & Strengths

### 📱 1. Missed Call & Busy Auto-Responder
* **Multi-Tier Rules:**
  * **Inner Circle (VIPs):** Friendly note informing them you are occupied, with an emergency keyword override.
  * **Standard Contacts:** Polite status update (e.g., *"In deep work until 4 PM. Will get back to you shortly"*).
  * **Unknown Numbers:** Clear boundary notice filtering out unsolicited callers.
* **Media & Link Support:** Multipart SMS engine cleanly sends long messages, Google Drive links, map pins, or document URLs without truncation.

### 🌙 2. Scheduled DND & Sleep Mode
* Set automatic quiet windows (e.g., 10:00 PM – 7:00 AM).
* **Urgent Repeat-Caller Detection:** If someone calls multiple times in a short window during an emergency, Pause recognizes the pattern and sounds the ringer.

### ⏰ 3. Scheduled Messages & Tasks
* Schedule SMS messages to be sent at specific dates, times, or recurring intervals directly from your phone's SIM card without third-party scheduling servers.

### 🎛️ 4. Quick Action Home Screen Widgets
* **Master Kill Switch:** Instantly disable all automation with a single tap.
* **Ghost Mode Toggle:** Quick-silence unknown callers before walking into a room.
* **1-Hour Pause:** Temporarily pause all blocking when expecting a food delivery or courier.
* **DND Bypass Toggle:** Quick switch to allow or block bypass ringtones.

### 📅 5. Optional Calendar Context Sync
* Queries your local on-device `CalendarContract` offline to automatically engage Ghost Mode during scheduled busy events.

---

## 🔍 Radical Honesty: Design Decisions & Limitations

We believe in complete engineering transparency:

* **No "Fake AI" or Cloud LLMs:** Pause is a **deterministic, rule-based engine**. It executes the exact rules and templates you configure. This guarantees zero hallucinations, zero battery drain, and zero delay.
* **Standard SIM SMS:** Auto-replies are sent via your phone carrier's SIM card. Standard cellular SMS rates apply according to your carrier plan.
* **Local Calendar Cadence:** Due to Android OS battery optimization (Doze), background calendar polling runs via `WorkManager` on a ~15-minute cadence. For immediate manual control, always use the one-tap Home Screen Widgets.

---

## 🔒 Privacy & Security Model

* **AES-256 SQLCipher:** Rules, contact mappings, and logs are encrypted at rest using keys derived from the **Android Hardware Keystore**.
* **Zero Network Permission Required for Core Engine:** No telemetry SDKs, no Google Analytics, no Crashlytics, and no ad libraries.
* **F-Droid Compliant:** Contains zero proprietary binary blobs; fully open-source under the OSI-approved MIT License.

---

## 🛠️ Build it Yourself

```bash
# 1. Clone the repository
git clone <YOUR_REPOSITORY_URL>
cd <YOUR_REPOSITORY_DIR>

# 2. Build release APK with Gradle
./gradlew assembleRelease
```

### ⚙️ Customizing Repository & Project Metadata
All repository URLs, donation endpoints, and project handles are centrally configured in:
`app/src/main/res/values/strings.xml`

You can update `repo_url`, `github_sponsor_url`, and other details in that single file without modifying any Kotlin source code.

---

## 📄 License
Pause is released under the [MIT License](LICENSE).
