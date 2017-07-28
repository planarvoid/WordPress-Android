package com.soundcloud.android.navigation;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.deeplinks.ChartDetails;
import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.search.topresults.TopResultsBucketViewModel;
import com.soundcloud.android.utils.annotations.IgnoreHashEquals;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

@AutoValue
@SuppressWarnings("PMD.GodClass")
public abstract class NavigationTarget {

    // If you add a field here, be sure to add it to NavigationTargetMatcher, too!

    @IgnoreHashEquals public abstract Date creationDate();

    public abstract Optional<DeepLink> deeplink();

    public abstract Optional<LinkNavigationParameters> linkNavigationParameters();

    public abstract Optional<String> deeplinkTarget();

    public abstract Screen screen();

    public abstract Optional<String> referrer();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<Urn> targetUrn();

    public abstract Optional<DiscoverySource> discoverySource();

    public abstract Optional<TopResultsMetaData> topResultsMetaData();

    public abstract Optional<StationsInfoMetaData> stationsInfoMetaData();

    public abstract Optional<SearchQuerySourceInfo> searchQuerySourceInfo();

    public abstract Optional<PromotedSourceInfo> promotedSourceInfo();

    public abstract Optional<ChartsMetaData> chartsMetaData();

    public abstract Optional<NotificationPreferencesMetaData> notificationPreferencesMetaData();

    public abstract Optional<UIEvent> uiEvent();

    public abstract Optional<Recording> recording();

    public abstract Optional<OfflineSettingsMetaData> offlineSettingsMetaData();

    public abstract Builder toBuilder();

    // If you add a field here, be sure to add it to NavigationTargetMatcher, too!

    static Builder newBuilder() {
        return new AutoValue_NavigationTarget.Builder()
                .creationDate(new Date())
                .linkNavigationParameters(Optional.absent())
                .referrer(Optional.absent())
                .deeplink(Optional.absent())
                .deeplinkTarget(Optional.absent())
                .queryUrn(Optional.absent())
                .targetUrn(Optional.absent())
                .discoverySource(Optional.absent())
                .chartsMetaData(Optional.absent())
                .searchQuerySourceInfo(Optional.absent())
                .promotedSourceInfo(Optional.absent())
                .topResultsMetaData(Optional.absent())
                .stationsInfoMetaData(Optional.absent())
                .uiEvent(Optional.absent())
                .notificationPreferencesMetaData(Optional.absent())
                .recording(Optional.absent())
                .offlineSettingsMetaData(Optional.absent());
    }

    /**
     * Used for internal navigation using deeplink's (e.g. provided through Discovery Backend) and external links.
     * Similar to {@link #forExternalDeeplink(String, String)} but for in app navigation.
     */
    public static NavigationTarget forNavigation(@Nullable String target, Optional<String> fallback, Screen screen, Optional<DiscoverySource> discoverySource) {
        return newBuilder().linkNavigationParameters(LinkNavigationParameters.create(target, fallback))
                           .screen(screen)
                           .discoverySource(discoverySource)
                           .build();
    }

    /**
     * Used for internal navigation using deeplink's.
     * Similar to {@link #forExternalDeeplink(String, String)} but for in app navigation.
     */
    private static NavigationTarget forNavigationDeeplink(DeepLink deepLink, Screen screen) {
        return newBuilder().deeplink(Optional.of(deepLink))
                           .screen(screen)
                           .build();
    }

    /**
     * Used for navigation from a real deeplink.
     * Similar to {@link #forNavigation(String, Optional, Screen, Optional)} but for real deeplink navigation.
     */
    public static NavigationTarget forExternalDeeplink(@Nullable String target, String referrer) {
        return newBuilder().linkNavigationParameters(LinkNavigationParameters.create(target))
                           .screen(Screen.DEEPLINK)
                           .referrer(Optional.of(referrer))
                           .build();
    }

    public static NavigationTarget forNotificationPreferences() {
        return forNavigationDeeplink(DeepLink.NOTIFICATION_PREFERENCES, Screen.UNKNOWN)
                .toBuilder()
                .notificationPreferencesMetaData(Optional.of(NotificationPreferencesMetaData.create(true)))
                .build();
    }

    public static NavigationTarget forLegal() {
        return forNavigationDeeplink(DeepLink.LEGAL, Screen.UNKNOWN);
    }

    public static NavigationTarget forHelpCenter() {
        return forNavigationDeeplink(DeepLink.HELP_CENTER, Screen.UNKNOWN);
    }

    public static NavigationTarget forPlaylistsAndAlbumsCollection() {
        return forNavigationDeeplink(DeepLink.PLAYLISTS_AND_ALBUMS_COLLECTION, Screen.PLAYLISTS);
    }

    public static NavigationTarget forPlaylistsCollection() {
        return forNavigationDeeplink(DeepLink.PLAYLISTS_COLLECTION, Screen.PLAYLISTS);
    }

    public static NavigationTarget forLegacyPlaylist(Urn urn, Screen screen) {
        return forPlaylist(urn, screen, Optional.absent(), Optional.absent(), Optional.absent());
    }

    public static NavigationTarget forLegacyPlaylist(Urn urn, Screen screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo, Optional<PromotedSourceInfo> promotedSourceInfo) {
        return forPlaylist(urn, screen, searchQuerySourceInfo, promotedSourceInfo, Optional.absent());
    }

    public static NavigationTarget forPlaylist(Urn entityUrn,
                                               Screen screen,
                                               Optional<SearchQuerySourceInfo> searchQuerySourceInfo,
                                               Optional<PromotedSourceInfo> promotedSourceInfo,
                                               Optional<UIEvent> uiEvent) {
        return forNavigationDeeplink(DeepLink.PLAYLISTS, screen)
                .toBuilder()
                .uiEvent(uiEvent)
                .targetUrn(Optional.of(entityUrn))
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .promotedSourceInfo(promotedSourceInfo)
                .build();
    }

    public static NavigationTarget forActivities() {
        return forNavigationDeeplink(DeepLink.ACTIVITIES, Screen.UNKNOWN);
    }

    public static NavigationTarget forBasicSettings() {
        return forNavigationDeeplink(DeepLink.BASIC_SETTINGS, Screen.UNKNOWN);
    }

    public static NavigationTarget forOfflineSettings(boolean showOnboarding) {
        return forNavigationDeeplink(DeepLink.OFFLINE_SETTINGS, Screen.UNKNOWN)
                .toBuilder()
                .offlineSettingsMetaData(Optional.of(OfflineSettingsMetaData.create(showOnboarding)))
                .build();
    }

    public static NavigationTarget forRecord(Optional<Recording> recording, Optional<Screen> screen) {
        return forNavigationDeeplink(DeepLink.RECORD, screen.or(Screen.UNKNOWN))
                .toBuilder()
                .recording(recording)
                .build();
    }

    public static NavigationTarget forFollowers(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forNavigationDeeplink(DeepLink.FOLLOWERS, Screen.UNKNOWN)
                .toBuilder()
                .targetUrn(Optional.of(userUrn))
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .build();
    }

    public static NavigationTarget forFollowings(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forNavigationDeeplink(DeepLink.FOLLOWINGS, Screen.UNKNOWN)
                .toBuilder()
                .targetUrn(Optional.of(userUrn))
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .build();
    }

    public static NavigationTarget forFullscreenVideoAd(Urn adUrn) {
        return forNavigationDeeplink(DeepLink.AD_FULLSCREEN_VIDEO, Screen.UNKNOWN)
                .toBuilder()
                .targetUrn(Optional.of(adUrn))
                .build();
    }

    public static NavigationTarget forAdClickthrough(String url) {
        return forNavigationDeeplink(DeepLink.AD_CLICKTHROUGH, Screen.UNKNOWN)
                .toBuilder()
                .deeplinkTarget(Optional.of(url))
                .build();
    }

    public static NavigationTarget forPrestitialAd() {
        return forNavigationDeeplink(DeepLink.AD_PRESTITIAL, Screen.UNKNOWN);
    }

    public static NavigationTarget forSearchViewAll(Optional<Urn> queryUrn, String query, TopResultsBucketViewModel.Kind kind, boolean isPremium) {
        return forNavigationDeeplink(DeepLink.SEARCH_RESULTS_VIEW_ALL, Screen.UNKNOWN)
                .toBuilder()
                .queryUrn(queryUrn)
                .topResultsMetaData(Optional.of(TopResultsMetaData.create(query, kind, isPremium)))
                .build();
    }

    public static NavigationTarget forSearchAutocomplete(Screen screen) {
        return forNavigationDeeplink(DeepLink.SEARCH_AUTOCOMPLETE, screen);
    }

    public static NavigationTarget forProfile(Urn userUrn) {
        return forProfile(userUrn, Optional.absent(), Optional.absent(), Optional.absent());
    }

    public static NavigationTarget forProfile(Urn userUrn, Optional<UIEvent> uiEvent, Optional<Screen> screen, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forNavigationDeeplink(DeepLink.PROFILE, screen.or(Screen.UNKNOWN))
                .toBuilder()
                .targetUrn(Optional.of(userUrn))
                .uiEvent(uiEvent)
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .build();
    }

    public static NavigationTarget forProfileReposts(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forProfileSubScreen(userUrn, searchQuerySourceInfo, DeepLink.PROFILE_REPOSTS, Screen.USERS_REPOSTS);
    }

    public static NavigationTarget forProfileTracks(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forProfileSubScreen(userUrn, searchQuerySourceInfo, DeepLink.PROFILE_TRACKS, Screen.USER_TRACKS);
    }

    public static NavigationTarget forProfileLikes(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forProfileSubScreen(userUrn, searchQuerySourceInfo, DeepLink.PROFILE_LIKES, Screen.USER_LIKES);
    }

    public static NavigationTarget forProfileAlbums(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forProfileSubScreen(userUrn, searchQuerySourceInfo, DeepLink.PROFILE_ALBUMS, Screen.USER_ALBUMS);
    }

    public static NavigationTarget forProfilePlaylists(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo) {
        return forProfileSubScreen(userUrn, searchQuerySourceInfo, DeepLink.PROFILE_PLAYLISTS, Screen.USER_PLAYLISTS);
    }

    private static NavigationTarget forProfileSubScreen(Urn userUrn, Optional<SearchQuerySourceInfo> searchQuerySourceInfo, DeepLink deepLink, Screen screen) {
        return forNavigationDeeplink(deepLink, screen)
                .toBuilder()
                .targetUrn(Optional.of(userUrn))
                .searchQuerySourceInfo(searchQuerySourceInfo)
                .build();
    }

    public static NavigationTarget forChart(ChartType type, Urn genre, ChartCategory category, @Nullable String header) {
        return forNavigationDeeplink(DeepLink.CHARTS, Screen.UNKNOWN)
                .toBuilder()
                .chartsMetaData(Optional.of(ChartsMetaData.create(type, genre, category, Optional.fromNullable(header))))
                .build();
    }

    public static NavigationTarget forAllGenres() {
        return forAllGenres(null);
    }

    public static NavigationTarget forAllGenres(@Nullable ChartCategory category) {
        return forNavigationDeeplink(DeepLink.CHARTS_ALL_GENRES, Screen.UNKNOWN)
                .toBuilder()
                .chartsMetaData(Optional.of(ChartsMetaData.create(Optional.fromNullable(category))))
                .build();
    }

    public static NavigationTarget forLikedStations() {
        return forNavigationDeeplink(DeepLink.LIKED_STATIONS, Screen.UNKNOWN);
    }

    public static NavigationTarget forStationInfo(Urn stationUrn, Optional<Urn> seedTrack, Optional<DiscoverySource> source, Optional<UIEvent> uiEvent) {
        return forNavigationDeeplink(DeepLink.STATION, Screen.UNKNOWN)
                .toBuilder()
                .discoverySource(source)
                .uiEvent(uiEvent)
                .targetUrn(Optional.of(stationUrn))
                .stationsInfoMetaData(Optional.of(StationsInfoMetaData.create(seedTrack)))
                .build();
    }

    public static NavigationTarget forExternalPackage(String packageNameCreators) {
        return forNavigationDeeplink(DeepLink.EXTERNAL_APP, Screen.UNKNOWN)
                .toBuilder()
                .deeplinkTarget(Optional.of(packageNameCreators))
                .build();
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
        abstract Builder creationDate(Date creationDate);

        abstract Builder screen(Screen screen);

        abstract Builder linkNavigationParameters(Optional<LinkNavigationParameters> linkNavigationParameters);

        abstract Builder referrer(Optional<String> referrer);

        abstract Builder discoverySource(Optional<DiscoverySource> discoverySource);

        abstract NavigationTarget build();

        // Optional arguments depending on the started context

        abstract Builder topResultsMetaData(Optional<TopResultsMetaData> metaData);

        abstract Builder stationsInfoMetaData(Optional<StationsInfoMetaData> metaData);

        abstract Builder deeplink(Optional<DeepLink> deepLink);

        abstract Builder deeplinkTarget(Optional<String> deepLinkTarget);

        abstract Builder queryUrn(Optional<Urn> queryUrn);

        abstract Builder targetUrn(Optional<Urn> targetUrn);

        abstract Builder searchQuerySourceInfo(Optional<SearchQuerySourceInfo> searchQuerySourceInfo);

        abstract Builder promotedSourceInfo(Optional<PromotedSourceInfo> promotedSourceInfo);

        abstract Builder chartsMetaData(Optional<ChartsMetaData> chartsMetaData);

        abstract Builder uiEvent(Optional<UIEvent> uiEvent);

        abstract Builder notificationPreferencesMetaData(Optional<NotificationPreferencesMetaData> notificationPreferencesMetaData);

        abstract Builder recording(Optional<Recording> recording);

        abstract Builder offlineSettingsMetaData(Optional<OfflineSettingsMetaData> offlineSettingsMetaData);

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
    abstract static class ChartsMetaData {
        abstract Optional<ChartCategory> category();

        abstract Optional<ChartDetails> chartDetails();

        static ChartsMetaData create(Optional<ChartCategory> category) {
            return new AutoValue_NavigationTarget_ChartsMetaData(category, Optional.absent());
        }

        static ChartsMetaData create(ChartType type, Urn genre, ChartCategory category, Optional<String> title) {
            return new AutoValue_NavigationTarget_ChartsMetaData(Optional.absent(), Optional.of(ChartDetails.create(type, genre, category, title)));
        }
    }

    @AutoValue
    abstract static class TopResultsMetaData {
        abstract String query();

        abstract TopResultsBucketViewModel.Kind kind();

        abstract boolean isPremium();

        static TopResultsMetaData create(String query, TopResultsBucketViewModel.Kind kind, boolean isPremium) {
            return new AutoValue_NavigationTarget_TopResultsMetaData(query, kind, isPremium);
        }
    }

    @AutoValue
    abstract static class StationsInfoMetaData {
        abstract Optional<Urn> seedTrack();

        static StationsInfoMetaData create(Optional<Urn> seedTrack) {
            return new AutoValue_NavigationTarget_StationsInfoMetaData(seedTrack);
        }
    }

    @AutoValue
    abstract static class NotificationPreferencesMetaData {
        abstract boolean isNavigationDeeplink();

        static NotificationPreferencesMetaData create(boolean isNavigationDeeplink) {
            return new AutoValue_NavigationTarget_NotificationPreferencesMetaData(isNavigationDeeplink);
        }
    }

    @AutoValue
    abstract static class OfflineSettingsMetaData {
        public abstract boolean showOnboarding();

        static OfflineSettingsMetaData create(boolean showOnboarding) {
            return new AutoValue_NavigationTarget_OfflineSettingsMetaData(showOnboarding);
        }
    }
}
