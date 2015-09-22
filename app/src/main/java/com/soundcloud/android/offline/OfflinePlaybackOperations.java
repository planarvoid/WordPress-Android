package com.soundcloud.android.offline;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.likes.TrackLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackUtils;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class OfflinePlaybackOperations {

    private final TrackLikeOperations likeOperations;
    private final PlaylistOperations playlistOperations;
    private final FeatureOperations featureOperations;
    private final PlaybackInitiator playbackInitiator;
    private final NetworkConnectionHelper connectionHelper;
    private final TrackDownloadsStorage trackDownloadsStorage;
    private final Scheduler scheduler;

    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations,
                                     NetworkConnectionHelper connectionHelper,
                                     PlaybackInitiator playbackInitiator,
                                     TrackLikeOperations likeOperations,
                                     PlaylistOperations playlistOperations,
                                     TrackDownloadsStorage trackDownloadsStorage,
                                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.featureOperations = featureOperations;
        this.connectionHelper = connectionHelper;
        this.playbackInitiator = playbackInitiator;
        this.likeOperations = likeOperations;
        this.playlistOperations = playlistOperations;
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.scheduler = scheduler;
    }

    public boolean shouldPlayOffline(PropertySet track) {
        return featureOperations.isOfflineContentEnabled()
                && track.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NO_OFFLINE) == OfflineState.DOWNLOADED;
    }

    public Observable<PlaybackResult> playLikes(final Urn trackUrn, final int position, final PlaySessionSource playSessionSource) {
        if (shouldCreateOfflinePlayQueue()) {
            return trackDownloadsStorage.likesUrns()
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(playIfAvailableOffline(trackUrn, position, playSessionSource));
        }
        return playbackInitiator.playTracks(likeOperations.likedTrackUrns(), trackUrn, position, playSessionSource);
    }

    public Observable<PlaybackResult> playPlaylist(Urn playlistUrn, Urn initialTrack, int position, PlaySessionSource sessionSource) {
        if (shouldCreateOfflinePlayQueue()) {
            return trackDownloadsStorage.playlistTrackUrns(playlistUrn)
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(playIfAvailableOffline(initialTrack, position, sessionSource));
        }
        return playbackInitiator
                .playTracks(playlistOperations.trackUrnsForPlayback(playlistUrn), initialTrack, position, sessionSource);
    }

    public Observable<PlaybackResult> playPlaylistShuffled(Urn playlistUrn, final PlaySessionSource sessionSource) {
        final Observable<List<Urn>> trackUrnsObservable = shouldCreateOfflinePlayQueue()
                ? trackDownloadsStorage.playlistTrackUrns(playlistUrn)
                : playlistOperations.trackUrnsForPlayback(playlistUrn);

        return playbackInitiator.playTracksShuffled(trackUrnsObservable.subscribeOn(scheduler), sessionSource);
    }

    private Func1<List<Urn>, Observable<PlaybackResult>> playIfAvailableOffline(final Urn trackUrn, final int position, final PlaySessionSource sessionSource) {
        return new Func1<List<Urn>, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(List<Urn> urns) {
                int corrected = PlaybackUtils.correctInitialPositionLegacy(urns, position, trackUrn);
                if (corrected < 0) {
                    return Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_OFFLINE));
                }
                return playbackInitiator.playTracks(urns, trackUrn, corrected, sessionSource);
            }
        };
    }

    public Observable<PlaybackResult> playLikedTracksShuffled(PlaySessionSource playSessionSource) {
        final Observable<List<Urn>> likedTracks = shouldCreateOfflinePlayQueue()
                ? trackDownloadsStorage.likesUrns()
                : likeOperations.likedTrackUrns();

        return playbackInitiator.playTracksShuffled(likedTracks.subscribeOn(scheduler), playSessionSource);
    }

    @VisibleForTesting
    boolean shouldCreateOfflinePlayQueue() {
        return featureOperations.isOfflineContentEnabled() && !connectionHelper.isNetworkConnected();
    }

}
