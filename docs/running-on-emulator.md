# Running on an emulator

## Phone app (`:mobile`)

1. **AVD:** Use a **phone** (not Wear) virtual device with **API 30+** (Android 11+). The project sets `minSdk = 30`; older images show install errors such as `INSTALL_FAILED_OLDER_SDK`.
2. **System image:** Prefer **Google Play** or **Google APIs** so **Google Play services** is available (needed for Wearable Data Layer if you test watch pairing later).
3. **Run configuration:** In Android Studio, choose the **`mobile`** module (e.g. `mobile` → `personalDebug` or `storeSafeDebug`), not `wear`.

## Wear app (`:wear`)

1. **AVD:** Use a **Wear OS** emulator (API **30+**). The manifest requires `android.hardware.type.watch`.
2. You **cannot** install or run the Wear module on a regular phone AVD; Play / `adb` will reject it or it will not appear as a valid target.

## If the app still does not install or start

- Confirm the **Run** dropdown targets **`mobile`** on a **phone** AVD (or **`wear`** on a **Wear** AVD).
- **Device Manager →** cold boot the AVD, or wipe data if the image is corrupted.
- For crashes after launch, capture **Logcat** for the process and stack trace.
