package com.example.adsdk.tracking;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single ad click event.
 * Contains all the information needed to track a user's ad interaction.
 */
public class ClickEvent {

    private final String adId;
    private final String adType; // "banner", "interstitial", or "appopen"
    private final long timestamp;
    private final String deviceId;
    private final JSONObject additionalData;

    private ClickEvent(Builder builder) {
        this.adId = builder.adId;
        this.adType = builder.adType;
        this.timestamp = builder.timestamp;
        this.deviceId = builder.deviceId;
        this.additionalData = builder.additionalData;
    }

    /**
     * @return The unique identifier for this ad
     */
    @NonNull
    public String getAdId() {
        return adId;
    }

    /**
     * @return The type of ad (banner, interstitial, appopen)
     */
    @NonNull
    public String getAdType() {
        return adType;
    }

    /**
     * @return Unix timestamp in milliseconds when the click occurred
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return Device or user identifier
     */
    @NonNull
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @return Additional custom data as JSON object
     */
    public JSONObject getAdditionalData() {
        return additionalData;
    }

    /**
     * Converts this click event to a JSON object for network transmission.
     *
     * @return JSON representation of the click event
     */
    @NonNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("ad_id", adId);
            json.put("ad_type", adType);
            json.put("timestamp", timestamp);
            json.put("device_id", deviceId);

            if (additionalData != null && additionalData.length() > 0) {
                json.put("additional_data", additionalData);
            }
        } catch (JSONException e) {
            // Should not happen with simple data types
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Builder for creating ClickEvent instances
     */
    public static class Builder {
        private String adId;
        private String adType;
        private long timestamp;
        private String deviceId;
        private JSONObject additionalData;

        public Builder() {
            this.timestamp = System.currentTimeMillis();
            this.additionalData = new JSONObject();
        }

        /**
         * Sets the ad ID (required)
         *
         * @param adId Unique identifier for the ad
         * @return This builder instance
         */
        public Builder setAdId(@NonNull String adId) {
            this.adId = adId;
            return this;
        }

        /**
         * Sets the ad type (required)
         *
         * @param adType Type of ad: "banner", "interstitial", or "appopen"
         * @return This builder instance
         */
        public Builder setAdType(@NonNull String adType) {
            this.adType = adType;
            return this;
        }

        /**
         * Sets the timestamp (optional, defaults to current time)
         *
         * @param timestamp Unix timestamp in milliseconds
         * @return This builder instance
         */
        public Builder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the device ID (required)
         *
         * @param deviceId Device or user identifier
         * @return This builder instance
         */
        public Builder setDeviceId(@NonNull String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        /**
         * Adds additional custom data
         *
         * @param key The key
         * @param value The value
         * @return This builder instance
         */
        public Builder addAdditionalData(@NonNull String key, @NonNull Object value) {
            try {
                this.additionalData.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return this;
        }

        /**
         * Builds the ClickEvent
         *
         * @return The configured ClickEvent
         * @throws IllegalStateException if required fields are not set
         */
        public ClickEvent build() {
            if (adId == null || adId.isEmpty()) {
                throw new IllegalStateException("Ad ID is required");
            }
            if (adType == null || adType.isEmpty()) {
                throw new IllegalStateException("Ad type is required");
            }
            if (deviceId == null || deviceId.isEmpty()) {
                throw new IllegalStateException("Device ID is required");
            }
            return new ClickEvent(this);
        }
    }
}
