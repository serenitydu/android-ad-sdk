package com.example.adsdk.listeners;

import androidx.annotation.NonNull;

/**
 * Listener interface for ad click events.
 * Implement this to receive notifications when a user clicks on an ad.
 */
public interface AdClickListener {

    /**
     * Called when the user clicks on an ad.
     *
     * @param adId The unique identifier of the clicked ad
     * @param adType The type of ad that was clicked (banner, interstitial, appopen)
     */
    void onAdClicked(@NonNull String adId, @NonNull String adType);
}
