package com.soundcloud.android.navigation;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnCollection;
import com.soundcloud.android.search.topresults.TopResults;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.Nullable;

@AutoValue
public abstract class NavigationTarget {
    public abstract Activity activity();
    @Nullable
    public abstract String target();
    public abstract Optional<String> fallback();
    public abstract Optional<DeepLink> deeplink();
    public abstract Screen screen();
    public abstract Optional<String> referrer();
    public abstract Optional<Urn> queryUrn();
    public abstract Optional<TopResultsMetaData> topResultsMetaData();
    public abstract Builder toBuilder();

    public static Builder newBuilder() {
        return new AutoValue_NavigationTarget.Builder()
                .referrer(Optional.absent())
                .fallback(Optional.absent())
                .deeplink(Optional.absent())
                .queryUrn(Optional.absent())
                .topResultsMetaData(Optional.absent());
    }

    /**
     * Used for internal navigation using deeplink's (e.g. provided through Discovery Backend) and external links.
     * Similar to {@link #forExternalDeeplink(Activity, String, String)} but for in app navigation.
     */
    public static NavigationTarget forNavigation(Activity activity, @Nullable String target, Optional<String> fallback, Screen screen) {
        return newBuilder().activity(activity)
                           .target(target)
                           .fallback(fallback)
                           .screen(screen)
                           .build();
    }

    /**
     * Used for internal navigation using deeplink's.
     * Similar to {@link #forExternalDeeplink(Activity, String, String)} but for in app navigation.
     */
    private static NavigationTarget forNavigationDeeplink(Activity activity, DeepLink deepLink, Screen screen) {
        return newBuilder().activity(activity)
                           .target(null)
                           .deeplink(Optional.of(deepLink))
                           .screen(screen)
                           .build();
    }

    /**
     * Used for navigation from a real deeplink.
     * Similar to {@link #forNavigation(Activity, String, Optional, Screen)} but for real deeplink navigation.
     */
    public static NavigationTarget forExternalDeeplink(Activity activity, @Nullable String target, String referrer) {
        return newBuilder().activity(activity)
                           .target(target)
                           .screen(Screen.DEEPLINK)
                           .referrer(Optional.of(referrer))
                           .build();
    }

    /**
     * Used for navigation based on a supported URN (Track, User, Playlist, SystemPlaylist).
     *
     * @param urn that should be open
     * @return a {@link NavigationTarget} to open the desired urn
     *
     * @throws IllegalArgumentException if the passed in URN is not suppred
     * @see {@link NavigationResolver#navigateToResource(NavigationTarget, Urn)} for supported URNs
     */
    public static NavigationTarget forUrn(Activity activity, Urn urn, Screen screen) {
        Preconditions.checkArgument(urn.isTrack() || urn.isUser() || urn.isPlaylist() || urn.isSystemPlaylist(), "URN navigation for " + UrnCollection.from(urn) + " not supported.");
        return forNavigation(activity, urn.toString(), Optional.absent(), screen);
    }

    public static NavigationTarget forActivities(Activity activity) {
        return forNavigationDeeplink(activity, DeepLink.ACTIVITIES, Screen.UNKNOWN);
    }

    public static NavigationTarget forSearchViewAll(Activity activity, Optional<Urn> queryUrn, String query, TopResults.Bucket.Kind kind, boolean isPremium) {
        return forNavigationDeeplink(activity, DeepLink.SEARCH_RESULTS_VIEW_ALL, Screen.UNKNOWN)
                .toBuilder()
                .queryUrn(queryUrn)
                .topResultsMetaData(Optional.of(TopResultsMetaData.create(query, kind, isPremium)))
                .build();
    }

    public NavigationTarget withScreen(Screen screen) {
        return toBuilder().screen(screen).build();
    }

    public Uri targetUri() {
        return Uri.parse(target());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder activity(Activity activity);
        public abstract Builder target(@Nullable String target);
        public abstract Builder fallback(Optional<String> fallback);
        public abstract Builder screen(Screen screen);
        public abstract Builder referrer(Optional<String> referrer);
        public abstract NavigationTarget build();

        // Optional arguments depending on the started context

        abstract Builder topResultsMetaData(Optional<TopResultsMetaData> metaData);
        abstract Builder deeplink(Optional<DeepLink> deepLink);
        abstract Builder queryUrn(Optional<Urn> queryUrn);
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
