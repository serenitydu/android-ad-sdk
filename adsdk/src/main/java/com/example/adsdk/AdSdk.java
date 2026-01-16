package com.example.adsdk;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.adsdk.config.AdConfig;
import com.example.adsdk.config.AdConfigLoader;
import com.example.adsdk.config.AdSdkConfig;
import com.example.adsdk.tracking.ClickTracker;

/**
 * Main entry point for the Ad SDK.
 * Must be initialized before using any ad views.
 *
 * Usage:
 * <pre>
 * AdSdkConfig config = new AdSdkConfig.Builder()
 *     .setClickTrackingEndpoint("https://api.example.com/click")
 *     .setLoggingEnabled(true)
 *     .build();
 *
 * AdSdk.initialize(context, config);
 * </pre>
 */
public class AdSdk {

    private static final String TAG = "AdSdk";
    private static AdSdk instance;

    private final Context applicationContext;
    private final AdSdkConfig config;
    private final ClickTracker clickTracker;
    private final AdConfig adConfig;
    private boolean initialized = false;

    private AdSdk(@NonNull Context context, @NonNull AdSdkConfig config) {
        this.applicationContext = context.getApplicationContext();
        this.config = config;
        this.clickTracker = new ClickTracker(config);

        // Load ad configuration from assets
        this.adConfig = AdConfigLoader.loadFromAssets(context);
        if (this.adConfig == null && config.isLoggingEnabled()) {
            Log.w(TAG, "Failed to load ad config from assets, ads will use default content");
        }

        this.initialized = true;
    }

    /**
     * Initializes the Ad SDK with the given configuration.
     * This must be called before using any ad views.
     *
     * @param context Application or Activity context
     * @param config SDK configuration
     * @throws IllegalArgumentException if context or config is null
     */
    public static synchronized void initialize(@NonNull Context context, @NonNull AdSdkConfig config) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("AdSdkConfig cannot be null");
        }

        if (instance == null) {
            instance = new AdSdk(context, config);
            if (config.isLoggingEnabled()) {
                Log.d(TAG, "Ad SDK initialized successfully");
            }
        } else {
            if (config.isLoggingEnabled()) {
                Log.w(TAG, "Ad SDK already initialized, ignoring duplicate initialization");
            }
        }
    }

    /**
     * Gets the singleton instance of the SDK.
     *
     * @return The SDK instance
     * @throws IllegalStateException if SDK has not been initialized
     */
    @NonNull
    public static synchronized AdSdk getInstance() {
        if (instance == null || !instance.initialized) {
            throw new IllegalStateException(
                "AdSdk is not initialized. Call AdSdk.initialize() first."
            );
        }
        return instance;
    }

    /**
     * Checks if the SDK has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static synchronized boolean isInitialized() {
        return instance != null && instance.initialized;
    }

    /**
     * Gets the application context.
     *
     * @return Application context
     */
    @NonNull
    public Context getApplicationContext() {
        return applicationContext;
    }

    /**
     * Gets the SDK configuration.
     *
     * @return The configuration
     */
    @NonNull
    public AdSdkConfig getConfig() {
        return config;
    }

    /**
     * Gets the click tracker instance.
     *
     * @return The click tracker
     */
    @NonNull
    public ClickTracker getClickTracker() {
        return clickTracker;
    }

    /**
     * Gets the ad configuration loaded from assets.
     *
     * @return The ad configuration, or null if not loaded
     */
    @androidx.annotation.Nullable
    public AdConfig getAdConfig() {
        return adConfig;
    }

    /**
     * Gets a specific attack pattern by index.
     * Attack patterns can be used by any ad type (banner, interstitial, app-open).
     *
     * @param index The index of the attack pattern (0-9 for the 10 attack patterns)
     * @return The attack pattern, or null if not found
     */
    @androidx.annotation.Nullable
    public AdConfig.AdVariant getAttackPattern(int index) {
        if (adConfig != null) {
            return adConfig.getAttackPattern(index);
        }
        return null;
    }

    /**
     * Gets the total number of attack patterns available.
     *
     * @return Number of attack patterns
     */
    public int getAttackPatternCount() {
        if (adConfig != null) {
            return adConfig.getAttackPatterns().size();
        }
        return 0;
    }

    /**
     * Logs a message if logging is enabled.
     *
     * @param message The message to log
     */
    public void log(String message) {
        if (config.isLoggingEnabled()) {
            Log.d(TAG, message);
        }
    }

    /**
     * Logs an error message if logging is enabled.
     *
     * @param message The error message
     * @param throwable Optional throwable
     */
    public void logError(String message, Throwable throwable) {
        if (config.isLoggingEnabled()) {
            if (throwable != null) {
                Log.e(TAG, message, throwable);
            } else {
                Log.e(TAG, message);
            }
        }
    }
}
