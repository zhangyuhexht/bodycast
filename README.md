# BodyCast

BodyCast takes the health data from your wearable and turns it into a short "body weather" report. It then gives you one small thing to do today, plus a suggestion for what to eat. No scores, no rankings, no calorie counts.

Version 22.61.0 (versionCode 88). Package name: `cn.pocket.toolbox`.

## Build it

You need JDK 21 and Android SDK Platform 37. Gradle comes with the wrapper, so there's nothing else to install.

1. Copy `local.properties.example` to `local.properties` and point `sdk.dir` at your own SDK. That file is gitignored.
2. Build and install:

```bash
./gradlew :app:assembleRelease --console=plain
adb install -r app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the usual debug keystore so it can be sideloaded. Replace it with a real release key before shipping anywhere official.

Unit tests:

```bash
./gradlew :app:test
```

Leave `android.builtInKotlin=false` in `gradle.properties`. AGP's bundled Kotlin compiler can't read Backdrop's metadata.

## Where things live

```
app/src/main/java/cn/pocket/toolbox/
  ui/           MainActivity, privacy page, Mi login, the four tabs, shared controls
  bodycast/     rule engine, store, weather/signal models, actions, recipes, echo data
  glass/        liquid glass components, theme, runtime fallback
  i18n/         Simplified Chinese / English strings and a missing-translation audit
  data/         Health Connect, Mi Cloud and manual input sources
  anim/         animation helpers
  notify/       notification code
  widget/       home screen widget code
app/src/test/    JVM unit tests
tools/           adb / emulator smoke helpers (set ANDROID_HOME or SDK first)
tools-generation/  scripts for generating docs and the icon
design-system/     design notes
```

Data comes in from a vendor health app through Health Connect. Mi Cloud is an optional extra source. The app never connects to a band over Bluetooth.

## About this copy

This is the clean Gradle project pulled out of the 22.61.0 source delivery. The APKs, website files, old branches, legacy code, the original author's `local.properties` and the stale docs were left out. The README in that delivery still said 22.49.0, which is why it isn't here.

Package names and imports were left untouched. The hardcoded local paths in the helper scripts now fall back to `ANDROID_HOME` / `ANDROID_SDK_ROOT`.

## License

GPL-3.0. Full text in `LICENSE`. Third-party Apache-2.0 and MIT code keeps its original notices — see `THIRD_PARTY_NOTICES.md` and `LICENSE-APACHE-2.0`.
