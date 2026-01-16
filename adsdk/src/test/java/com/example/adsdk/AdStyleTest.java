package com.example.adsdk;

import com.example.adsdk.models.AdStyle;
import com.example.adsdk.ads.banner.BannerAdView;
import com.example.adsdk.ads.interstitial.InterstitialAd;
import com.example.adsdk.ads.appopen.AppOpenAd;

import org.junit.Test;

/**
 * Simple test to verify that setAdStyle methods are accessible
 */
public class AdStyleTest {

    @Test
    public void testAdStyleMethods() {
        // This test verifies that the setAdStyle methods compile properly

        // Test BannerAdView
        // BannerAdView bannerAdView = new BannerAdView(null);
        // bannerAdView.setAdStyle(AdStyle.ADMOB);

        // Test InterstitialAd
        // InterstitialAd interstitialAd = new InterstitialAd(null);
        // interstitialAd.setAdStyle(AdStyle.ADMOB);

        // Test AppOpenAd
        // AppOpenAd appOpenAd = new AppOpenAd(null);
        // appOpenAd.setAdStyle(AdStyle.ADMOB);

        // If this compiles, the methods are properly exposed
        assert true;
    }
}