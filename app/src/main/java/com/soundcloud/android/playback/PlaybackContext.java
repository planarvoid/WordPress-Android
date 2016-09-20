package com.soundcloud.android.playback;

import static com.soundcloud.android.utils.Urns.optionalFromNotSetUrn;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.discovery.ChartSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

@AutoValue
public abstract class PlaybackContext {

    enum Bucket {
        EXPLICIT,
        AUTO_PLAY,
        PLAYLIST,
        TRACK_STATION,
        ARTIST_STATION,
        PROFILE,
        NEW_AND_HOT_CHARTS,
        TOP_50_CHARTS,
        LISTENING_HISTORY,
        SUGGESTED_TRACKS,
        STREAM(Screen.STREAM),
        LINK(Screen.DEEPLINK),
        YOUR_LIKES(Screen.LIKES, Screen.YOUR_LIKES),
        SEARCH_RESULT(Screen.SEARCH_EVERYTHING, Screen.SEARCH_PREMIUM_CONTENT, Screen.SEARCH_TRACKS),
        OTHER;

        private List<Screen> screens;

        Bucket(Screen... screens) {
            this.screens = Arrays.asList(screens);
        }

        @Nullable
        static Bucket fromScreen(Screen screen) {
            for (Bucket bucket : values()) {
                if (bucket.screens.contains(screen)) {
                    return bucket;
                }
            }
            return Bucket.OTHER;
        }
    }

    public static PlaybackContext create(PlaySessionSource playSessionSource) {
        return builder()
                .bucket(bucketFromPlaySessionSource(playSessionSource))
                .urn(urnFromPlaySessionSource(playSessionSource))
                .query(queryFromPlaySessionSource(playSessionSource))
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
        } else {
            return Bucket.fromScreen(Screen.fromTag(screenTag));
        }
    }

    private static Bucket bucketFromChart(ChartSourceInfo chartSourceInfo) {
        switch (chartSourceInfo.getChartType()) {
            case TRENDING:
                return Bucket.NEW_AND_HOT_CHARTS;
            case TOP:
                return Bucket.TOP_50_CHARTS;
            default:
                return Bucket.OTHER;
        }
    }

    static Builder builder() {
        return new AutoValue_PlaybackContext.Builder();
    }

    public abstract Bucket bucket();

    public abstract Optional<Urn> urn();

    public abstract Optional<String> query();

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder bucket(Bucket bucket);

        abstract Builder urn(Optional<Urn> urn);

        abstract Builder query(Optional<String> query);

        abstract PlaybackContext build();
    }

}
