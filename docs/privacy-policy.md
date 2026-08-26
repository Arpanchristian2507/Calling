# Privacy Policy for Calling

**Effective date:** 26 August 2026  
**Last updated:** 26 August 2026

This Privacy Policy describes how **Calling** (“the App”, “we”, “us”, or “our”) handles information when you use our Android phone app (`dev.arpan.calling`) and, if installed, the companion Wear OS app.

Calling lets you simulate fake incoming phone calls on your own device for personal use (for example, scheduling a call, choosing a caller name, photo, voice clip, and call-screen theme). **We do not operate user accounts, backend servers, advertising, or analytics in the App.**

By installing or using the App, you agree to this Privacy Policy. If you do not agree, please do not use the App.

---

## 1. Summary

| Topic | Our practice |
| --- | --- |
| Data sent to us (the developer) | **None.** We do not receive your data on our servers. |
| Data stored on your device | Caller names, optional photos/audio you choose, theme settings, and scheduled fake calls. |
| Data shared with third parties for our purposes | **None.** We do not sell or share your data. |
| Third-party libraries | Google Play services (Wearable) for optional watch-to-phone triggers only. |
| Ads | **None.** |
| Accounts | **None.** |

---

## 2. Information the App processes

### 2.1 Information you provide

When you use the App, you may enter or select:

- **Caller display name** — text you type for the fake incoming call screen.
- **Caller photo** — an image you pick from your device gallery; a copy is saved in the App’s private storage.
- **In-call voice audio** — an audio file you pick from your device; a copy is saved in the App’s private storage and played locally after you answer a fake call.
- **Call-screen background image** — an optional image you pick from your gallery for the fake call UI.
- **Theme and layout preferences** — your choices for incoming-call appearance (for example Samsung-style or OnePlus-style themes, gradient vs dark background).

This information is used **only** to show and play your fake calls on **your device**. We do not upload it to our servers.

### 2.2 Information stored automatically on your device

The App also stores locally:

- **Scheduled fake calls** — caller name and scheduled date/time so alarms can fire at the right moment.
- **App settings** — stored in the App’s private preferences files.

### 2.3 Information we do not collect

The App does **not** collect, access, or use:

- Real phone call logs, SMS, or contact lists
- Precise or coarse location
- Device identifiers for tracking (such as advertising ID, IMEI, or serial numbers)
- Microphone or camera recordings (except when **you** explicitly pick a photo or audio file from the system picker)
- Financial or payment information
- Health data
- Information about other apps installed on your device

---

## 3. Permissions and why they are used

The App requests only the permissions needed for its features:

| Permission | Purpose |
| --- | --- |
| **Notifications** (`POST_NOTIFICATIONS`) | Show an incoming-call notification when a fake call is triggered or scheduled. |
| **Vibrate** (`VIBRATE`) | Vibrate during an incoming fake call, similar to a real call. |
| **Full-screen intent** (`USE_FULL_SCREEN_INTENT`, personal build variant only) | Display the full-screen incoming-call UI over the lock screen when appropriate. |

The Wear OS companion app may use **Wake lock** and **Vibrate** so you can trigger a fake call from your watch.

Permissions are requested at runtime where Android requires it (for example notifications). The App does not use permissions for background tracking, marketing, or unrelated purposes.

---

## 4. Wear OS companion app

If you install the Wear OS companion and pair it with your phone:

- Trigger messages (including the caller name and optional delay you set on the watch) are sent **directly between your watch and phone** using [Google Play services Wearable APIs](https://developers.google.com/android/reference/com/google/android/gms/wearable/package-summary).
- Those messages are processed on your devices to start a fake call. **We do not receive them.**

Google’s handling of data processed by Play services is governed by [Google’s Privacy Policy](https://policies.google.com/privacy).

---

## 5. How we use information

We use the information described above solely to:

- Display fake incoming and active call screens
- Play optional custom in-call audio on your device
- Schedule and deliver fake calls at times you choose
- Remember your theme and layout preferences
- Send optional watch-to-phone trigger messages between your own devices

We **do not** use your information for advertising, profiling, or selling to third parties.

---

## 6. Sharing of information

**We do not sell, rent, or share your personal information with third parties for their marketing or commercial purposes.**

Limited sharing may occur only in these cases:

- **Google Play services (Wearable)** — device-to-device messages between your phone and watch, as described above.
- **Android backup** — if you enable backup on your device, the App’s local data may be included in encrypted backup to your Google account according to [Android backup rules](https://developer.android.com/guide/topics/data/backup). We do not control Google’s backup service.
- **Legal requirements** — if required by applicable law, regulation, legal process, or enforceable governmental request.

---

## 7. Data retention and deletion

All App data is stored **on your device** until you remove it.

You can delete your data at any time by:

- Removing caller photos, voice clips, or backgrounds using the controls in the App
- Cancelling scheduled fake calls in the App
- Clearing App storage: **Settings → Apps → Calling → Storage → Clear storage** (wording may vary by device)
- Uninstalling the App, which removes locally stored App data from the device

Because we do not operate accounts or cloud storage, there is no separate online account to delete.

---

## 8. Security

We store your chosen photos, audio, and settings in the App’s **private app storage**, which other apps cannot read without root access or device backup exports.

Wear trigger messages travel over Google’s Wearable channel between your paired devices. We do not operate our own servers for this data.

No method of electronic storage is 100% secure. You are responsible for securing your device and any content you choose to import into the App.

---

## 9. Children’s privacy

The App is **not directed at children under 13** (or the minimum age required in your country). We do not knowingly collect personal information from children. If you believe a child has provided information through the App, contact us and we will help you remove locally stored data by uninstalling the App or clearing its storage.

---

## 10. International users

The App is designed to process data **locally on your device**. If you use the App in the European Economic Area, United Kingdom, or other regions with privacy laws, you may have rights to access, correct, or delete personal data we process. Because we do not receive your data on our servers, those rights are exercised primarily by managing or deleting data on your device as described in Section 7.

---

## 11. Changes to this Privacy Policy

We may update this Privacy Policy from time to time. When we do, we will revise the “Last updated” date at the top. Material changes may also be noted in the App or on the Play Store listing. Continued use of the App after an update means you accept the revised policy.

---

## 12. Google Play Data safety

When completing the **Data safety** section in Google Play Console for Calling, you should declare practices consistent with this policy, including:

- Data is **not collected** by the developer from users’ devices
- Optional user-provided content (names, photos, audio) is **stored locally** for app functionality
- Data is **not shared** with third parties for developer purposes
- **No ads** and **no account** required

Always keep your Play Console declarations aligned with this document and the App’s actual behavior.

---

## 13. Contact us

If you have questions about this Privacy Policy or the App’s data practices, contact:

**Arpan Christian**  
Email: [arpanchristian2507@gmail.com](mailto:arpanchristian2507@gmail.com)  
GitHub: [github.com/Arpanchristian2507/Calling](https://github.com/Arpanchristian2507/Calling)

---

*This document is provided for transparency and Google Play policy compliance. It is not legal advice. Consult a qualified attorney if you need advice for your specific situation.*
