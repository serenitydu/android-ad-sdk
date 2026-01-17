package com.example.adsdk.ads.banner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.adsdk.AdSdk;
import com.example.adsdk.config.AdConfig;
import com.example.adsdk.config.AdConfigLoader;
import com.example.adsdk.listeners.AdClickListener;
import com.example.adsdk.listeners.AdLoadListener;
import com.example.adsdk.tracking.ClickEvent;
import com.example.adsdk.models.AdContent;
import com.example.adsdk.models.AdStyle;

import java.util.UUID;

/**
 * A simple banner ad view that displays a rectangular ad at the bottom or top of the screen.
 * Automatically tracks clicks when the user taps the banner.
 *
 * Usage in XML:
 * <pre>
 * &lt;com.example.adsdk.ads.banner.BannerAdView
 *     android:id="@+id/banner_ad"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" /&gt;
 * </pre>
 *
 * Usage in code:
 * <pre>
 * BannerAdView bannerAd = findViewById(R.id.banner_ad);
 * bannerAd.setAdClickListener(new AdClickListener() {
 *     @Override
 *     public void onAdClicked(@NonNull String adId, @NonNull String adType) {
 *         // Handle click
 *     }
 * });
 * bannerAd.loadAd("your-device-id");
 * </pre>
 */
public class BannerAdView extends FrameLayout {

    private static final String AD_TYPE = "banner";
    private static final int DEFAULT_HEIGHT_DP = 50;
    private static final int DEFAULT_PADDING_DP = 8;

    private String adId;
    private String deviceId;
    private TextView adContentView;
    private AdClickListener clickListener;
    private AdLoadListener loadListener;
    private boolean isLoaded = false;
    private AdConfig.AdVariant currentVariant;
    private Integer specificPatternIndex = null; // Specific pattern to use, or null for default
    private AdStyle adStyle = AdStyle.ADMOB; // Default to ADMOB style

    public BannerAdView(@NonNull Context context) {
        super(context);
        init();
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Initializes the banner ad view
     */
    private void init() {
        // Generate unique ad ID
        adId = "banner_" + UUID.randomUUID().toString();

        // Set default height
        int heightPx = dpToPx(DEFAULT_HEIGHT_DP);
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
        ));

        // Create ad content view
        adContentView = new TextView(getContext());
        adContentView.setGravity(Gravity.CENTER);
        adContentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        adContentView.setTextColor(Color.WHITE);

        int paddingPx = dpToPx(DEFAULT_PADDING_DP);
        adContentView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Create background
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#4CAF50"));
        background.setCornerRadius(dpToPx(4));
        adContentView.setBackground(background);

        // Add to layout
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(paddingPx, paddingPx, paddingPx, paddingPx);
        addView(adContentView, params);

        // Initially invisible until loaded
        setVisibility(View.GONE);

        // Set click listener
        adContentView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
            }
        });
    }

    /**
     * Sets a specific attack pattern to use instead of the default.
     * Patterns are 0-indexed (0 = benign, 1-10 = attack patterns).
     *
     * @param patternIndex The index of the attack pattern to use (0-10)
     * @return This BannerAdView instance for chaining
     */
    public BannerAdView setAttackPattern(int patternIndex) {
        this.specificPatternIndex = patternIndex;
        return this;
    }

    /**
     * Sets the ad style (ADMOB only supported).
     * Must be called before loadAd().
     *
     * @param style The ad style to use (only AdStyle.ADMOB is supported)
     * @return This BannerAdView instance for chaining
     */
    public BannerAdView setAdStyle(AdStyle style) {
        this.adStyle = style;
        return this;
    }

    /**
     * Loads the banner ad with the specified device ID.
     *
     * @param deviceId The device or user identifier
     */
    public void loadAd(@NonNull String deviceId) {
        if (!AdSdk.isInitialized()) {
            notifyLoadFailure("AdSdk is not initialized");
            return;
        }

        this.deviceId = deviceId;

        // Load ad content from configuration
        post(new Runnable() {
            @Override
            public void run() {
                // If a specific pattern was set, use pattern-based content first
                if (specificPatternIndex != null) {
                    AdConfig adConfig = AdSdk.getInstance().getAdConfig();
                    if (adConfig != null) {
                        currentVariant = adConfig.getAttackPattern(specificPatternIndex);
                        if (currentVariant != null) {
                            AdSdk.getInstance().log("Banner ad using specified pattern " + specificPatternIndex + ": " + currentVariant.getName());
                        }
                    }
                } else {
                    // Try to load app-specific content for apps that don't specify a pattern
                    String packageName = getContext().getPackageName();
                    AdContent appSpecificContent = AdConfigLoader.loadAppSpecificContent(getContext(), packageName);

                    if (appSpecificContent != null) {
                        AdSdk.getInstance().log("Banner ad using app-specific content for package: " + packageName);
                        // Create a variant for the app-specific content
                        currentVariant = new AdConfig.AdVariant("app_specific_" + packageName,
                                                               "App Specific Attack",
                                                               appSpecificContent);
                    } else {
                        // Fallback to pattern 0 if no app-specific content found
                        AdConfig adConfig = AdSdk.getInstance().getAdConfig();
                        if (adConfig != null) {
                            currentVariant = adConfig.getAttackPattern(0);
                            if (currentVariant != null) {
                                AdSdk.getInstance().log("Banner ad using default pattern 0: " + currentVariant.getName());
                            }
                        }
                    }
                }

                if (currentVariant != null && currentVariant.getContent() instanceof AdContent.TextAdContent) {
                    AdContent.TextAdContent textContent = (AdContent.TextAdContent) currentVariant.getContent();

                    // Always render AdMob style (only supported style)
                    renderAdMobStyle(textContent);

                    AdSdk.getInstance().log("AD_LOADED: style=BANNER/" + adStyle.name() + ", pattern=" + currentVariant.getName());
                } else {
                    // Fallback to default content if config not available
                    adContentView.setText("📱 Sample Banner Ad - Tap Me!");
                    AdSdk.getInstance().log("Banner ad loaded with default content");
                }

                setVisibility(View.VISIBLE);
                isLoaded = true;

                // UUID no longer logged - using meaningful pattern/style info above

                if (loadListener != null) {
                    loadListener.onAdLoaded(adId);
                }
            }
        });
    }

    /**
     * Renders basic style (simple text banner)
     */

    /**
     * Renders AdMob style (professional layout with app icon, name, rating, CTA)
     */
    private void renderAdMobStyle(@NonNull AdContent.TextAdContent textContent) {
        // Clear existing views
        removeAllViews();

        // Create main container
        android.widget.LinearLayout container = new android.widget.LinearLayout(getContext());
        container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setBackgroundColor(Color.WHITE);
        int padding = dpToPx(8);
        container.setPadding(padding, padding, padding, padding);

        // Left section: App icon (load PNG from assets)
        android.widget.ImageView iconView = new android.widget.ImageView(getContext());
        iconView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        // Load PNG image from assets
        try {
            if (textContent.getAppIcon() != null && textContent.getAppIcon().endsWith(".png")) {
                InputStream inputStream = getContext().getAssets().open(textContent.getAppIcon());
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
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

        android.widget.LinearLayout.LayoutParams iconParams = new android.widget.LinearLayout.LayoutParams(
                dpToPx(48), dpToPx(48)
        );
        iconParams.setMargins(0, 0, dpToPx(12), 0);

        // Make icon clickable
        iconView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
            }
        });

        container.addView(iconView, iconParams);

        // Middle section: App info
        android.widget.LinearLayout infoContainer = new android.widget.LinearLayout(getContext());
        infoContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams infoParams = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );

        // App name + "Ad" badge
        android.widget.LinearLayout nameRow = new android.widget.LinearLayout(getContext());
        nameRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView appNameView = new TextView(getContext());
        appNameView.setText(textContent.getAppName() != null ? textContent.getAppName() : "App");
        appNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        appNameView.setTextColor(Color.BLACK);
        appNameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameRow.addView(appNameView);

        TextView adBadge = new TextView(getContext());
        adBadge.setText(" Ad");
        adBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        adBadge.setTextColor(Color.parseColor("#666666"));
        android.widget.LinearLayout.LayoutParams badgeParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.setMargins(dpToPx(6), 0, 0, 0);
        nameRow.addView(adBadge, badgeParams);

        infoContainer.addView(nameRow);

        // Rating + Price
        // android.widget.LinearLayout ratingRow = new android.widget.LinearLayout(getContext());
        // ratingRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        // ratingRow.setGravity(Gravity.CENTER_VERTICAL);

        // if (textContent.getRating() != null) {
        //     TextView ratingView = new TextView(getContext());
        //     ratingView.setText(String.format("%.1f ⭐", textContent.getRating()));
        //     ratingView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        //     ratingView.setTextColor(Color.parseColor("#666666"));
        //     ratingRow.addView(ratingView);
        // }

        // if (textContent.getPrice() != null) {
        //     TextView priceView = new TextView(getContext());
        //     priceView.setText(" • " + textContent.getPrice());
        //     priceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        //     priceView.setTextColor(Color.parseColor("#666666"));
        //     ratingRow.addView(priceView);
        // }

        // android.widget.LinearLayout.LayoutParams ratingParams = new android.widget.LinearLayout.LayoutParams(
        //         android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        //         android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        // );
        // ratingParams.setMargins(0, dpToPx(2), 0, 0);
        // infoContainer.addView(ratingRow, ratingParams);

        // Ad text (attack pattern) - show full text without truncation
        TextView adTextView = new TextView(getContext());
        adTextView.setText(textContent.getText());
        adTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        adTextView.setTextColor(Color.parseColor("#333333"));
        // Remove maxLines and ellipsize to show full attack pattern text
        android.widget.LinearLayout.LayoutParams textParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, dpToPx(4), 0, 0);
        infoContainer.addView(adTextView, textParams);

        // Make text content area clickable
        infoContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
            }
        });

        container.addView(infoContainer, infoParams);

        // Right section: CTA button
        android.widget.Button ctaButton = new android.widget.Button(getContext());
        ctaButton.setText(textContent.getCtaText() != null ? textContent.getCtaText() : "INSTALL");
        ctaButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        ctaButton.setTextColor(Color.WHITE);
        ctaButton.setAllCaps(true);
        ctaButton.setTypeface(null, android.graphics.Typeface.BOLD);

        GradientDrawable ctaBackground = new GradientDrawable();
        ctaBackground.setColor(Color.parseColor("#1A73E8")); // Google blue
        ctaBackground.setCornerRadius(dpToPx(4));
        ctaButton.setBackground(ctaBackground);

        int ctaPadding = dpToPx(12);
        ctaButton.setPadding(ctaPadding, dpToPx(6), ctaPadding, dpToPx(6));

        android.widget.LinearLayout.LayoutParams ctaParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ctaParams.setMargins(dpToPx(8), 0, 0, 0);

        ctaButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
            }
        });

        container.addView(ctaButton, ctaParams);

        // Make entire container clickable as fallback
        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAdClick();
            }
        });

        // Add container to banner
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        addView(container, containerParams);

        // Set overall background
        setBackgroundColor(Color.WHITE);
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
        String styleInfo = "BANNER/" + adStyle.name();

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
        AdSdk.getInstance().log("BannerAd: redirectToAppUrl() called");

        if (currentVariant == null) {
            AdSdk.getInstance().logError("BannerAd: currentVariant is null", null);
            return;
        }

        if (!(currentVariant.getContent() instanceof AdContent.TextAdContent)) {
            AdSdk.getInstance().logError("BannerAd: currentVariant content is not TextAdContent", null);
            return;
        }

        AdContent.TextAdContent textContent = (AdContent.TextAdContent) currentVariant.getContent();
        String appUrl = textContent.getAppUrl();

        AdSdk.getInstance().log("BannerAd: appUrl = " + appUrl);

        if (appUrl == null || appUrl.isEmpty()) {
            AdSdk.getInstance().logError("BannerAd: appUrl is null or empty", null);
            return;
        }

        try {
            AdSdk.getInstance().log("BannerAd: Creating intent for URL: " + appUrl);

            // Check if it's a local asset file
            if (appUrl.startsWith("file:///android_asset/")) {
                AdSdk.getInstance().log("BannerAd: Opening local asset in WebView: " + appUrl);

                // Use WebViewActivity for local assets to avoid FileUriExposedException
                android.content.Intent webViewIntent = new android.content.Intent(getContext(), com.example.adsdk.WebViewActivity.class);
                webViewIntent.putExtra("url", appUrl);
                webViewIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

                getContext().startActivity(webViewIntent);
                AdSdk.getInstance().log("BannerAd: Successfully opened local asset: " + appUrl);
                return;
            }

            // Handle external URLs with the original logic
            AdSdk.getInstance().log("BannerAd: Opening external URL: " + appUrl);

            // Create standard ACTION_VIEW intent for external URLs
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(appUrl));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            // Let Android handle the URL naturally - try direct intent first
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                AdSdk.getInstance().log("BannerAd: Opening URL with system default: " + appUrl);
                getContext().startActivity(intent);
                AdSdk.getInstance().log("BannerAd: Successfully opened URL: " + appUrl);
            } else {
                // Fallback to chooser if no default handler
                AdSdk.getInstance().log("BannerAd: No default handler, using chooser for URL: " + appUrl);
                android.content.Intent chooserIntent = android.content.Intent.createChooser(intent, "Open link with");
                chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    getContext().startActivity(chooserIntent);
                    AdSdk.getInstance().log("BannerAd: Successfully opened URL with chooser: " + appUrl);
                } catch (Exception chooserException) {
                    AdSdk.getInstance().logError("BannerAd: Chooser failed for URL: " + appUrl, chooserException);
                    android.widget.Toast.makeText(getContext(), "Unable to open link", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            AdSdk.getInstance().logError("BannerAd: Failed to redirect to app URL: " + appUrl, e);
        }
    }

    /**
     * Sets the click listener for this banner ad
     *
     * @param listener The click listener
     */
    public void setAdClickListener(@Nullable AdClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Sets the load listener for this banner ad
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
     * Destroys the banner ad and cleans up resources
     */
    public void destroy() {
        isLoaded = false;
        setVisibility(View.GONE);
        clickListener = null;
        loadListener = null;
        AdSdk.getInstance().log("Banner ad destroyed: " + adId);
    }

    /**
     * Notifies load failure
     */
    private void notifyLoadFailure(String errorMessage) {
        AdSdk.getInstance().logError("Banner ad failed to load: " + errorMessage, null);
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
                getContext().getResources().getDisplayMetrics()
        );
    }
}
