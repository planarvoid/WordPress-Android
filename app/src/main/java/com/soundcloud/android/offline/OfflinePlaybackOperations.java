package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.likes.TrackLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackUtils;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class OfflinePlaybackOperations {

    private final TrackLikeOperations likeOperations;
    private final PlaylistOperations playlistOperations;
    private final FeatureOperations featureOperations;
    private final PlaybackOperations playbackOperations;
    private final NetworkConnectionHelper connectionHelper;
    private final OfflineTracksStorage offlineTracksStorage;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations,
                                     NetworkConnectionHelper connectionHelper,
                                     PlaybackOperations playbackOperations,
                                     TrackLikeOperations likeOperations,
                                     PlaylistOperations playlistOperations,
                                     OfflineTracksStorage offlineTracksStorage) {
        this.featureOperations = featureOperations;
        this.connectionHelper = connectionHelper;
        this.playbackOperations = playbackOperations;
        this.likeOperations = likeOperations;
        this.playlistOperations = playlistOperations;
        this.offlineTracksStorage = offlineTracksStorage;
    }

    public boolean shouldPlayOffline(PropertySet track) {
        return featureOperations.isOfflineContentEnabled()
                && track.getOrElseNull(OfflineProperty.DOWNLOADED_AT) != null
                && track.getOrElseNull(OfflineProperty.REMOVED_AT) == null;
    }

    public Observable<List<Urn>> playLikes(final Urn trackUrn, final int position, final PlaySessionSource playSessionSource) {
        if (shouldCreateOfflinePlayQueue()) {
            return offlineTracksStorage.likesUrns()
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(playIfAvailableOffline(trackUrn, position, playSessionSource));
        }
        return playbackOperations.playTracks(likeOperations.likedTrackUrns(), trackUrn, position, playSessionSource);
    }

    public Observable<List<Urn>> playPlaylist(Urn playlistUrn, Urn initialTrack, int position, PlaySessionSource sessionSource) {
        if (shouldCreateOfflinePlayQueue()) {
            return offlineTracksStorage.playlistTrackUrns(playlistUrn)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(playIfAvailableOffline(initialTrack, position, sessionSource));
        }
        return playbackOperations
                .playTracks(playlistOperations.trackUrnsForPlayback(playlistUrn), initialTrack, position, sessionSource);
    }

    public Observable<List<Urn>> playPlaylistShuffled(Urn playlistUrn, final PlaySessionSource sessionSource) {
        final Observable<List<Urn>> trackUrnsObservable = shouldCreateOfflinePlayQueue()
                ? offlineTracksStorage.playlistTrackUrns(playlistUrn)
                : playlistOperations.trackUrnsForPlayback(playlistUrn);

        return playbackOperations.playTracksShuffled(trackUrnsObservable, sessionSource);
    }

    private Func1<List<Urn>, Observable<List<Urn>>> playIfAvailableOffline(final Urn trackUrn, final int position, final PlaySessionSource sessionSource) {
        return new Func1<List<Urn>, Observable<List<Urn>>>() {
            @Override
            public Observable<List<Urn>> call(List<Urn> urns) {
                int corrected = PlaybackUtils.correctInitialPosition(urns, position, trackUrn);
                if (corrected < 0) {
                    throw new TrackNotAvailableOffline();
                }
                return playbackOperations.playTracks(urns, trackUrn, corrected, sessionSource);
            }
        };
    }

    public Observable<List<Urn>> playLikedTracksShuffled(PlaySessionSource playSessionSource) {
        final Observable<List<Urn>> likedTracks = shouldCreateOfflinePlayQueue()
                ? offlineTracksStorage.likesUrns()
                : likeOperations.likedTrackUrns();

        return playbackOperations.playTracksShuffled(likedTracks, playSessionSource);
    }

    @VisibleForTesting
    boolean shouldCreateOfflinePlayQueue() {
        return featureOperations.isOfflineContentEnabled() && !connectionHelper.isNetworkConnected();
    }

    public static class TrackNotAvailableOffline extends IllegalStateException {}
}
