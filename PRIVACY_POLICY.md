# Privacy Policy

**Effective Date: August 2026**

Your privacy is our absolute priority. This policy outlines exactly how Shield handles your data.

## 1. Zero Data Collection
**Shield does not collect, harvest, upload, or sell your data.** 
The developer has zero access to your SMS messages, call logs, contacts, or automation rules. We have intentionally excluded all analytics, telemetry, and crash-reporting SDKs from the codebase.

## 2. Local Storage & Encryption
All data processed by Shield (including SMS logs, configurations, and contact rules) is stored entirely on your local device. 
* This local database is **encrypted at rest** using SQLCipher and your device's Hardware Keystore.
* The app automatically purges old logs to maintain optimal performance and limit data retention.

## 3. Third-Party Webhooks (Opt-In Only)
The only time Shield will transmit data over a network is if **you** explicitly configure an Advanced Integration (Webhook). 
* Webhooks are 100% opt-in.
* To protect your data in transit, Shield strictly enforces the use of HTTPS for all Webhook URLs.
* You are responsible for the privacy policies of the endpoints (e.g., Discord, Slack) to which you choose to forward your data.

## 4. Required Permissions
Shield requires permissions like `RECEIVE_SMS` and `READ_CALL_LOG` to function. These permissions are strictly used by the local, offline automation engine to filter calls and forward messages according to your customized rules. They are never used to upload data to external servers.

## 5. Contact
If you have any questions about this Privacy Policy, please open an issue in the GitHub repository.
