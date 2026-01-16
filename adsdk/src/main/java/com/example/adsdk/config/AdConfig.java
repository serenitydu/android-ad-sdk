package com.example.adsdk.config;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.adsdk.models.AdContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class that holds ad content for different ad types.
 * Attack patterns can be used by any ad type (banner, interstitial, app-open).
 */
public class AdConfig {
    private final List<AdVariant> attackPatterns;
    private final AdContent bannerDefaultContent;
    private final AdContent interstitialDefaultContent;
    private final AdContent appOpenDefaultContent;

    private AdConfig(Builder builder) {
        this.attackPatterns = builder.attackPatterns;
        this.bannerDefaultContent = builder.bannerDefaultContent;
        this.interstitialDefaultContent = builder.interstitialDefaultContent;
        this.appOpenDefaultContent = builder.appOpenDefaultContent;
    }

    /**
     * Gets all attack patterns. These patterns can be used by any ad type.
     *
     * @return List of attack patterns
     */
    @NonNull
    public List<AdVariant> getAttackPatterns() {
        return attackPatterns != null ? attackPatterns : new ArrayList<>();
    }

    /**
     * Gets default banner content (fallback if no attack pattern is selected)
     */
    @Nullable
    public AdContent getBannerDefaultContent() {
        return bannerDefaultContent;
    }

    /**
     * Gets default interstitial content (fallback if no attack pattern is selected)
     */
    @Nullable
    public AdContent getInterstitialDefaultContent() {
        return interstitialDefaultContent;
    }

    /**
     * Gets default app-open content (fallback if no attack pattern is selected)
     */
    @Nullable
    public AdContent getAppOpenDefaultContent() {
        return appOpenDefaultContent;
    }

    /**
     * Get a specific attack pattern by index.
     * Attack patterns can be used by any ad type (banner, interstitial, app-open).
     *
     * @param index The index of the attack pattern (0-based)
     * @return AdVariant at the specified index, or null if out of bounds
     */
    @Nullable
    public AdVariant getAttackPattern(int index) {
        if (attackPatterns != null && index >= 0 && index < attackPatterns.size()) {
            return attackPatterns.get(index);
        }
        return null;
    }

    /**
     * Represents a single ad variant with an ID, name, and content
     */
    public static class AdVariant {
        private final String id;
        private final String name;
        private final AdContent content;

        public AdVariant(@NonNull String id, @NonNull String name, @NonNull AdContent content) {
            this.id = id;
            this.name = name;
            this.content = content;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @NonNull
        public AdContent getContent() {
            return content;
        }
    }

    public static class Builder {
        private List<AdVariant> attackPatterns;
        private AdContent bannerDefaultContent;
        private AdContent interstitialDefaultContent;
        private AdContent appOpenDefaultContent;

        public Builder setAttackPatterns(@NonNull List<AdVariant> patterns) {
            this.attackPatterns = patterns;
            return this;
        }

        public Builder setBannerDefaultContent(@NonNull AdContent content) {
            this.bannerDefaultContent = content;
            return this;
        }

        public Builder setInterstitialDefaultContent(@NonNull AdContent content) {
            this.interstitialDefaultContent = content;
            return this;
        }

        public Builder setAppOpenDefaultContent(@NonNull AdContent content) {
            this.appOpenDefaultContent = content;
            return this;
        }

        @NonNull
        public AdConfig build() {
            return new AdConfig(this);
        }
    }
}

