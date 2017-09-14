package com.soundcloud.android.playback;

import static com.soundcloud.android.utils.Urns.optionalFromNotSetUrn;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
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
        TRACK_STATION,
        ARTIST_STATION,
        PROFILE,
        LISTENING_HISTORY,
        STREAM(Screen.STREAM),
        LINK(Screen.DEEPLINK),
        YOUR_LIKES(Screen.LIKES, Screen.YOUR_LIKES),
        SEARCH_RESULT(Screen.SEARCH_EVERYTHING, Screen.SEARCH_PREMIUM_CONTENT, Screen.SEARCH_TRACKS),
        CAST,
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
        return optionalFromNotSetUrn(playSessionSource.getCollectionUrn());
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
        } else if (playSessionSource.isFromPlaylistHistory()) {
            return Bucket.LISTENING_HISTORY;
        } else if (DiscoverySource.CAST.equals(discoverySource)) {
            return Bucket.CAST;
        } else {
            return Bucket.fromScreen(Screen.fromTag(screenTag));
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
