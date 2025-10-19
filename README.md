<div align="center">
  <a href="#taison">
    <img src="app/src/main/res/mipmap/ic_launcher.png" alt="Taison logo" title="Taison logo" width="128"/>
  </a>

  # Taison <span style="color:#eab710;">App</span>
</div>

Taison is a personal fork of [Mihon](https://github.com/mihonapp/mihon), the open source manga reader for Android.  It keeps pace with upstream while experimenting with quality-of-life tweaks and a refreshed visual identity.

## Taison Highlights

- **Library navigation** – category tabs are hidden by default, replaced with a quick category dropdown inside the toolbar. Tabs can still be re-enabled in settings.
- **History scoping** – History entries are limited to the active library category from *Settings › Library › Categories*. This option can also be turned off in settings.
- **Taison look** – a warm default palette unique to this fork; the legacy blue palette lives on as *Classic Blue*.

## Download

Changelogs are published on the [Gent8/Taison releases page](https://github.com/Gent8/Taison/releases).  

## Build

Make sure the Android 14 (API 34) SDK and command-line tools are installed.

```bash
./gradlew assembleRelease
```

Signed APKs land in `app/build/outputs/apk/release/`. Add `bundleRelease` if you need an `.aab`.

## Releasing

- Update `versionCode`/`versionName` in `app/build.gradle.kts` and summarize the changes in `CHANGELOG.md`.
- Generate a signed `release` build with your keystore (`./gradlew assembleRelease` after configuring `signingConfigs`, or Android Studio’s **Build › Generate Signed Bundle / APK**).
- Commit the release changes, tag them (e.g. `git tag v1.0.0 && git push --tags`), then draft a GitHub release referencing the changelog entry.
- Attach the signed artifacts to the release and verify the APK installs before hitting publish.

## License

Taison is distributed under the [Apache License, Version 2.0](./LICENSE).
