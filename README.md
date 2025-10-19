# Taison

Taison is a personal fork of [Mihon](https://github.com/mihonapp/mihon), the open source manga reader for Android.  It keeps pace with upstream while experimenting with quality-of-life tweaks and a refreshed visual identity.

## Taison Highlights

- **Library navigation** – category tabs are hidden by default, replaced with a quick category dropdown inside the toolbar. Tabs can still be re-enabled in settings.
- **History scoping** – optionally limit history entries to the active library category from *Settings › Library › Categories*.
- **Sunrise look** – a new warm Taison default theme; the legacy blue palette lives on as *Classic Blue*.
- **Rebranded identity** – updated application ID (`com.gent8.taison`), user agents, and in-app references to reflect the new fork.

## Download

Binary builds and changelogs are published on the [Gent8/Taison releases page](https://github.com/Gent8/Taison/releases).  If you are coming from older AmanoTeam builds (`com.amanoteam.taison`), install this fork fresh—automatic upgrades are not possible across package IDs.

## Building from Source

```bash
./gradlew assembleRelease
```

Artifacts are written to `app/build/outputs/apk/`.  Add `-Penable-updater` or other Gradle properties to opt into optional features defined under `buildSrc/src/main/kotlin/mihon/buildlogic/BuildConfig.kt`.

## Credits

This project would not exist without the Mihon community and everyone who contributes to upstream Tachiyomi forks.  Thank you!

## License

Taison is distributed under the [Apache License, Version 2.0](./LICENSE).
