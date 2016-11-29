package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.java.collections.Lists;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class PlaybackInitiator {

    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final PolicyOperations policyOperations;

    @Inject
    public PlaybackInitiator(PlayQueueManager playQueueManager,
                             PlaySessionController playSessionController,
                             PolicyOperations policyOperations) {
        this.playQueueManager = playQueueManager;
        this.playSessionController = playSessionController;
        this.policyOperations = policyOperations;
    }

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns, int position, PlaySessionSource playSessionSource) {
        return playTracks(trackUrns, trackUrns.get(position), position, playSessionSource);
    }

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns, Urn trackUrn, int position, PlaySessionSource playSessionSource) {
        return playTracks(Observable.from(trackUrns).toList(), trackUrn, position, playSessionSource);
    }

    public Observable<PlaybackResult> startPlayback(Urn trackUrn, Screen screen) {
        playQueueManager.clearAll();
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        return playTracks(Observable.just(trackUrn).toList(), trackUrn, 0, playSessionSource);
    }

    public Observable<PlaybackResult> startPlaybackWithRecommendations(Urn urn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracks(Observable.just(urn).toList(), urn, 0, playSessionSource);
    }

    @Deprecated
    // Please, use playTrackWithRecommendations instead.
    public Observable<PlaybackResult> playTrackWithRecommendationsLegacy(Urn track, PlaySessionSource playSessionSource) {
        // TODO : move to the alternative solution when playing the tracking story DROID-1028
        return playTracks(Observable.just(track).toList(), track, 0, playSessionSource);
    }

    public Observable<PlaybackResult> playPosts(Observable<List<PlayableWithReposter>> playables, Urn initialTrack, int position, PlaySessionSource playSessionSource) {
        Observable<List<Urn>> urns = playables.map(playablesItem -> Lists.transform(playablesItem, PlayableWithReposter::getUrn));
        return playTracks(urns, initialTrack, position, playSessionSource);
    }


    public Observable<PlaybackResult> playTracks(Observable<List<Urn>> allTracks, Urn initialTrack, int position, PlaySessionSource playSessionSource) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            playSessionController.playCurrent();
            return Observable.just(PlaybackResult.success());
        } else {
            return allTracks.flatMap(policyOperations.blockedStatuses())
                            .zipWith(allTracks, (blockedUrns, urns) -> PlayQueue.fromTrackUrnList(urns, playSessionSource, blockedUrns))
                            .map(addExplicitContentFromCurrentPlayQueue(position, initialTrack))
                            .flatMap(playQueue -> playSessionController.playNewQueue(playQueue, initialTrack, position, playSessionSource))
                            .observeOn(AndroidSchedulers.mainThread());
        }
    }

    public Observable<PlaybackResult> playStation(Urn stationUrn,
                                                  List<StationTrack> stationTracks,
                                                  final PlaySessionSource playSessionSource,
                                                  final Urn clickedTrack, final int playQueuePosition) {

        if (isCurrentPlayQueueOrRecommendationState(clickedTrack, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        }

        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, stationTracks, playSessionSource);

        return playSessionController.playNewQueue(playQueue, playQueue.getUrn(playQueuePosition), playQueuePosition, playSessionSource);
    }

    public Observable<PlaybackResult> playTracksShuffled(Observable<List<Urn>> allTracks, final PlaySessionSource playSessionSource) {
        return allTracks.flatMap(policyOperations.blockedStatuses())
                        .zipWith(allTracks, (blockedUrns, urns) -> {
                            PlayQueue playQueue = PlayQueue.fromTrackUrnList(urns, playSessionSource, blockedUrns);
                            return ShuffledPlayQueue.from(playQueue, 0, playQueue.size());
                        })
                        .flatMap(playQueue -> playSessionController.playNewQueue(playQueue, playQueue.getUrn(0), 0, playSessionSource))
                        .observeOn(AndroidSchedulers.mainThread());
    }

    private Func1<PlayQueue, PlayQueue> addExplicitContentFromCurrentPlayQueue(final int startPosition, Urn initialTrack) {
        return playQueueItems -> {
            final List<PlayQueueItem> explicitQueueItems = playQueueManager.getUpcomingExplicitQueueItems();
            final int updatedInitialPosition = PlaybackUtils.correctInitialPosition(playQueueItems, startPosition, initialTrack);
            if (playQueueItems.size() <= updatedInitialPosition) {
                playQueueItems.insertAllItems(updatedInitialPosition, explicitQueueItems);
            } else {
                playQueueItems.insertAllItems(updatedInitialPosition + 1, explicitQueueItems);
            }
            return playQueueItems;
        };
    }

    private boolean shouldChangePlayQueue(Urn trackUrn, PlaySessionSource playSessionSource) {
        return playQueueManager.isQueueEmpty() || !isCurrentTrack(trackUrn) || !isCurrentScreenSource(playSessionSource) || !playQueueManager.isCurrentCollection(playSessionSource.getCollectionUrn());
    }

    private boolean isCurrentScreenSource(PlaySessionSource playSessionSource) {
        return playSessionSource.getOriginScreen().equals(playQueueManager.getScreenTag());
    }

    private boolean isCurrentTrack(Urn trackUrn) {
        return playQueueManager.isCurrentTrack(trackUrn);
    }

    private boolean isCurrentPlayQueueOrRecommendationState(Urn trackUrn, PlaySessionSource playSessionSource) {
        return isCurrentTrack(trackUrn) && isCurrentScreenSource(playSessionSource) && playQueueManager.isCurrentCollectionOrRecommendation(playSessionSource.getCollectionUrn());
    }

}
