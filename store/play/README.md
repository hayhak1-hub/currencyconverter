# Play Store materials — Currency Converter

**Package:** `com.hayhak.currencyConverter`  
**Release AAB:** `app/build/outputs/bundle/release/currencyconverter-release.aab`

## Listing copy

| Locale | File |
|--------|------|
| English (default) | [listing-en.txt](listing-en.txt) |
| Deutsch | [listing-de.txt](listing-de.txt) |
| Türkçe | [listing-tr.txt](listing-tr.txt) |

## Versionshinweise (release notes)

Paste [release-notes.xml](release-notes.xml) into Play Console → Release → **Versionshinweise** (all 20 app languages).

| Tag | File |
|-----|------|
| en-US | [whatsnew/en-US.txt](whatsnew/en-US.txt) |
| de-DE | [whatsnew/de-DE.txt](whatsnew/de-DE.txt) |
| tr-TR | [whatsnew/tr-TR.txt](whatsnew/tr-TR.txt) |

## Required assets checklist

- [x] **App icon** — 512×512 PNG → `store/play/app-icon-512.png`
- [x] **Feature graphic** — 1024×500 PNG → `store/play/feature-graphic-1024x500.png`
- [ ] **Phone screenshots** — min 2, max 8 (1080×1920 or 9:16)
  - Converter screen
  - Rates / dashboard
  - History chart
  - Settings / dark theme (optional)
- [ ] **7-inch tablet** — optional but recommended (at least 1)
- [x] **Privacy policy URL** — https://hayhak1-hub.github.io/currencyconverter/datenschutz.html
  - English: https://hayhak1-hub.github.io/currencyconverter/privacy-policy.html
  - Türkçe: https://hayhak1-hub.github.io/currencyconverter/gizlilik.html
- [ ] **Data safety form** — declare: Internet (rates), optional notifications (alerts), no account, no ads
- [ ] **Content rating** — IARC questionnaire (Finance, no user-generated content)
- [ ] **Target audience** — 13+ or general audience

## Upload steps

1. Build signed bundle: `./gradlew bundleRelease`
2. Play Console → app `com.hayhak.currencyConverter` → **Create new release** (versionCode must be higher than the last uploaded one)
3. Upload AAB to **Production** or **Internal testing**
4. Paste listing text from locale files
5. Complete Data safety + Content rating before rollout

## Keystore

Signing config reads `keystore/keystore.properties` (gitignored).  
Template: `keystore/keystore.properties.example`

**Back up** `keystore/currencyconverter-upload.jks` and passwords securely — required for all future updates.
