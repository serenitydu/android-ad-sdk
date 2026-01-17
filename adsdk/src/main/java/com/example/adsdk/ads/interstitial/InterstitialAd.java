package com.example.adsdk.ads.interstitial;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.adsdk.AdSdk;
import com.example.adsdk.config.AdConfig;
import com.example.adsdk.listeners.AdClickListener;
import com.example.adsdk.listeners.AdLoadListener;
import com.example.adsdk.models.AdContent;
import com.example.adsdk.models.AdFormat;
import com.example.adsdk.models.AdStyle;
import com.example.adsdk.tracking.ClickEvent;

import java.util.UUID;

/**
 * A full-screen interstitial ad that appears as an overlay.
 * Automatically tracks clicks when the user taps the ad content.
 *
 * Usage:
 * <pre>
 * InterstitialAd interstitialAd = new InterstitialAd(activity);
 * interstitialAd.setAdClickListener(new AdClickListener() {
 *     @Override
 *     public void onAdClicked(@NonNull String adId, @NonNull String adType) {
 *         // Handle click
 *     }
 * });
 * interstitialAd.loadAd("your-device-id");
 *
 * // Later, when ready to show
 * if (interstitialAd.isLoaded()) {
 *     interstitialAd.show();
 * }
 * </pre>
 */
public class InterstitialAd {

    private static final String AD_TYPE = "interstitial";

    private final Activity activity;
    private String adId;
    private String deviceId;
    private Dialog adDialog;
    private AdClickListener clickListener;
    private AdLoadListener loadListener;
    private boolean isLoaded = false;
    private boolean isShowing = false;
    private AdConfig.AdVariant currentVariant;
    private AdStyle adStyle = AdStyle.ADMOB; // Default to ADMOB style
    private Integer specificPatternIndex = null; // Specific pattern to use, or null for default

    /**
     * Creates a new interstitial ad
     *
     * @param activity The activity context
     */
    public InterstitialAd(@NonNull Activity activity) {
        this.activity = activity;
        this.adId = "interstitial_" + UUID.randomUUID().toString();
    }

    /**
     * Sets the ad style (ADMOB only supported).
     * Must be called before loadAd().
     *
     * @param style The ad style to use (only AdStyle.ADMOB is supported)
     * @return This InterstitialAd instance for chaining
     */
    public InterstitialAd setAdStyle(AdStyle style) {
        this.adStyle = style;
        return this;
    }

    /**
     * Sets the attack pattern index for this interstitial ad.
     * Must be called before loadAd().
     *
     * @param attackIndex The attack pattern index (0 = benign, 1-10 = attack patterns)
     * @return This InterstitialAd instance for chaining
     */
    public InterstitialAd setAttackPattern(int patternIndex) {
        this.specificPatternIndex = patternIndex;
        return this;
    }

    /**
     * Loads the interstitial ad with the specified device ID.
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
        adDialog = new Dialog(activity);
        adDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        adDialog.setCancelable(true);

        // Make dialog full screen
        if (adDialog.getWindow() != null) {
            adDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            adDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        // Determine which pattern to use
        int patternIndex = (specificPatternIndex != null) ? specificPatternIndex : 0; // Default to pattern 0 (Benign)

        // Load attack pattern
        AdConfig adConfig = AdSdk.getInstance().getAdConfig();
        if (adConfig != null) {
            currentVariant = adConfig.getAttackPattern(patternIndex);
            if (currentVariant != null) {
                AdSdk.getInstance().log("AD_LOADED: style=INTERSTITIAL/" + adStyle.name() + ", pattern=" + currentVariant.getName());
            }
        }

        // Create content layout
        LinearLayout contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);
        contentLayout.setBackgroundColor(Color.parseColor("#80000000")); // Semi-transparent black

        // Always render AdMob style (only supported style)
        renderAdMobStyle(contentLayout);

        // Set content view
        adDialog.setContentView(contentLayout);

        // Handle dialog dismiss
        adDialog.setOnDismissListener(dialog -> {
            isShowing = false;
            AdSdk.getInstance().log("Interstitial ad dismissed: " + adId);
        });
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
     * Renders AdMob style (professional full-screen layout with app icon, name, rating, CTA)
     */
    private void renderAdMobStyle(LinearLayout contentLayout) {
        if (currentVariant == null || !(currentVariant.getContent() instanceof AdContent.TextAdContent)) {
            // Use default content if no content available
            currentVariant = createDefaultAdVariant();
        }

        AdContent.TextAdContent textContent = (AdContent.TextAdContent) currentVariant.getContent();

        // Create FrameLayout to support overlay close button
        android.widget.FrameLayout frameContainer = new android.widget.FrameLayout(activity);
        frameContainer.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                dpToPx(350),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        frameContainer.setLayoutParams(frameParams);

        // Create main container
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setBackgroundColor(Color.WHITE); // Standard AdMob white background
        int padding = dpToPx(32);
        container.setPadding(padding, padding, padding, padding);

        android.widget.FrameLayout.LayoutParams containerParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        frameContainer.addView(container, containerParams);

        // Top section: App icon
        ImageView iconView = new ImageView(activity);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Load PNG image from assets
        try {
            if (textContent.getAppIcon() != null && textContent.getAppIcon().endsWith(".png")) {
                InputStream inputStream = activity.getAssets().open(textContent.getAppIcon());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                iconView.setImageBitmap(bitmap);
            } else {
                // Fallback to default icon if no PNG specified
                iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        } catch (Exception e) {
            AdSdk.getInstance().logError("Failed to load app icon: " + textContent.getAppIcon(), e);
            // Fallback to default system icon
            iconView.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(80), dpToPx(80)
        );
        iconParams.setMargins(0, 0, 0, dpToPx(16));
        iconParams.gravity = Gravity.CENTER;

        // Make icon clickable
        iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
                dismiss();
            }
        });

        container.addView(iconView, iconParams);

        // App name + "Ad" badge
        LinearLayout nameRow = new LinearLayout(activity);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER);

        TextView appNameView = new TextView(activity);
        appNameView.setText(textContent.getAppName() != null ? textContent.getAppName() : "App");
        appNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        appNameView.setTextColor(Color.BLACK);
        appNameView.setTypeface(null, android.graphics.Typeface.BOLD);
        appNameView.setGravity(Gravity.CENTER);
        nameRow.addView(appNameView);

        TextView adBadge = new TextView(activity);
        adBadge.setText(" Ad");
        adBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        adBadge.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.setMargins(dpToPx(8), 0, 0, 0);
        nameRow.addView(adBadge, badgeParams);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, 0, 0, dpToPx(8));
        nameParams.gravity = Gravity.CENTER;
        container.addView(nameRow, nameParams);

        // Rating + Price
        LinearLayout ratingRow = new LinearLayout(activity);
        ratingRow.setOrientation(LinearLayout.HORIZONTAL);
        ratingRow.setGravity(Gravity.CENTER);

        if (textContent.getRating() != null) {
            TextView ratingView = new TextView(activity);
            ratingView.setText(String.format("%.1f ⭐", textContent.getRating()));
            ratingView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            ratingView.setTextColor(Color.parseColor("#666666"));
            ratingView.setGravity(Gravity.CENTER);
            ratingRow.addView(ratingView);
        }

        if (textContent.getPrice() != null) {
            TextView priceView = new TextView(activity);
            priceView.setText(" • " + textContent.getPrice());
            priceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            priceView.setTextColor(Color.parseColor("#666666"));
            ratingRow.addView(priceView);
        }

        LinearLayout.LayoutParams ratingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ratingParams.setMargins(0, 0, 0, dpToPx(16));
        ratingParams.gravity = Gravity.CENTER;
        container.addView(ratingRow, ratingParams);

        // Ad text (attack pattern) - show full text
        TextView adTextView = new TextView(activity);
        adTextView.setText(textContent.getText());
        adTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        adTextView.setTextColor(Color.parseColor("#333333")); // Standard dark gray text
        adTextView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, 0, 0, dpToPx(24));
        container.addView(adTextView, textParams);

        // Make text content area clickable
        adTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
                dismiss();
            }
        });

        // CTA button
        Button ctaButton = new Button(activity);
        ctaButton.setText(textContent.getCtaText() != null ? textContent.getCtaText() : "INSTALL");
        ctaButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        ctaButton.setTextColor(Color.WHITE);
        ctaButton.setAllCaps(true);
        ctaButton.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable ctaBackground = new GradientDrawable();
        ctaBackground.setColor(Color.parseColor("#1A73E8")); // Google blue
        ctaBackground.setCornerRadius(dpToPx(6));
        ctaButton.setBackground(ctaBackground);

        LinearLayout.LayoutParams ctaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
        );
        ctaParams.setMargins(0, 0, 0, dpToPx(12));

        ctaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
                dismiss();
            }
        });

        container.addView(ctaButton, ctaParams);

        // Make entire container clickable as fallback
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
                dismiss();
            }
        });

        // Add x-button close button in upper-right corner
        createCloseButton(frameContainer);

        // Add frame container to content layout
        contentLayout.addView(frameContainer);
    }

    /**
     * Creates the close button with x-button image in upper-right corner
     */
    private void createCloseButton(android.widget.FrameLayout frameContainer) {
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
        closeButtonBg.setCornerRadius(dpToPx(16)); // Smaller radius for smaller button
        closeButton.setBackground(closeButtonBg);
        closeButton.setPadding(0, 0, 0, 0);

        android.widget.FrameLayout.LayoutParams closeParams = new android.widget.FrameLayout.LayoutParams(
                dpToPx(32), // Smaller button: 32dp instead of 48dp
                dpToPx(32)
        );
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, dpToPx(16), dpToPx(16), 0); // Smaller margins too

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        frameContainer.addView(closeButton, closeParams);
    }

    /**
     * Shows the interstitial ad.
     * The ad must be loaded first.
     *
     * @return true if the ad was shown, false if not loaded or already showing
     */
    public boolean show() {
        if (!isLoaded) {
            AdSdk.getInstance().logError("Cannot show interstitial ad: not loaded", null);
            return false;
        }

        if (isShowing) {
            AdSdk.getInstance().logError("Cannot show interstitial ad: already showing", null);
            return false;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (adDialog != null && !activity.isFinishing()) {
                    adDialog.show();
                    isShowing = true;
                    AdSdk.getInstance().log("Interstitial ad shown: " + adId);
                }
            }
        });

        return true;
    }

    /**
     * Dismisses the interstitial ad if it's currently showing
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
        String patternName = (currentVariant != null) ? currentVariant.getName() : "Unknown Pattern";
        String styleInfo = "INTERSTITIAL/" + adStyle.name();

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
        AdSdk.getInstance().log("InterstitialAd: redirectToAppUrl() called");

        if (currentVariant == null) {
            AdSdk.getInstance().logError("InterstitialAd: currentVariant is null", null);
            return;
        }

        if (!(currentVariant.getContent() instanceof AdContent.TextAdContent)) {
            AdSdk.getInstance().logError("InterstitialAd: currentVariant content is not TextAdContent", null);
            return;
        }

        AdContent.TextAdContent textContent = (AdContent.TextAdContent) currentVariant.getContent();
        String appUrl = textContent.getAppUrl();

        AdSdk.getInstance().log("InterstitialAd: appUrl = " + appUrl);

        if (appUrl == null || appUrl.isEmpty()) {
            AdSdk.getInstance().logError("InterstitialAd: appUrl is null or empty", null);
            return;
        }

        try {
            AdSdk.getInstance().log("InterstitialAd: Creating intent for URL: " + appUrl);

            // Create standard ACTION_VIEW intent
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(appUrl));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            // Let Android handle the URL naturally - try direct intent first
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                AdSdk.getInstance().log("InterstitialAd: Opening URL with system default: " + appUrl);
                activity.startActivity(intent);
                AdSdk.getInstance().log("InterstitialAd: Successfully opened URL: " + appUrl);
            } else {
                // Fallback to chooser if no default handler
                AdSdk.getInstance().log("InterstitialAd: No default handler, using chooser for URL: " + appUrl);
                android.content.Intent chooserIntent = android.content.Intent.createChooser(intent, "Open link with");
                chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    activity.startActivity(chooserIntent);
                    AdSdk.getInstance().log("InterstitialAd: Successfully opened URL with chooser: " + appUrl);
                } catch (Exception chooserException) {
                    AdSdk.getInstance().logError("InterstitialAd: Chooser failed for URL: " + appUrl, chooserException);
                    android.widget.Toast.makeText(activity, "Unable to open link", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            AdSdk.getInstance().logError("InterstitialAd: Failed to redirect to app URL: " + appUrl, e);
        }
    }

    /**
     * Sets the click listener for this interstitial ad
     *
     * @param listener The click listener
     */
    public void setAdClickListener(@Nullable AdClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Sets the load listener for this interstitial ad
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
     * Destroys the interstitial ad and cleans up resources
     */
    public void destroy() {
        dismiss();
        isLoaded = false;
        clickListener = null;
        loadListener = null;
        if (adDialog != null) {
            adDialog = null;
        }
        AdSdk.getInstance().log("Interstitial ad destroyed: " + adId);
    }

    /**
     * Notifies load failure
     */
    private void notifyLoadFailure(String errorMessage) {
        AdSdk.getInstance().logError("Interstitial ad failed to load: " + errorMessage, null);
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
