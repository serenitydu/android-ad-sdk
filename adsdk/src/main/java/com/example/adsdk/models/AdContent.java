package com.example.adsdk.models;

import androidx.annotation.NonNull;

/**
 * Base class for ad content.
 * Contains common properties shared by all ad formats.
 */
public abstract class AdContent {
    private final AdFormat format;

    protected AdContent(@NonNull AdFormat format) {
        this.format = format;
    }

    @NonNull
    public AdFormat getFormat() {
        return format;
    }

    /**
     * Text-based ad content
     */
    public static class TextAdContent extends AdContent {
        private final String text;
        private final String backgroundColor;
        private final String textColor;

        // AdMob-style metadata (optional)
        private final String appName;
        private final String appIcon;
        private final Float rating;
        private final String price;
        private final String ctaText;
        private final String appUrl;

        public TextAdContent(@NonNull String text, @NonNull String backgroundColor, @NonNull String textColor) {
            this(text, backgroundColor, textColor, null, null, null, null, null, null);
        }

        public TextAdContent(@NonNull String text, @NonNull String backgroundColor, @NonNull String textColor,
                           String appName, String appIcon, Float rating, String price, String ctaText, String appUrl) {
            super(AdFormat.TEXT);
            this.text = text;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.appName = appName;
            this.appIcon = appIcon;
            this.rating = rating;
            this.price = price;
            this.ctaText = ctaText;
            this.appUrl = appUrl;
        }

        @NonNull
        public String getText() {
            return text;
        }

        @NonNull
        public String getBackgroundColor() {
            return backgroundColor;
        }

        @NonNull
        public String getTextColor() {
            return textColor;
        }

        public String getAppName() {
            return appName;
        }

        public String getAppIcon() {
            return appIcon;
        }

        public Float getRating() {
            return rating;
        }

        public String getPrice() {
            return price;
        }

        public String getCtaText() {
            return ctaText;
        }

        public String getAppUrl() {
            return appUrl;
        }

        public boolean hasAdMobMetadata() {
            return appName != null || appIcon != null || rating != null || price != null || ctaText != null;
        }
    }

    /**
     * Image-based ad content
     */
    public static class ImageAdContent extends AdContent {
        private final String imageUrl;
        private final String clickUrl;

        public ImageAdContent(@NonNull String imageUrl, @NonNull String clickUrl) {
            super(AdFormat.IMAGE);
            this.imageUrl = imageUrl;
            this.clickUrl = clickUrl;
        }

        @NonNull
        public String getImageUrl() {
            return imageUrl;
        }

        @NonNull
        public String getClickUrl() {
            return clickUrl;
        }
    }

    /**
     * HTML-based ad content
     */
    public static class HtmlAdContent extends AdContent {
        private final String html;

        public HtmlAdContent(@NonNull String html) {
            super(AdFormat.HTML);
            this.html = html;
        }

        @NonNull
        public String getHtml() {
            return html;
        }
    }
}
