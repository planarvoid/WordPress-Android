package com.soundcloud.android.playback;

import static com.soundcloud.android.utils.Urns.optionalFromNotSetUrn;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.discovery.charts.ChartSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Arrays;
import java.util.List;

@AutoValue
public abstract class PlaybackContext {

    public enum Bucket {
        EXPLICIT,
        AUTO_PLAY,
        PLAYLIST,
        // TODO : ALBUM
        TRACK_STATION,
        ARTIST_STATION,
        PROFILE,
        CHARTS_TRENDING,
        CHARTS_TOP,
        LISTENING_HISTORY,
        SUGGESTED_TRACKS,
        STREAM(Screen.STREAM),
        LINK(Screen.DEEPLINK),
        YOUR_LIKES(Screen.LIKES, Screen.YOUR_LIKES),
        SEARCH_RESULT(Screen.SEARCH_EVERYTHING, Screen.SEARCH_PREMIUM_CONTENT, Screen.SEARCH_TRACKS),
        CAST,
        NEW_FOR_YOU,
        OTHER;

        private List<Screen> screens;

        Bucket(Screen... screens) {
            this.screens = Arrays.asList(screens);
        }

        static Bucket fromScreen(Screen screen) {
            for (Bucket bucket : values()) {
                if (bucket.screens.contains(screen)) {
                    return bucket;
                }
            }
            return OTHER;
        }
    }

    public static PlaybackContext create(PlaySessionSource playSessionSource) {
        return builder()
                .bucket(bucketFromPlaySessionSource(playSessionSource))
                .urn(urnFromPlaySessionSource(playSessionSource))
                .query(queryFromPlaySessionSource(playSessionSource))
                .build();
    }

    public static PlaybackContext create(Bucket bucket) {
        return create(bucket, Optional.absent());
    }

    public static PlaybackContext create(Bucket bucket, Optional<Urn> urn) {
        return builder()
                .bucket(bucket)
                .urn(urn)
                .query(Optional.absent())
                .build();
    }

    private static Optional<Urn> urnFromPlaySessionSource(PlaySessionSource playSessionSource) {
        if (playSessionSource.isFromChart()) {
            final ChartSourceInfo chartSourceInfo = playSessionSource.getChartSourceInfo();
            return optionalFromNotSetUrn(chartSourceInfo.getGenre());
        } else {
            return optionalFromNotSetUrn(playSessionSource.getCollectionUrn());
        }
    }

    private static Optional<String> queryFromPlaySessionSource(PlaySessionSource playSessionSource) {
        if (playSessionSource.isFromSearchQuery()) {
            final SearchQuerySourceInfo searchQuerySourceInfo = playSessionSource.getSearchQuerySourceInfo();

            if (searchQuerySourceInfo != null) {
                return Optional.fromNullable(searchQuerySourceInfo.getQueryString());
            }
        }
        return Optional.absent();
    }

    private static Bucket bucketFromPlaySessionSource(PlaySessionSource playSessionSource) {
        final String screenTag = playSessionSource.getOriginScreen();
        final Urn collectionUrn = playSessionSource.getCollectionUrn();
        final DiscoverySource discoverySource = playSessionSource.getDiscoverySource();

        if (collectionUrn.isPlaylist()) {
            return Bucket.PLAYLIST;
        } else if (collectionUrn.isUser()) {
            return Bucket.PROFILE;
        } else if (collectionUrn.isArtistStation()) {
            return Bucket.ARTIST_STATION;
        } else if (collectionUrn.isTrackStation()) {
            return Bucket.TRACK_STATION;
        } else if (playSessionSource.isFromChart()) {
            return bucketFromChart(playSessionSource.getChartSourceInfo());
        } else if (playSessionSource.isFromPlaylistHistory()) {
            return Bucket.LISTENING_HISTORY;
        } else if (playSessionSource.isFromRecommendations()) {
            return Bucket.SUGGESTED_TRACKS;
        } else if (DiscoverySource.CAST.equals(discoverySource)) {
            return Bucket.CAST;
        } else if (DiscoverySource.NEW_FOR_YOU.equals(discoverySource)) {
            return Bucket.NEW_FOR_YOU;
        } else {
            return Bucket.fromScreen(Screen.fromTag(screenTag));
        }
    }

    private static Bucket bucketFromChart(ChartSourceInfo chartSourceInfo) {
        switch (chartSourceInfo.getChartType()) {
            case TRENDING:
                return Bucket.CHARTS_TRENDING;
            case TOP:
                return Bucket.CHARTS_TOP;
            default:
                throw new IllegalArgumentException("Unknown chart type: " + chartSourceInfo.getChartType().name());
        }
    }

    public static Builder builder() {
        return new AutoValue_PlaybackContext.Builder();
    }

    public abstract Bucket bucket();

    public abstract Optional<Urn> urn();

    public abstract Optional<String> query();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder bucket(Bucket bucket);

        public abstract Builder urn(Optional<Urn> urn);

        public abstract Builder query(Optional<String> query);

        public abstract PlaybackContext build();
    }

}
