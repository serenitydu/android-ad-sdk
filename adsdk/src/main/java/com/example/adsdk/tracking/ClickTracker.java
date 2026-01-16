package com.example.adsdk.tracking;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.adsdk.config.AdSdkConfig;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles asynchronous tracking of ad click events.
 * Sends immediate HTTP POST requests to the backend for each click.
 * All network operations run on a background thread to avoid blocking the UI.
 */
public class ClickTracker {

    private static final String TAG = "ClickTracker";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

    private final AdSdkConfig config;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    /**
     * Creates a new ClickTracker instance.
     *
     * @param config SDK configuration
     */
    public ClickTracker(@NonNull AdSdkConfig config) {
        this.config = config;
        // Single-threaded executor to ensure sequential processing if needed
        // but can be changed to a cached thread pool for more parallelism
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Tracks a click event by immediately sending it to the backend.
     * This method returns immediately; the HTTP request happens asynchronously.
     *
     * @param event The click event to track
     */
    public void trackClick(@NonNull final ClickEvent event) {
        trackClick(event, null);
    }

    /**
     * Tracks a click event by immediately sending it to the backend.
     * This method returns immediately; the HTTP request happens asynchronously.
     *
     * @param event The click event to track
     * @param callback Optional callback for success/failure (called on main thread)
     */
    public void trackClick(@NonNull final ClickEvent event, final TrackingCallback callback) {
        if (event == null) {
            logError("Cannot track null click event");
            if (callback != null) {
                notifyFailure(callback, new IllegalArgumentException("Click event is null"));
            }
            return;
        }

        log("Tracking click event for ad: " + event.getAdId() + " (" + event.getAdType() + ")");

        // Execute network call on background thread
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                sendClickEvent(event, callback);
            }
        });
    }

    /**
     * Sends the click event via HTTP POST to the backend.
     * This runs on a background thread.
     *
     * @param event The click event to send
     * @param callback Optional callback for result
     */
    private void sendClickEvent(@NonNull ClickEvent event, TrackingCallback callback) {
        HttpURLConnection connection = null;
        try {
            // Create URL and connection
            URL url = new URL(config.getClickTrackingEndpoint());
            connection = (HttpURLConnection) url.openConnection();

            // Configure connection
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", CONTENT_TYPE_JSON);
            connection.setRequestProperty("Accept", CONTENT_TYPE_JSON);
            connection.setConnectTimeout(config.getConnectionTimeoutMs());
            connection.setReadTimeout(config.getReadTimeoutMs());
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Convert event to JSON
            JSONObject jsonPayload = event.toJson();
            byte[] jsonBytes = jsonPayload.toString().getBytes(StandardCharsets.UTF_8);

            // Send request
            connection.setFixedLengthStreamingMode(jsonBytes.length);
            try (OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {
                outputStream.write(jsonBytes);
                outputStream.flush();
            }

            // Get response code
            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                // Success
                log("Click event sent successfully. Response code: " + responseCode);

                // Read response body (optional)
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    log("Server response: " + response.toString());
                }

                if (callback != null) {
                    notifySuccess(callback);
                }
            } else {
                // Error response
                String errorMessage = "Server returned error code: " + responseCode;
                logError(errorMessage);

                // Try to read error response
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    logError("Error response: " + errorResponse.toString());
                } catch (Exception e) {
                    // Ignore error reading error response
                }

                if (callback != null) {
                    notifyFailure(callback, new Exception(errorMessage));
                }
            }

        } catch (Exception e) {
            logError("Failed to send click event: " + e.getMessage());
            e.printStackTrace();

            if (callback != null) {
                notifyFailure(callback, e);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Notifies success on the main thread
     */
    private void notifySuccess(final TrackingCallback callback) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess();
            }
        });
    }

    /**
     * Notifies failure on the main thread
     */
    private void notifyFailure(final TrackingCallback callback, final Exception error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Logs a message if logging is enabled
     */
    private void log(String message) {
        if (config.isLoggingEnabled()) {
            Log.d(TAG, message);
        }
    }

    /**
     * Logs an error message if logging is enabled
     */
    private void logError(String message) {
        if (config.isLoggingEnabled()) {
            Log.e(TAG, message);
        }
    }

    /**
     * Shuts down the executor service.
     * Should be called when the SDK is no longer needed (e.g., app shutdown).
     */
    public void shutdown() {
        executorService.shutdown();
        log("ClickTracker shut down");
    }

    /**
     * Callback interface for tracking results
     */
    public interface TrackingCallback {
        /**
         * Called when the click event was successfully sent to the backend
         */
        void onSuccess();

        /**
         * Called when the click event failed to send
         *
         * @param error The error that occurred
         */
        void onFailure(Exception error);
    }
}
