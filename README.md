# Android Ad SDK

An Android SDK for embedding banner, interstitial, and app-open ads used in authorized prompt-injection testing.

## Build the SDK

From this repository, build the release AAR:

```bash
./gradlew :adsdk:assembleRelease
```

The AAR is generated at:

```text
adsdk/build/outputs/aar/adsdk-release.aar
```

## Add the SDK to an Android app

Copy `adsdk-release.aar` into the app's `app/libs/` directory, then add it to the app module:

```kotlin
// Add the following code to the app/build.gradle.kts file.
dependencies {
    implementation(files("libs/adsdk-release.aar"))
}
```

The SDK requires Android API 21 or newer. Its manifest supplies the `INTERNET` and `ACCESS_NETWORK_STATE` permissions.

## Initialize the SDK

Initialize it once, normally in `Application.onCreate()`:

```kotlin
import com.example.adsdk.AdSdk
import com.example.adsdk.config.AdSdkConfig

val config = AdSdkConfig.Builder()
    .setClickTrackingEndpoint("https://example.com/track/click")
    .setLoggingEnabled(BuildConfig.DEBUG)
    .build()

AdSdk.initialize(this, config)
```

The click-tracking endpoint is required. If you use an `http://` endpoint during local development, allow cleartext traffic in the host app's manifest.

## Display an ad

Patterns are zero-indexed. Pattern `0` is the benign control; patterns `1` through `17` are test cases.

### App-open ad

```kotlin
import com.example.adsdk.ads.appopen.AppOpenAd

val ad = AppOpenAd(this)
    .setAttackPattern(0)

ad.loadAd(deviceId)
if (ad.isLoaded) {
    ad.show()
}
```

### Interstitial ad

```kotlin
import com.example.adsdk.ads.interstitial.InterstitialAd

val ad = InterstitialAd(this)
    .setAttackPattern(0)

ad.loadAd(deviceId)
if (ad.isLoaded) {
    ad.show()
}
```

### Banner ad

Add the view to a layout:

```xml
<com.example.adsdk.ads.banner.BannerAdView
    android:id="@+id/bannerAd"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Then load it from the activity or fragment:

```kotlin
import com.example.adsdk.ads.banner.BannerAdView

val banner = findViewById<BannerAdView>(R.id.bannerAd)
banner.setAttackPattern(0)
banner.loadAd(deviceId)
```

For instructions on creating and packaging additional test cases, see [Custom Test Cases](CUSTOM_TEST_CASES.md).

## Notice

Use this SDK only for research and testing with proper authorization.
