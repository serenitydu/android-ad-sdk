# Custom Test Cases

Test cases are stored in [`adsdk/src/main/assets/attack_pattern.json`](adsdk/src/main/assets/attack_pattern.json) and packaged into the SDK's AAR at build time.

## Add a text test case

Append an object to the `attackPatterns` array:

```json
{
  "id": "pattern_18_custom_example",
  "name": "Custom Example",
  "format": "text",
  "content": {
    "text": "Your test prompt goes here.",
    "backgroundColor": "#263238",
    "textColor": "#FFFFFF",
    "appName": "Notepad - Notes & To Do List",
    "appIcon": "notepad.png",
    "rating": 4.5,
    "price": "FREE",
    "ctaText": "CLICK",
    "appUrl": "https://example.com"
  }
}
```

The fields are:

| Field | Required | Description |
|---|---|---|
| `id` | Yes | A unique identifier for the test case. |
| `name` | Yes | A readable name used in logs and tracking metadata. |
| `format` | Yes | Use `text` for a text-based test case. |
| `content.text` | Yes | The text displayed in the ad. |
| `content.backgroundColor` | Yes | Background color in Android-compatible color notation. |
| `content.textColor` | Yes | Text color in Android-compatible color notation. |
| `content.appName` | No | App name displayed by the AdMob-style renderer. |
| `content.appIcon` | No | Icon filename from `adsdk/src/main/assets/`. |
| `content.rating` | No | Numeric rating displayed with the ad. |
| `content.price` | No | Price label, such as `FREE`. |
| `content.ctaText` | No | Call-to-action button text. |
| `content.appUrl` | No | Destination opened when the ad is clicked. |

Keep each `id` unique. The object's position in `attackPatterns` becomes its zero-based pattern index, so appending a case preserves all existing indexes.

Do not edit copies under `adsdk/build/`; Gradle regenerates those files.


## Rebuild and embed the customized SDK

Build a new AAR after changing the configuration:

```bash
./gradlew :adsdk:assembleRelease
```

Replace the host app's existing `app/libs/adsdk-release.aar` with the newly generated file from `adsdk/build/outputs/aar/`.

Select the new case by its array index before calling `loadAd()`:

```kotlin
val customPatternIndex = 18

val ad = AppOpenAd(this)
    .setAttackPattern(customPatternIndex)

ad.loadAd(deviceId)
```

The same `setAttackPattern(index)` method is available on `AppOpenAd`, `InterstitialAd`, and `BannerAdView`.

## Troubleshooting

- If the case is missing, confirm that you rebuilt the AAR and replaced the copy in the host app.
- If no content appears, validate the JSON and check that all required fields are present.
- If an icon is missing, confirm that its filename exactly matches an asset in `adsdk/src/main/assets/`.
- Enable SDK logging with `.setLoggingEnabled(true)` to inspect loading and selection messages in Logcat.
