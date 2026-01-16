# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep SDK public API
-keep public class com.example.adsdk.AdSdk { *; }
-keep public class com.example.adsdk.config.AdSdkConfig { *; }
-keep public class com.example.adsdk.config.AdSdkConfig$Builder { *; }
-keep public class com.example.adsdk.ads.banner.BannerAdView { *; }
-keep public class com.example.adsdk.ads.interstitial.InterstitialAd { *; }
-keep public class com.example.adsdk.ads.appopen.AppOpenAd { *; }
-keep public enum com.example.adsdk.models.AdStyle { *; }
-keep public interface com.example.adsdk.listeners.** { *; }

# Keep tracking classes
-keep class com.example.adsdk.tracking.** { *; }

# Standard Android rules
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exception
