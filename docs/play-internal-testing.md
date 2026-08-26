# Google Play internal testing (phone + Wear)

Versioning for **both** `:mobile` and `:wear` comes from the repo root [`gradle.properties`](../gradle.properties): `calling.versionCode` and `calling.versionName`. Every Play upload needs a **higher `versionCode`** than the last one on that listing.

## 1. Release signing (required for Play Console)

Without `keystore.properties`, release builds use the **debug** keystore and are fine for sideloading only. For Play:

1. Copy [`keystore.properties.example`](../keystore.properties.example) to `keystore.properties` at the repo root.
2. Set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`.
3. Re-run the bundle tasks below so the AAB is signed with your upload key.

## 2. Build AABs (`storeSafe` = Play `applicationId` `dev.arpan.calling`)

```bash
./gradlew :mobile:bundleStoreSafeRelease :wear:bundleStoreSafeRelease
```

Artifacts (typical paths):

| Module | Output |
|--------|--------|
| Phone | `mobile/build/outputs/bundle/storeSafeRelease/mobile-storeSafe-release.aab` |
| Wear | `wear/build/outputs/bundle/storeSafeRelease/wear-storeSafe-release.aab` |

Use **`personal`** only if your Play listing uses `dev.arpan.calling.personal` (`bundlePersonalRelease` for each module).

## 3. Play Console — internal testing track

1. Open [Google Play Console](https://play.google.com/console/) → your app.
2. **Testing** → **Internal testing** (or **Closed testing**).
3. Create or select a **release**, then **Create new release**.
4. Upload the **phone** AAB first (primary artifact). If Wear is a separate multi-APK/bundle in the same app, add the **Wear** AAB per Console prompts (Wear OS / device-specific bundles).
5. Add **release notes**, **review** and **roll out** to internal testers.
6. Under **Testers**, add Google accounts or a Google Group; share the **opt-in URL** so testers install from the Play Store (not sideload).

## 4. Checklist before each upload

- [ ] Bump `calling.versionCode` (and optionally `calling.versionName`) in `gradle.properties`.
- [ ] Rebuild `bundleStoreSafeRelease` for every artifact you ship in that release.
- [ ] Confirm `keystore.properties` is present for production signing (never commit it).
