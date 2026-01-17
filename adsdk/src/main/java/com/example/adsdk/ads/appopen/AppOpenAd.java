package com.example.adsdk.ads.appopen;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.adsdk.AdSdk;
import com.example.adsdk.config.AdConfig;
import com.example.adsdk.listeners.AdClickListener;
import com.example.adsdk.models.AdFormat;
import com.example.adsdk.listeners.AdLoadListener;
import com.example.adsdk.models.AdContent;
import com.example.adsdk.models.AdStyle;
import com.example.adsdk.tracking.ClickEvent;

import java.util.UUID;

/**
 * An app-open ad that appears when the app is launched or resumed from background.
 * Similar to interstitial but designed specifically for app open events.
 * Automatically tracks clicks when the user taps the ad content.
 *
 * Usage:
 * <pre>
 * AppOpenAd appOpenAd = new AppOpenAd(activity);
 * appOpenAd.setAdClickListener(new AdClickListener() {
 *     @Override
 *     public void onAdClicked(@NonNull String adId, @NonNull String adType) {
 *         // Handle click
 *     }
 * });
 * appOpenAd.loadAd("your-device-id");
 *
 * // Later, when app is opened/resumed
 * if (appOpenAd.isLoaded()) {
 *     appOpenAd.show();
 * }
 * </pre>
 */
public class AppOpenAd {

    private static final String AD_TYPE = "appopen";

    private final Activity activity;
    private String adId;
    private String deviceId;
    private Dialog adDialog;
    private AdClickListener clickListener;
    private AdLoadListener loadListener;
    private boolean isLoaded = false;
    private boolean isShowing = false;
    private long loadTimeMs = 0;
    private static final long AD_EXPIRY_MS = 4 * 60 * 60 * 1000; // 4 hours
    private Integer specificPatternIndex = null; // Specific pattern to use, or null for default
    private AdStyle adStyle = AdStyle.ADMOB; // Default to ADMOB style

    /**
     * Creates a new app-open ad
     *
     * @param activity The activity context
     */
    public AppOpenAd(@NonNull Activity activity) {
        this.activity = activity;
        this.adId = "appopen_" + UUID.randomUUID().toString();
        AdSdk.getInstance().log("AppOpenAd: Created with default style = " + adStyle);
    }

    /**
     * Sets a specific attack pattern to use instead of the default app-open content.
     * Patterns are 0-indexed (0-10 for the 11 patterns: 0 benign + 10 attack patterns).
     *
     * @param patternIndex The index of the attack pattern to use (0-10)
     * @return This AppOpenAd instance for chaining
     */
    public AppOpenAd setAttackPattern(int patternIndex) {
        this.specificPatternIndex = patternIndex;
        return this;
    }

    /**
     * Sets the ad style (ADMOB only supported).
     * Must be called before loadAd().
     *
     * @param style The ad style to use (only AdStyle.ADMOB is supported)
     * @return This AppOpenAd instance for chaining
     */
    public AppOpenAd setAdStyle(AdStyle style) {
        this.adStyle = style;
        return this;
    }

    // Test method to verify method visibility - renamed for testing
    public AppOpenAd setStyle(AdStyle style) {
        this.adStyle = style;
        return this;
    }

    /**
     * Loads the app-open ad with the specified device ID.
     *
     * @param deviceId The device or user identifier
     */
    public void loadAd(@NonNull String deviceId) {
        if (!AdSdk.isInitialized()) {
            notifyLoadFailure("AdSdk is not initialized");
            return;
        }

        this.deviceId = deviceId;

        // Simulate ad loading (in a real SDK, this would fetch ad content from a server)
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAdDialog();
                isLoaded = true;
                loadTimeMs = System.currentTimeMillis();

                // UUID no longer logged - using meaningful pattern/style info above

                if (loadListener != null) {
                    loadListener.onAdLoaded(adId);
                }
            }
        });
    }

    /**
     * Creates the ad dialog
     */
    private void createAdDialog() {
        adDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        adDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        adDialog.setCancelable(true);

        // Make dialog truly full screen
        if (adDialog.getWindow() != null) {
            adDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#E0F7FA")));
            adDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            // Set flags for full screen immersive mode
            adDialog.getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }

        // Create full-screen content layout
        android.widget.FrameLayout contentLayout = new android.widget.FrameLayout(activity);

        // Load background color from config - either specific pattern or default to pattern 1
        String defaultBgColor = "#E0F7FA"; // Light blue background
        AdConfig adConfig = AdSdk.getInstance().getAdConfig();

        // Determine which pattern to use
        int patternIndex = (specificPatternIndex != null) ? specificPatternIndex : 0; // Default to pattern 0 (Benign)
        AdConfig.AdVariant currentVariant = null;

        if (adConfig != null) {
            currentVariant = adConfig.getAttackPattern(patternIndex);
            if (currentVariant != null) {
                AdSdk.getInstance().log("AD_LOADED: style=APP_OPEN/" + adStyle.name() + ", pattern=" + currentVariant.getName());
                if (currentVariant.getContent() instanceof AdContent.TextAdContent) {
                    defaultBgColor = ((AdContent.TextAdContent) currentVariant.getContent()).getBackgroundColor();
                }
            }
        }
        contentLayout.setBackgroundColor(Color.parseColor(defaultBgColor));

        // Create close button in top-right corner (shared across styles)
        createCloseButton(contentLayout);

        // Always render AdMob style (only supported style)
        AdSdk.getInstance().log("AppOpenAd: Rendering AdMob style");
        renderAdMobStyle(contentLayout, currentVariant);

        // Set content view
        adDialog.setContentView(contentLayout);

        // Handle dialog dismiss
        adDialog.setOnDismissListener(dialog -> {
            isShowing = false;
            AdSdk.getInstance().log("App-open ad dismissed: " + adId);
        });
    }

    /**
     * Creates the close button (shared across styles)
     */
    private void createCloseButton(android.widget.FrameLayout contentLayout) {
        ImageButton closeButton = new ImageButton(activity);

        // Load x-button image from assets
        try {
            InputStream inputStream = activity.getAssets().open("x-button-48.png");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            closeButton.setImageBitmap(bitmap);
            closeButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        } catch (IOException e) {
            AdSdk.getInstance().logError("Failed to load close button image, using fallback", e);
            // Fallback to text if image loading fails
            closeButton.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        }

        // Set transparent background with circular shape
        GradientDrawable closeButtonBg = new GradientDrawable();
        closeButtonBg.setColor(Color.parseColor("#4D000000")); // Dark semi-transparent
        closeButtonBg.setCornerRadius(dpToPx(24));
        closeButton.setBackground(closeButtonBg);
        closeButton.setPadding(0, 0, 0, 0);

        android.widget.FrameLayout.LayoutParams closeParams = new android.widget.FrameLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
        );
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, dpToPx(40), dpToPx(20), 0);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        contentLayout.addView(closeButton, closeParams);
    }


    /**
     * Creates a default ad variant when no content is available
     */
    private AdConfig.AdVariant createDefaultAdVariant() {
        AdContent.TextAdContent defaultContent = new AdContent.TextAdContent(
                "Stay organized with powerful note-taking features!",  // text
                "#FFFFFF",  // backgroundColor
                "#000000",  // textColor
                "Notepad - Notes & To Do List",  // appName
                "notepad.png",  // appIcon
                4.5f,  // rating
                "FREE",  // price
                "INSTALL",  // ctaText
                "https://play.google.com/store/apps/details?id=notes.notepad"  // appUrl
        );

        return new AdConfig.AdVariant("default_admob", "Default AdMob Ad", defaultContent);
    }

    /**
     * Renders AdMob style (official Google AdMob app open ad layout)
     * Layout structure matches the reference image:
     * 1. Top promotional image (app_ad.png)
     * 2. App logo (notepad.png) in center
     * 3. App name below logo
     * 4. Ad context from config
     * 5. Rating and stars
     * 6. Google Play logo and Install button
     */
    private void renderAdMobStyle(android.widget.FrameLayout contentLayout, AdConfig.AdVariant currentVariant) {
        AdSdk.getInstance().log("AppOpenAd: renderAdMobStyle() called");
        if (currentVariant == null || !(currentVariant.getContent() instanceof AdContent.TextAdContent)) {
            AdSdk.getInstance().log("AppOpenAd: No content available - using default AdMob content");
            // Use default content if no content available
            currentVariant = createDefaultAdVariant();
        }

        AdContent.TextAdContent textContent = (AdContent.TextAdContent) currentVariant.getContent();

        // Set full screen background to white
        contentLayout.setBackgroundColor(Color.WHITE);
        if (adDialog.getWindow() != null) {
            adDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }

        // Create main vertical layout (full screen)
        LinearLayout mainLayout = new LinearLayout(activity);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.WHITE);

        android.widget.FrameLayout.LayoutParams mainParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );

        // === TOP PROMOTIONAL IMAGE (app_ad.png) ===
        android.widget.ImageView topAdImage = new android.widget.ImageView(activity);
        topAdImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

        // Load the app advertisement image
        try {
            InputStream inputStream = activity.getAssets().open("app_ad.png");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            topAdImage.setImageBitmap(bitmap);
            AdSdk.getInstance().log("AppOpenAd: Successfully loaded app_ad.png");
        } catch (Exception e) {
            AdSdk.getInstance().logError("Failed to load app_ad.png", e);
            // Fallback to colored background if image fails to load
            topAdImage.setBackgroundColor(Color.parseColor("#FF6B35"));
        }

        // Make top image clickable
        topAdImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
                dismiss();
            }
        });

        LinearLayout.LayoutParams topImageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(240) // Height for the promotional banner
        );
        mainLayout.addView(topAdImage, topImageParams);

        // === MAIN CONTENT AREA ===
        LinearLayout contentArea = new LinearLayout(activity);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setGravity(Gravity.CENTER);
        contentArea.setBackgroundColor(Color.WHITE);
        int contentPadding = dpToPx(32);
        contentArea.setPadding(contentPadding, contentPadding, contentPadding, contentPadding);

        // App logo (notepad.png) - larger, centered
        android.widget.ImageView appLogo = new android.widget.ImageView(activity);
        appLogo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        // Load app icon from assets
        try {
            if (textContent.getAppIcon() != null && textContent.getAppIcon().endsWith(".png")) {
                InputStream inputStream = activity.getAssets().open(textContent.getAppIcon());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                appLogo.setImageBitmap(bitmap);
                AdSdk.getInstance().log("AppOpenAd: Successfully loaded " + textContent.getAppIcon());
            } else {
                // Fallback to default icon
                appLogo.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        } catch (Exception e) {
            AdSdk.getInstance().logError("Failed to load app icon: " + textContent.getAppIcon(), e);
            appLogo.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // Add rounded corners to app logo
        GradientDrawable logoBackground = new GradientDrawable();
        logoBackground.setCornerRadius(dpToPx(16));
        logoBackground.setColor(Color.TRANSPARENT);
        appLogo.setBackground(logoBackground);
        appLogo.setClipToOutline(true);

        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                dpToPx(80), dpToPx(80)
        );
        logoParams.setMargins(0, 0, 0, dpToPx(16));
        contentArea.addView(appLogo, logoParams);

        // App name (large, bold)
        TextView appName = new TextView(activity);
        appName.setText(textContent.getAppName() != null ? textContent.getAppName() : "App Name");
        appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        appName.setTextColor(Color.BLACK);
        appName.setTypeface(null, android.graphics.Typeface.BOLD);
        appName.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, 0, 0, dpToPx(12));
        contentArea.addView(appName, nameParams);

        // Ad context text (LLM prompt injection content)
        TextView adContextText = new TextView(activity);
        String attackText = textContent.getText();
        AdSdk.getInstance().log("AppOpenAd: Attack pattern text: " + attackText);
        adContextText.setText(attackText);
        adContextText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        adContextText.setTextColor(Color.parseColor("#333333"));
        adContextText.setGravity(Gravity.CENTER);
        adContextText.setLineSpacing(dpToPx(2), 1.2f);

        LinearLayout.LayoutParams contextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contextParams.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        contentArea.addView(adContextText, contextParams);

        // Rating and stars
        TextView ratingText = new TextView(activity);
        if (textContent.getRating() != null) {
            ratingText.setText(String.format("%.1f ★★★★★", textContent.getRating()));
        } else {
            ratingText.setText("4.2 ★★★★★");
        }
        ratingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        ratingText.setTextColor(Color.parseColor("#FF9500")); // Orange color for stars
        ratingText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ratingParams.setMargins(0, 0, 0, dpToPx(8));
        contentArea.addView(ratingText, ratingParams);

        // Price (FREE)
        TextView priceText = new TextView(activity);
        priceText.setText(textContent.getPrice() != null ? textContent.getPrice() : "FREE");
        priceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        priceText.setTextColor(Color.parseColor("#666666"));
        priceText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        priceParams.setMargins(0, 0, 0, dpToPx(24));
        contentArea.addView(priceText, priceParams);

        LinearLayout.LayoutParams contentAreaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1.0f // Take remaining space
        );
        mainLayout.addView(contentArea, contentAreaParams);

        // === BOTTOM SECTION ===
        LinearLayout bottomSection = new LinearLayout(activity);
        bottomSection.setOrientation(LinearLayout.VERTICAL);
        bottomSection.setGravity(Gravity.CENTER);
        bottomSection.setBackgroundColor(Color.WHITE);
        int bottomPadding = dpToPx(20);
        bottomSection.setPadding(bottomPadding, bottomPadding, bottomPadding, bottomPadding);

        // Google Play logo
        android.widget.ImageView googlePlayLogo = new android.widget.ImageView(activity);
        googlePlayLogo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        // Load Google Play logo from assets
        try {
            InputStream inputStream = activity.getAssets().open("Google_Play_logo.png");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            googlePlayLogo.setImageBitmap(bitmap);
            AdSdk.getInstance().log("AppOpenAd: Successfully loaded Google_Play_logo.png");
        } catch (Exception e) {
            AdSdk.getInstance().logError("Failed to load Google_Play_logo.png", e);
            // Fallback to text if image fails to load
            googlePlayLogo.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        LinearLayout.LayoutParams googlePlayLogoParams = new LinearLayout.LayoutParams(
                dpToPx(100), dpToPx(30) // Appropriate size for Google Play logo
        );
        googlePlayLogoParams.setMargins(0, 0, 0, dpToPx(16));
        bottomSection.addView(googlePlayLogo, googlePlayLogoParams);

        // Install button
        Button installButton = new Button(activity);
        installButton.setText(textContent.getCtaText() != null ? textContent.getCtaText() : "Install");
        installButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        installButton.setTextColor(Color.WHITE);
        installButton.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable installBg = new GradientDrawable();
        installBg.setColor(Color.parseColor("#1A73E8")); // Google blue
        installBg.setCornerRadius(dpToPx(24));
        installButton.setBackground(installBg);

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
                dismiss();
            }
        });

        LinearLayout.LayoutParams installParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
        );
        installParams.setMargins(dpToPx(40), 0, dpToPx(40), 0);
        bottomSection.addView(installButton, installParams);

        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        mainLayout.addView(bottomSection, bottomParams);

        // Add main layout to content
        contentLayout.addView(mainLayout, mainParams);

        // Add close button overlay (top right)
        createCloseButton(contentLayout);
    }

    /**
     * Shows the app-open ad.
     * The ad must be loaded first and not expired.
     *
     * @return true if the ad was shown, false if not loaded, expired, or already showing
     */
    public boolean show() {
        if (!isLoaded) {
            AdSdk.getInstance().logError("Cannot show app-open ad: not loaded", null);
            return false;
        }

        if (isExpired()) {
            AdSdk.getInstance().logError("Cannot show app-open ad: expired", null);
            return false;
        }

        if (isShowing) {
            AdSdk.getInstance().logError("Cannot show app-open ad: already showing", null);
            return false;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adDialog != null && !activity.isFinishing()) {
                    adDialog.show();
                    isShowing = true;
                    AdSdk.getInstance().log("App-open ad shown: " + adId);
                }
            }
        });

        return true;
    }

    /**
     * Dismisses the app-open ad if it's currently showing
     */
    public void dismiss() {
        if (isShowing && adDialog != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adDialog.dismiss();
                }
            });
        }
    }

    /**
     * Handles ad click events
     */
    private void handleAdClick() {
        if (!isLoaded) {
            AdSdk.getInstance().logError("Ad clicked before loaded", null);
            return;
        }

        // Log meaningful click information for research
        int patternIndex = (specificPatternIndex != null) ? specificPatternIndex : 0;
        AdConfig adConfig = AdSdk.getInstance().getAdConfig();
        String patternName = "Unknown Pattern";

        if (adConfig != null) {
            AdConfig.AdVariant variant = adConfig.getAttackPattern(patternIndex);
            if (variant != null) {
                patternName = variant.getName();
            }
        }

        String styleInfo = "APP_OPEN/" + adStyle.name();
        AdSdk.getInstance().log("AD_CLICKED: style=" + styleInfo + ", pattern=" + patternName);

        // Track the click event
        if (deviceId != null && !deviceId.isEmpty()) {
            ClickEvent clickEvent = new ClickEvent.Builder()
                    .setAdId(adId)
                    .setAdType(AD_TYPE)
                    .setDeviceId(deviceId)
                    .addAdditionalData("pattern", patternName)
                    .addAdditionalData("style", styleInfo)
                    .build();

            AdSdk.getInstance().getClickTracker().trackClick(clickEvent);
        } else {
            AdSdk.getInstance().logError("Cannot track click: deviceId is null or empty", null);
        }

        // Notify listener
        if (clickListener != null) {
            clickListener.onAdClicked(adId, AD_TYPE);
        }

        // Redirect to app URL if available
        redirectToAppUrl();
    }

    /**
     * Redirects user to the app's Play Store URL
     */
    private void redirectToAppUrl() {
        AdSdk.getInstance().log("AppOpenAd: redirectToAppUrl() called");

        int patternIndex = (specificPatternIndex != null) ? specificPatternIndex : 0;
        AdSdk.getInstance().log("AppOpenAd: patternIndex = " + patternIndex);

        AdConfig adConfig = AdSdk.getInstance().getAdConfig();

        if (adConfig == null) {
            AdSdk.getInstance().logError("AppOpenAd: adConfig is null", null);
            return;
        }

        AdConfig.AdVariant variant = adConfig.getAttackPattern(patternIndex);
        if (variant == null) {
            AdSdk.getInstance().logError("AppOpenAd: variant is null for pattern index: " + patternIndex, null);
            return;
        }

        if (!(variant.getContent() instanceof AdContent.TextAdContent)) {
            AdSdk.getInstance().logError("AppOpenAd: variant content is not TextAdContent", null);
            return;
        }

        AdContent.TextAdContent textContent = (AdContent.TextAdContent) variant.getContent();
        String appUrl = textContent.getAppUrl();

        AdSdk.getInstance().log("AppOpenAd: appUrl = " + appUrl);

        if (appUrl == null || appUrl.isEmpty()) {
            AdSdk.getInstance().logError("AppOpenAd: appUrl is null or empty", null);
            return;
        }

        try {
            AdSdk.getInstance().log("AppOpenAd: Creating intent for URL: " + appUrl);

            // Create standard ACTION_VIEW intent
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(appUrl));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            // Let Android handle the URL naturally - try direct intent first
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                AdSdk.getInstance().log("AppOpenAd: Opening URL with system default: " + appUrl);
                activity.startActivity(intent);
                AdSdk.getInstance().log("AppOpenAd: Successfully opened URL: " + appUrl);
            } else {
                // Fallback to chooser if no default handler
                AdSdk.getInstance().log("AppOpenAd: No default handler, using chooser for URL: " + appUrl);
                android.content.Intent chooserIntent = android.content.Intent.createChooser(intent, "Open link with");
                chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    activity.startActivity(chooserIntent);
                    AdSdk.getInstance().log("AppOpenAd: Successfully opened URL with chooser: " + appUrl);
                } catch (Exception chooserException) {
                    AdSdk.getInstance().logError("AppOpenAd: Chooser failed for URL: " + appUrl, chooserException);
                    android.widget.Toast.makeText(activity, "Unable to open link", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            AdSdk.getInstance().logError("AppOpenAd: Failed to redirect to app URL: " + appUrl, e);
        }
    }

    /**
     * Checks if the ad has expired.
     * App-open ads expire after 4 hours.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        if (!isLoaded) {
            return false;
        }
        long currentTimeMs = System.currentTimeMillis();
        return (currentTimeMs - loadTimeMs) >= AD_EXPIRY_MS;
    }

    /**
     * Sets the click listener for this app-open ad
     *
     * @param listener The click listener
     */
    public void setAdClickListener(@Nullable AdClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Sets the load listener for this app-open ad
     *
     * @param listener The load listener
     */
    public void setAdLoadListener(@Nullable AdLoadListener listener) {
        this.loadListener = listener;
    }

    /**
     * Gets the unique ad ID
     *
     * @return The ad ID
     */
    @NonNull
    public String getAdId() {
        return adId;
    }

    /**
     * Checks if the ad is currently loaded
     *
     * @return true if loaded, false otherwise
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Checks if the ad is currently showing
     *
     * @return true if showing, false otherwise
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Destroys the app-open ad and cleans up resources
     */
    public void destroy() {
        dismiss();
        isLoaded = false;
        clickListener = null;
        loadListener = null;
        if (adDialog != null) {
            adDialog = null;
        }
        AdSdk.getInstance().log("App-open ad destroyed: " + adId);
    }

    /**
     * Notifies load failure
     */
    private void notifyLoadFailure(String errorMessage) {
        AdSdk.getInstance().logError("App-open ad failed to load: " + errorMessage, null);
        if (loadListener != null) {
            loadListener.onAdFailedToLoad(adId, errorMessage);
        }
    }

    /**
     * Converts dp to pixels
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                activity.getResources().getDisplayMetrics()
        );
    }
}
