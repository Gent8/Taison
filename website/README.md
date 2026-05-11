# Taison share landing site

Static site backing Taison's direct entry sharing feature, served at
[`https://taison.gent8.com`](https://taison.gent8.com).

When a Taison user shares an entry, the app emits a link of the form
`https://taison.gent8.com/e/?s=…&t=…&u=…&c=…&d=…&a=…&g=…&st=…`. The browser
loads `e/index.html`, which decodes the payload, renders a preview (cover,
title, author, status, genres, description) and offers an **Open in Taison**
button. The button dispatches an `intent://` URL that hands off to the
installed app; recipients without the app land on the install page via the
intent's `browser_fallback_url`.

The wire format is defined by the Kotlin source in
`app/src/main/java/eu/kanade/tachiyomi/ui/deeplink/TaisonEntryLink.kt`.

## Layout

```
/                       redirect to the Taison GitHub repo
/e/                     landing page for shared entry links
/.well-known/assetlinks.json   Digital Asset Links file for App Link verification
/public/                shared assets (featureGraphic, app icon)
/CNAME                  GitHub Pages custom domain pin
/.nojekyll              disable Jekyll so /.well-known/ is not stripped
```

## Deployment

The site deploys automatically from [`.github/workflows/pages.yml`](../.github/workflows/pages.yml)
on every push to `develop` or `main` that touches `website/**`. GitHub Pages
in this repo is configured with **Source: GitHub Actions** and custom domain
`taison.gent8.com`.

## Updating the signing fingerprint

`.well-known/assetlinks.json` pins the SHA-256 fingerprint of the Taison
release signing certificate. If the keystore changes, regenerate with:

```bash
keytool -list -v -keystore <keystore> -alias <alias> | grep SHA256
```

and replace the value in `assetlinks.json`. App Link verification breaks
silently when the file and the installed app disagree.
