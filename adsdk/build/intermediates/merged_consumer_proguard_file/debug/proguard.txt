# Consumer ProGuard rules for SDK users

# Keep SDK public API
-keep public class com.example.adsdk.AdSdk { *; }
-keep public class com.example.adsdk.config.AdSdkConfig { *; }
-keep public class com.example.adsdk.config.AdSdkConfig$Builder { *; }
-keep public class com.example.adsdk.ads.banner.BannerAdView { *; }
-keep public class com.example.adsdk.ads.interstitial.InterstitialAd { *; }
-keep public class com.example.adsdk.ads.appopen.AppOpenAd { *; }
-keep public enum com.example.adsdk.models.AdStyle { *; }
-keep public interface com.example.adsdk.listeners.** { *; }
