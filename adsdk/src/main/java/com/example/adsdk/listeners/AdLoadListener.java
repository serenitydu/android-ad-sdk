package com.example.adsdk.listeners;

import androidx.annotation.NonNull;

/**
 * Listener interface for ad loading events.
 * Implement this to receive notifications about ad loading status.
 */
public interface AdLoadListener {

    /**
     * Called when an ad is successfully loaded and ready to display.
     *
     * @param adId The unique identifier of the loaded ad
     */
    void onAdLoaded(@NonNull String adId);

    /**
     * Called when an ad fails to load.
     *
     * @param adId The unique identifier of the ad that failed to load
     * @param errorMessage Description of the error
     */
    void onAdFailedToLoad(@NonNull String adId, @NonNull String errorMessage);
}
