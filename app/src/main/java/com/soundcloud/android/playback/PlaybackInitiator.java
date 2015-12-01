package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.Stations;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class PlaybackInitiator {

    public static final boolean WITH_RELATED = true;
    public static final boolean WITHOUT_RELATED = false;

    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final PlaySessionController playSessionController;

    @Inject
    public PlaybackInitiator(PlayQueueManager playQueueManager,
                             PlayQueueOperations playQueueOperations,
                             PlaySessionController playSessionController) {
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.playSessionController = playSessionController;
    }

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns, int position, PlaySessionSource playSessionSource) {
        return playTracks(trackUrns, trackUrns.get(position), position, playSessionSource);
    }

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns, Urn trackUrn, int position,
                                            PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = Observable.from(trackUrns).toList().map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, trackUrn, position, playSessionSource);
    }

    public Observable<PlaybackResult> playTracks(Observable<List<Urn>> allTracks, Urn initialTrack, int position,
                                            PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = allTracks.map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource);
    }

    public Observable<PlaybackResult> playPosts(Observable<List<PropertySet>> allTracks, Urn initialTrack, int position,
                                                 PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = allTracks.map(tracksToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource);
    }

    @Deprecated
    // Please, use playTrackWithRecommendations instead.
    public Observable<PlaybackResult> playTrackWithRecommendationsLegacy(Urn track, PlaySessionSource playSessionSource) {
        // TODO : move to the alternative solution when playing the tracking story DROID-1028
        final Observable<PlayQueue> playQueue = Observable.just(track).toList().map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, track, 0, playSessionSource);
    }

    public Observable<PlaybackResult> playTrackWithRecommendations(final Urn seedTrack, final PlaySessionSource playSessionSource, final int startPosition) {
        return playQueueOperations
                .relatedTracksPlayQueueWithSeedTrack(seedTrack)
                .flatMap(toPlaybackResult(startPosition, playSessionSource));
    }

    public Observable<PlaybackResult> playStation(Urn stationUrn, List<StationTrack> stationTracks, final PlaySessionSource playSessionSource, final int previousPosition) {
        // TODO : once we land the playback operations refactoring #3876
        // move this code to a proper stations builder.
        final int nextPosition;
        final Urn previousTrackUrn;
        if (previousPosition == Stations.NEVER_PLAYED)  {
            previousTrackUrn = Urn.NOT_SET;
            nextPosition = 0;
        } else {
            previousTrackUrn = stationTracks.get(previousPosition).getTrackUrn();
            nextPosition = (previousPosition + 1) % stationTracks.size();
        }

        if (isCurrentPlayQueueOrRecommendationState(previousTrackUrn, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        }

        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, stationTracks);
        return playNewQueue(playQueue, playQueue.getUrn(nextPosition), nextPosition, playSessionSource);
    }

    public Observable<PlaybackResult> playTracksShuffled(Observable<List<Urn>> trackUrnsObservable, final PlaySessionSource playSessionSource) {
        return trackUrnsObservable
                .map(toShuffledPlayQueue(playSessionSource))
                .flatMap(toPlaybackResult(Urn.NOT_SET, 0, playSessionSource))
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Func1<List<Urn>, PlayQueue> toShuffledPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, PlayQueue>() {
            @Override
            public PlayQueue call(List<Urn> urns) {
                return PlayQueue.shuffled(urns, playSessionSource);
            }
        };
    }

    private Observable<PlaybackResult> playTracksList(Observable<PlayQueue> playQueue,
                                                      Urn initialTrack,
                                                      int startPosition,
                                                      final PlaySessionSource playSessionSource) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        } else {
            return playQueue
                    .flatMap(toPlaybackResult(initialTrack, startPosition, playSessionSource))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    private Func1<List<Urn>, PlayQueue> urnsToPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, PlayQueue>() {
            @Override
            public PlayQueue call(List<Urn> urns) {
                if (urns.isEmpty()) {
                    return PlayQueue.empty();
                } else {
                    return PlayQueue.fromTrackUrnList(urns, playSessionSource);
                }
            }
        };
    }

    private Func1<List<PropertySet>, PlayQueue> tracksToPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<PropertySet>, PlayQueue>() {
            @Override
            public PlayQueue call(List<PropertySet> urns) {
                if (urns.isEmpty()) {
                    return PlayQueue.empty();
                } else {
                    return PlayQueue.fromTrackList(urns, playSessionSource);
                }
            }
        };
    }

    private Func1<PlayQueue, Observable<PlaybackResult>> toPlaybackResult(final int startPosition,
                                                                          final PlaySessionSource playSessionSource) {
        return new Func1<PlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(PlayQueue playQueue) {
                return playNewQueue(playQueue, playQueue.getUrn(startPosition), startPosition, playSessionSource);
            }
        };
    }

    private Func1<PlayQueue, Observable<PlaybackResult>> toPlaybackResult(final Urn initialTrack,
                                                                          final int startPosition,
                                                                          final PlaySessionSource playSessionSource) {
        return new Func1<PlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(PlayQueue playQueue) {
                return playNewQueue(playQueue, initialTrack, startPosition, playSessionSource);
            }
        };
    }

    private Observable<PlaybackResult> playNewQueue(PlayQueue playQueue, Urn initialTrack, int startPosition, PlaySessionSource playSessionSource) {
        if (playQueue.isEmpty()) {
            return Observable.just(PlaybackResult.error(MISSING_PLAYABLE_TRACKS));
        } else {
            return playSessionController.playNewQueue(playQueue, initialTrack, startPosition, playSessionSource);
        }
    }

    public Observable<PlaybackResult> startPlayback(PublicApiTrack track, Screen screen) {
        playQueueManager.clearAll();

        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        return playTracksList(Observable.just(track.getUrn()).toList()
                .map(urnsToPlayQueue(playSessionSource)), track.getUrn(), 0, playSessionSource);
    }

    public Observable<PlaybackResult> startPlaybackWithRecommendations(Urn urn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracksList(Observable.just(urn).toList()
                .map(urnsToPlayQueue(playSessionSource)), urn, 0, playSessionSource);
    }

    private boolean shouldChangePlayQueue(Urn trackUrn, PlaySessionSource playSessionSource) {
        return playQueueManager.isQueueEmpty() || !isCurrentTrack(trackUrn) || !isCurrentScreenSource(playSessionSource) ||
                !playQueueManager.isCurrentCollection(playSessionSource.getCollectionUrn());
    }

    private boolean isCurrentScreenSource(PlaySessionSource playSessionSource) {
        return playSessionSource.getOriginScreen().equals(playQueueManager.getScreenTag());
    }

    private boolean isCurrentTrack(Urn trackUrn) {
        return playQueueManager.isCurrentTrack(trackUrn);
    }

    private boolean isCurrentPlayQueueOrRecommendationState(Urn trackUrn, PlaySessionSource playSessionSource) {
        return isCurrentTrack(trackUrn)
                && isCurrentScreenSource(playSessionSource)
                && playQueueManager.isCurrentCollectionOrRecommendation(playSessionSource.getCollectionUrn());
    }

}
