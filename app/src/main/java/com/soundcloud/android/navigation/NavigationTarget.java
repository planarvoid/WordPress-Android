package com.soundcloud.android.navigation;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnCollection;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.search.topresults.TopResults;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@AutoValue
public abstract class NavigationTarget {
    public abstract Activity activity();

    public abstract Optional<DeepLink> deeplink();

    public abstract Optional<LinkNavigationParameters> linkNavigationParameters();

    public abstract Optional<String> deeplinkTarget();

    public abstract Screen screen();

    public abstract Optional<String> referrer();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<Urn> targetUrn();

    public abstract Optional<DiscoverySource> discoverySource();

    public abstract Optional<TopResultsMetaData> topResultsMetaData();

    public abstract Optional<SearchQuerySourceInfo> searchQuerySourceInfo();

    public abstract Optional<UIEvent> uiEvent();

    abstract Builder toBuilder();

    static Builder newBuilder() {
        return new AutoValue_NavigationTarget.Builder()
                .linkNavigationParameters(Optional.absent())
                .referrer(Optional.absent())
                .deeplink(Optional.absent())
                .deeplinkTarget(Optional.absent())
                .queryUrn(Optional.absent())
                .targetUrn(Optional.absent())
                .discoverySource(Optional.absent())
                .searchQuerySourceInfo(Optional.absent())
                .topResultsMetaData(Optional.absent())
                .uiEvent(Optional.absent());
    }

    /**
     * Used for internal navigation using deeplink's (e.g. provided through Discovery Backend) and external links.
     * Similar to {@link #forExternalDeeplink(Activity, String, String)} but for in app navigation.
     */
    public static NavigationTarget forNavigation(Activity activity, @Nullable String target, Optional<String> fallback, Screen screen, Optional<DiscoverySource> discoverySource) {
        return newBuilder().activity(activity)
                           .linkNavigationParameters(LinkNavigationParameters.create(target, fallback))
                           .screen(screen)
                           .discoverySource(discoverySource)
                           .build();
    }

    /**
     * Used for internal navigation using deeplink's.
     * Similar to {@link #forExternalDeeplink(Activity, String, String)} but for in app navigation.
     */
    private static NavigationTarget forNavigationDeeplink(Activity activity, DeepLink deepLink, Screen screen) {
        return newBuilder().activity(activity)
                           .deeplink(Optional.of(deepLink))
                           .screen(screen)
                           .build();
    }

    /**
     * Used for navigation from a real deeplink.
     * Similar to {@link #forNavigation(Activity, String, Optional, Screen, Optional)} but for real deeplink navigation.
     */
    public static NavigationTarget forExternalDeeplink(Activity activity, @Nullable String target, String referrer) {
        return newBuilder().activity(activity)
                           .linkNavigationParameters(LinkNavigationParameters.create(target))
                           .screen(Screen.DEEPLINK)
                           .referrer(Optional.of(referrer))
                           .build();
    }

    /**
     * Used for navigation based on a supported URN (Track, User, Playlist, SystemPlaylist).
     *
     * @param urn that should be open
     * @return a {@link NavigationTarget} to open the desired urn
     * @throws IllegalArgumentException if the passed in URN is not suppred
     * @see {@link NavigationResolver#navigateToResource(NavigationTarget, Urn)} for supported URNs
     */
    public static NavigationTarget forUrn(Activity activity, Urn urn, Screen screen) {
        Preconditions.checkArgument(urn.isTrack() || urn.isUser() || urn.isPlaylist() || urn.isSystemPlaylist(), "URN navigation for " + UrnCollection.from(urn) + " not supported.");
        return forNavigation(activity, urn.toString(), Optional.absent(), screen, Optional.absent());
    }

    public static NavigationTarget forActivities(Activity activity) {
        return forNavigationDeeplink(activity, DeepLink.ACTIVITIES, Screen.UNKNOWN);
    }

    public static NavigationTarget forFollowers(Activity activity, Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forNavigationDeeplink(activity, DeepLink.FOLLOWERS, Screen.UNKNOWN)
                .toBuilder()
                .targetUrn(Optional.of(userUrn))
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .build();
    }

    public static NavigationTarget forFollowings(Activity activity, Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forNavigationDeeplink(activity, DeepLink.FOLLOWINGS, Screen.UNKNOWN)
                .toBuilder()
                .targetUrn(Optional.of(userUrn))
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .build();
    }

    public static NavigationTarget forFullscreenVideoAd(Activity activity, Urn adUrn) {
        return forNavigationDeeplink(activity, DeepLink.AD_FULLSCREEN_VIDEO, Screen.UNKNOWN)
                .toBuilder()
                .targetUrn(Optional.of(adUrn))
                .build();
    }

    public static NavigationTarget forAdClickthrough(Activity activity, String url) {
        return forNavigationDeeplink(activity, DeepLink.AD_CLICKTHROUGH, Screen.UNKNOWN)
                .toBuilder()
                .deeplinkTarget(Optional.of(url))
                .build();
    }

    public static NavigationTarget forSearchViewAll(Activity activity, Optional<Urn> queryUrn, String query, TopResults.Bucket.Kind kind, boolean isPremium) {
        return forNavigationDeeplink(activity, DeepLink.SEARCH_RESULTS_VIEW_ALL, Screen.UNKNOWN)
                .toBuilder()
                .queryUrn(queryUrn)
                .topResultsMetaData(Optional.of(TopResultsMetaData.create(query, kind, isPremium)))
                .build();
    }

    public static NavigationTarget forSearchAutocomplete(Activity activity, Screen screen) {
        return forNavigationDeeplink(activity, DeepLink.SEARCH_AUTOCOMPLETE, screen);
    }

    public static NavigationTarget forProfile(Activity activity, Urn userUrn) {
        return forProfile(activity, userUrn, Optional.absent(), Optional.absent(), Optional.absent());
    }

    public static NavigationTarget forProfile(Activity activity, Urn userUrn, Optional<UIEvent> uiEvent, Optional<Screen> screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forNavigationDeeplink(activity, DeepLink.PROFILE, screen.or(Screen.UNKNOWN))
                .toBuilder()
                .targetUrn(Optional.of(userUrn))
                .uiEvent(uiEvent)
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .build();
    }

    public static NavigationTarget forPrestitialAd(Activity activity) {
        return forNavigationDeeplink(activity, DeepLink.AD_PRESTITIAL, Screen.UNKNOWN);
    }

    NavigationTarget withScreen(Screen screen) {
        return toBuilder().screen(screen).build();
    }

    NavigationTarget withTarget(String target) {
        return toBuilder().linkNavigationParameters(LinkNavigationParameters.create(target)).build();
    }

    NavigationTarget withFallback(Optional<String> fallback) {
        return toBuilder().linkNavigationParameters(linkNavigationParameters().get().withFallback(fallback)).build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder activity(Activity activity);

        abstract Builder screen(Screen screen);

        abstract Builder linkNavigationParameters(Optional<LinkNavigationParameters> linkNavigationParameters);

        abstract Builder referrer(Optional<String> referrer);

        abstract Builder discoverySource(Optional<DiscoverySource> discoverySource);

        abstract NavigationTarget build();

        // Optional arguments depending on the started context

        abstract Builder topResultsMetaData(Optional<TopResultsMetaData> metaData);

        abstract Builder deeplink(Optional<DeepLink> deepLink);

        abstract Builder deeplinkTarget(Optional<String> deepLinkTarget);

        abstract Builder queryUrn(Optional<Urn> queryUrn);

        abstract Builder targetUrn(Optional<Urn> targetUrn);

        abstract Builder searchQuerySourceInfo(Optional<SearchQuerySourceInfo> searchQuerySourceInfo);

        abstract Builder uiEvent(Optional<UIEvent> uiEvent);

        Builder linkNavigationParameters(@Nullable LinkNavigationParameters parameters) {
            return linkNavigationParameters(Optional.fromNullable(parameters));
        }

        Builder discoverySource(@Nullable DiscoverySource discoverySource) {
            return discoverySource(Optional.fromNullable(discoverySource));
        }
    }

    @AutoValue
    public abstract static class LinkNavigationParameters {
        @Nullable
        public abstract String target();

        public abstract Optional<String> fallback();

        static LinkNavigationParameters create(String target) {
            return new AutoValue_NavigationTarget_LinkNavigationParameters(target, Optional.absent());
        }

        static LinkNavigationParameters create(String target, @NonNull String fallback) {
            return new AutoValue_NavigationTarget_LinkNavigationParameters(target, Optional.of(fallback));
        }

        static LinkNavigationParameters create(String target, Optional<String> fallback) {
            return new AutoValue_NavigationTarget_LinkNavigationParameters(target, fallback);
        }

        LinkNavigationParameters withFallback(String fallback) {
            return new AutoValue_NavigationTarget_LinkNavigationParameters(target(), Optional.of(fallback));
        }

        LinkNavigationParameters withFallback(Optional<String> fallback) {
            return new AutoValue_NavigationTarget_LinkNavigationParameters(target(), fallback);
        }

        Uri targetUri() {
            return Uri.parse(target());
        }
    }

    @AutoValue
    abstract static class TopResultsMetaData {
        abstract String query();

        abstract TopResults.Bucket.Kind kind();

        abstract boolean isPremium();

        static TopResultsMetaData create(String query, TopResults.Bucket.Kind kind, boolean isPremium) {
            return new AutoValue_NavigationTarget_TopResultsMetaData(query, kind, isPremium);
        }
    }
}
