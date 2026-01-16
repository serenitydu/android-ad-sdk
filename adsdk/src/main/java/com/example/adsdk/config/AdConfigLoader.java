package com.example.adsdk.config;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.adsdk.models.AdContent;
import com.example.adsdk.models.AdFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Loads and parses ad configuration from JSON file
 */
public class AdConfigLoader {

    private static final String CONFIG_FILE = "attack_pattern.json";

    /**
     * Load ad configuration from assets
     *
     * @param context Application context
     * @return AdConfig object or null if loading fails
     */
    @Nullable
    public static AdConfig loadFromAssets(@NonNull Context context) {
        try {
            String jsonString = loadJsonFromAssets(context, CONFIG_FILE);
            return parseConfig(jsonString);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load app-specific attack content based on package name
     *
     * @param context Application context
     * @param packageName Package name of the current app
     * @return AdContent for the specific app or null if not found
     */
    @Nullable
    public static AdContent loadAppSpecificContent(@NonNull Context context, @NonNull String packageName) {
        try {
            android.util.Log.d("AdConfigLoader", "Looking for app-specific content for package: " + packageName);
            String jsonString = loadJsonFromAssets(context, CONFIG_FILE);
            JSONObject root = new JSONObject(jsonString);

            if (root.has("appSpecificAttacks")) {
                org.json.JSONArray attacksArray = root.getJSONArray("appSpecificAttacks");
                android.util.Log.d("AdConfigLoader", "Found " + attacksArray.length() + " app-specific attacks");

                for (int i = 0; i < attacksArray.length(); i++) {
                    JSONObject attackObj = attacksArray.getJSONObject(i);
                    String appPackageName = attackObj.getString("packageName");
                    android.util.Log.d("AdConfigLoader", "Comparing '" + packageName + "' with '" + appPackageName + "'");

                    if (packageName.equals(appPackageName)) {
                        android.util.Log.d("AdConfigLoader", "MATCH FOUND! Loading app-specific content for: " + packageName);
                        return parseAdContent(attackObj);
                    }
                }
                android.util.Log.d("AdConfigLoader", "No match found for package: " + packageName);
            } else {
                android.util.Log.d("AdConfigLoader", "No appSpecificAttacks array found in JSON");
            }

            return null;
        } catch (IOException | JSONException e) {
            android.util.Log.e("AdConfigLoader", "Error loading app-specific content", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Read JSON file from assets
     */
    @NonNull
    private static String loadJsonFromAssets(@NonNull Context context, @NonNull String filename) throws IOException {
        InputStream inputStream = context.getAssets().open(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    /**
     * Parse JSON string into AdConfig object
     */
    @NonNull
    private static AdConfig parseConfig(@NonNull String jsonString) throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        AdConfig.Builder builder = new AdConfig.Builder();

        // Parse attack patterns array - supports both JSON structures
        org.json.JSONArray attacksArray = null;
        if (root.has("attackPatterns")) {
            // Pattern-based attacks from attack_pattern.json
            attacksArray = root.getJSONArray("attackPatterns");
        } else if (root.has("appSpecificAttacks")) {
            // App-specific attacks from attack_app.json
            attacksArray = root.getJSONArray("appSpecificAttacks");
        }

        if (attacksArray != null) {
            java.util.List<AdConfig.AdVariant> patterns = new java.util.ArrayList<>();

            for (int i = 0; i < attacksArray.length(); i++) {
                JSONObject attackObj = attacksArray.getJSONObject(i);
                String id = attackObj.getString("id");
                String name = attackObj.getString("name");
                AdContent content = parseAdContent(attackObj);

                if (content != null) {
                    patterns.add(new AdConfig.AdVariant(id, name, content));
                }
            }

            if (!patterns.isEmpty()) {
                builder.setAttackPatterns(patterns);
            }
        }

        // Parse defaults object
        if (root.has("defaults")) {
            JSONObject defaultsObj = root.getJSONObject("defaults");

            // Parse banner default
            if (defaultsObj.has("banner")) {
                JSONObject bannerObj = defaultsObj.getJSONObject("banner");
                AdContent bannerContent = parseAdContent(bannerObj);
                if (bannerContent != null) {
                    builder.setBannerDefaultContent(bannerContent);
                }
            }

            // Parse interstitial default
            if (defaultsObj.has("interstitial")) {
                JSONObject interstitialObj = defaultsObj.getJSONObject("interstitial");
                AdContent interstitialContent = parseAdContent(interstitialObj);
                if (interstitialContent != null) {
                    builder.setInterstitialDefaultContent(interstitialContent);
                }
            }

            // Parse app-open default
            if (defaultsObj.has("appOpen")) {
                JSONObject appOpenObj = defaultsObj.getJSONObject("appOpen");
                AdContent appOpenContent = parseAdContent(appOpenObj);
                if (appOpenContent != null) {
                    builder.setAppOpenDefaultContent(appOpenContent);
                }
            }
        }

        return builder.build();
    }

    /**
     * Parse individual ad content based on format
     */
    @Nullable
    private static AdContent parseAdContent(@NonNull JSONObject adObj) throws JSONException {
        String formatStr = adObj.getString("format");
        AdFormat format = AdFormat.valueOf(formatStr.toUpperCase());
        JSONObject contentObj = adObj.getJSONObject("content");

        switch (format) {
            case TEXT:
                return parseTextContent(contentObj);
            case IMAGE:
                return parseImageContent(contentObj);
            case HTML:
                return parseHtmlContent(contentObj);
            default:
                return null;
        }
    }

    /**
     * Parse text ad content
     */
    @NonNull
    private static AdContent.TextAdContent parseTextContent(@NonNull JSONObject contentObj) throws JSONException {
        String text = contentObj.getString("text");
        String backgroundColor = contentObj.getString("backgroundColor");
        String textColor = contentObj.getString("textColor");

        // Parse optional AdMob metadata
        String appName = contentObj.optString("appName", null);
        String appIcon = contentObj.optString("appIcon", null);
        Float rating = null;
        if (contentObj.has("rating")) {
            rating = (float) contentObj.getDouble("rating");
        }
        String price = contentObj.optString("price", null);
        String ctaText = contentObj.optString("ctaText", null);
        String appUrl = contentObj.optString("appUrl", null);

        return new AdContent.TextAdContent(text, backgroundColor, textColor,
                                          appName, appIcon, rating, price, ctaText, appUrl);
    }

    /**
     * Parse image ad content
     */
    @NonNull
    private static AdContent.ImageAdContent parseImageContent(@NonNull JSONObject contentObj) throws JSONException {
        String imageUrl = contentObj.getString("imageUrl");
        String clickUrl = contentObj.getString("clickUrl");
        return new AdContent.ImageAdContent(imageUrl, clickUrl);
    }

    /**
     * Parse HTML ad content
     */
    @NonNull
    private static AdContent.HtmlAdContent parseHtmlContent(@NonNull JSONObject contentObj) throws JSONException {
        String html = contentObj.getString("html");
        return new AdContent.HtmlAdContent(html);
    }
}
