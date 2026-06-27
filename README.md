# Shield & Forward

Shield & Forward is an advanced, fully local Android application designed to keep you focused and protected from unwanted interruptions. With offline SMS forwarding, intelligent call shielding, and VIP divert capabilities, you get complete control over who reaches you and when. 

## Features
- **Local Engine (KJ AI):** Analyze SMS messages on-device for spam or suspicious patterns using a built-in neural categorization engine.
- **Auto SMS Forwarding:** Forward incoming SMS messages to an external number via an integrated webhook or SMS relay.
- **Smart Call Shielding:** Automatically muting or forwarding unrecognized (non-VIP) calls so you remain undisturbed.
- **VIP Call Management:** Maintain a list of VIP contacts who bypass all Do Not Disturb and forwarding rules.
- **Emergency DND Bypass:** If a number calls repeatedly within a short timeframe, it triggers an emergency alarm, alerting you of urgent situations even in silent mode.
- **Dual SIM Support:** Advanced dual SIM management with specific MMI codes to toggle forwarding.
- **Offline & Private:** All data, logs, and rules are kept strictly local.

## F-Droid Readiness
This application is designed to be fully open-source and respects user privacy.
- **No Trackers:** Contains no telemetry, no crashlytics, and no proprietary trackers.
- **Open Source:** Licensed under the MIT License.
- **Local First:** All machine learning and rule processing happens locally on the device using Room Database.

## Setup & Compilation
1. Clone this repository.
2. Open with Android Studio.
3. Build the project using `gradle assembleDebug` or `gradle assembleRelease`.
4. Ensure all permissions are granted on first launch (SMS, Call Logs, Phone State).

## Legal
Please see the [Privacy Policy](PRIVACY_POLICY.md) and [Terms of Conditions](TERMS.md) files for more details. 

## Contribution
Check out our [Contributing Guidelines](CONTRIBUTING.md) to see how you can get involved.
