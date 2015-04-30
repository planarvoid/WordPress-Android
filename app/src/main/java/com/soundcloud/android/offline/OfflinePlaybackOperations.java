package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.likes.TrackLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackUtils;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class OfflinePlaybackOperations {

    private final TrackLikeOperations likeOperations;
    private final PlaylistOperations playlistOperations;
    private final FeatureOperations featureOperations;
    private final PlaybackOperations playbackOperations;
    private final NetworkConnectionHelper connectionHelper;
    private final OfflineTracksStorage offlineTracksStorage;
    private final Scheduler scheduler;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations,
                                     NetworkConnectionHelper connectionHelper,
                                     PlaybackOperations playbackOperations,
                                     TrackLikeOperations likeOperations,
                                     PlaylistOperations playlistOperations,
                                     OfflineTracksStorage offlineTracksStorage,
                                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.featureOperations = featureOperations;
        this.connectionHelper = connectionHelper;
        this.playbackOperations = playbackOperations;
        this.likeOperations = likeOperations;
        this.playlistOperations = playlistOperations;
        this.offlineTracksStorage = offlineTracksStorage;
        this.scheduler = scheduler;
    }

    public boolean shouldPlayOffline(PropertySet track) {
        return featureOperations.isOfflineContentEnabled()
                && track.getOrElse(OfflineProperty.DOWNLOAD_STATE, DownloadState.NO_OFFLINE) == DownloadState.DOWNLOADED;
    }

    public Observable<PlaybackResult> playLikes(final Urn trackUrn, final int position, final PlaySessionSource playSessionSource) {
        if (shouldCreateOfflinePlayQueue()) {
            return offlineTracksStorage.likesUrns()
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(playIfAvailableOffline(trackUrn, position, playSessionSource));
        }
        return playbackOperations.playTracks(likeOperations.likedTrackUrns(), trackUrn, position, playSessionSource);
    }

    public Observable<PlaybackResult> playPlaylist(Urn playlistUrn, Urn initialTrack, int position, PlaySessionSource sessionSource) {
        if (shouldCreateOfflinePlayQueue()) {
            return offlineTracksStorage.playlistTrackUrns(playlistUrn)
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(playIfAvailableOffline(initialTrack, position, sessionSource));
        }
        return playbackOperations
                .playTracks(playlistOperations.trackUrnsForPlayback(playlistUrn), initialTrack, position, sessionSource);
    }

    public Observable<PlaybackResult> playPlaylistShuffled(Urn playlistUrn, final PlaySessionSource sessionSource) {
        final Observable<List<Urn>> trackUrnsObservable = shouldCreateOfflinePlayQueue()
                ? offlineTracksStorage.playlistTrackUrns(playlistUrn)
                : playlistOperations.trackUrnsForPlayback(playlistUrn);

        return playbackOperations.playTracksShuffled(trackUrnsObservable.subscribeOn(scheduler), sessionSource);
    }

    private Func1<List<Urn>, Observable<PlaybackResult>> playIfAvailableOffline(final Urn trackUrn, final int position, final PlaySessionSource sessionSource) {
        return new Func1<List<Urn>, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(List<Urn> urns) {
                int corrected = PlaybackUtils.correctInitialPosition(urns, position, trackUrn);
                if (corrected < 0) {
                    throw new TrackNotAvailableOffline();
                }
                return playbackOperations.playTracks(urns, trackUrn, corrected, sessionSource);
            }
        };
    }

    public Observable<PlaybackResult> playLikedTracksShuffled(PlaySessionSource playSessionSource) {
        final Observable<List<Urn>> likedTracks = shouldCreateOfflinePlayQueue()
                ? offlineTracksStorage.likesUrns()
                : likeOperations.likedTrackUrns();

        return playbackOperations.playTracksShuffled(likedTracks.subscribeOn(scheduler), playSessionSource);
    }

    @VisibleForTesting
    boolean shouldCreateOfflinePlayQueue() {
        return featureOperations.isOfflineContentEnabled() && !connectionHelper.isNetworkConnected();
    }

    public static class TrackNotAvailableOffline extends IllegalStateException {}
}
