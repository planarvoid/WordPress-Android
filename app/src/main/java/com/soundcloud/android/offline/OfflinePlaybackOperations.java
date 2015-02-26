package com.soundcloud.android.offline;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.likes.TrackLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.LoadOfflineTrackUrnsCommand;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackUtils;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.tracks.TrackProperty;
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
    private final FeatureOperations featureOperations;
    private final PlaybackOperations playbackOperations;
    private final NetworkConnectionHelper connectionHelper;
    private final PlaybackToastHelper playbackToastHelper;
    private final LoadOfflineTrackUrnsCommand offlineLikesUrnsCommand;
    private final Scheduler scheduler;


    @Inject
    public OfflinePlaybackOperations(FeatureOperations featureOperations, NetworkConnectionHelper connectionHelper,
                                     PlaybackOperations playbackOperations, TrackLikeOperations likeOperations,
                                     PlaybackToastHelper playbackToastHelper,
                                     LoadOfflineTrackUrnsCommand offlineLikesUrnsCommand,
                                     @Named("Storage") Scheduler scheduler) {
        this.featureOperations = featureOperations;
        this.connectionHelper = connectionHelper;
        this.playbackOperations = playbackOperations;
        this.likeOperations = likeOperations;
        this.playbackToastHelper = playbackToastHelper;
        this.offlineLikesUrnsCommand = offlineLikesUrnsCommand;
        this.scheduler = scheduler;
    }

    public boolean shouldCreateOfflinePlayQueue() {
        return featureOperations.isOfflineContentEnabled() && !connectionHelper.isNetworkConnected();
    }

    public boolean shouldPlayOffline(PropertySet track) {
        return featureOperations.isOfflineContentEnabled()
                && track.getOrElseNull(TrackProperty.OFFLINE_DOWNLOADED_AT) != null
                && track.getOrElseNull(TrackProperty.OFFLINE_REMOVED_AT) == null;
    }

    public Observable<List<Urn>> playLikes(final Urn trackUrn, final int position, final PlaySessionSource playSessionSource) {
        if (shouldCreateOfflinePlayQueue()) {
            return offlineLikesUrnsCommand
                    .toObservable()
                    .subscribeOn(scheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(playIfAvailableOffline(trackUrn, position, playSessionSource));
        }
        return playbackOperations.playTracks(likeOperations.likedTrackUrns(), trackUrn, position, playSessionSource);
    }

    private Func1<List<Urn>, Observable<List<Urn>>> playIfAvailableOffline(final Urn trackUrn,
                                                                           final int position,
                                                                           final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, Observable<List<Urn>>>() {
            @Override
            public Observable<List<Urn>> call(List<Urn> urns) {
                int corrected = PlaybackUtils.correctInitialPosition(urns, position, trackUrn);
                if (corrected < 0) {
                    playbackToastHelper.showTrackUnavailableOfflineToast();
                    return Observable.empty();
                }
                return playbackOperations.playTracks(urns, trackUrn, corrected, playSessionSource);
            }
        };
    }

    public Observable<List<Urn>> playTracksShuffled(PlaySessionSource playSessionSource) {
        final Observable<List<Urn>> likedTracks = shouldCreateOfflinePlayQueue()
                ? offlineLikesUrnsCommand.toObservable()
                : likeOperations.likedTrackUrns();

        return playbackOperations.playTracksShuffled(likedTracks, playSessionSource);
    }
}
