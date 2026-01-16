package com.example.adsdk.config;

import androidx.annotation.NonNull;

/**
 * Configuration class for the Ad SDK.
 * Contains the backend endpoint URL and other settings.
 */
public class AdSdkConfig {

    private final String clickTrackingEndpoint;
    private final int connectionTimeoutMs;
    private final int readTimeoutMs;
    private final boolean loggingEnabled;

    private AdSdkConfig(Builder builder) {
        this.clickTrackingEndpoint = builder.clickTrackingEndpoint;
        this.connectionTimeoutMs = builder.connectionTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;
        this.loggingEnabled = builder.loggingEnabled;
    }

    /**
     * @return The backend endpoint URL for click tracking
     */
    @NonNull
    public String getClickTrackingEndpoint() {
        return clickTrackingEndpoint;
    }

    /**
     * @return Connection timeout in milliseconds
     */
    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    /**
     * @return Read timeout in milliseconds
     */
    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /**
     * @return Whether logging is enabled
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Builder pattern for creating AdSdkConfig instances
     */
    public static class Builder {
        private String clickTrackingEndpoint;
        private int connectionTimeoutMs = 10000; // Default 10 seconds
        private int readTimeoutMs = 10000; // Default 10 seconds
        private boolean loggingEnabled = false;

        /**
         * Sets the backend endpoint URL for click tracking (required)
         *
         * @param endpoint The full URL endpoint (e.g., "https://api.example.com/click")
         * @return This builder instance
         */
        public Builder setClickTrackingEndpoint(@NonNull String endpoint) {
            this.clickTrackingEndpoint = endpoint;
            return this;
        }

        /**
         * Sets the connection timeout
         *
         * @param timeoutMs Timeout in milliseconds
         * @return This builder instance
         */
        public Builder setConnectionTimeout(int timeoutMs) {
            this.connectionTimeoutMs = timeoutMs;
            return this;
        }

        /**
         * Sets the read timeout
         *
         * @param timeoutMs Timeout in milliseconds
         * @return This builder instance
         */
        public Builder setReadTimeout(int timeoutMs) {
            this.readTimeoutMs = timeoutMs;
            return this;
        }

        /**
         * Enables or disables SDK logging
         *
         * @param enabled Whether logging should be enabled
         * @return This builder instance
         */
        public Builder setLoggingEnabled(boolean enabled) {
            this.loggingEnabled = enabled;
            return this;
        }

        /**
         * Builds the AdSdkConfig instance
         *
         * @return The configured AdSdkConfig
         * @throws IllegalStateException if required fields are not set
         */
        public AdSdkConfig build() {
            if (clickTrackingEndpoint == null || clickTrackingEndpoint.isEmpty()) {
                throw new IllegalStateException("Click tracking endpoint is required");
            }
            return new AdSdkConfig(this);
        }
    }
}
