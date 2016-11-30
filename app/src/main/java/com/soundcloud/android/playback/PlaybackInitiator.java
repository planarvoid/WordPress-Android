package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlayQueue.fromTrackUrnList;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

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

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns,
                                                 int position,
                                                 PlaySessionSource playSessionSource) {
        return playTracks(trackUrns, trackUrns.get(position), position, playSessionSource);
    }

    public Observable<PlaybackResult> playTracks(List<Urn> trackUrns, Urn trackUrn, int position,
                                                 PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = Observable.from(trackUrns)
                                                          .toList()
                                                          .flatMap(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, trackUrn, position, playSessionSource);
    }

    public Observable<PlaybackResult> playTracks(Observable<List<Urn>> allTracks, Urn initialTrack, int position,
                                                 PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = allTracks.flatMap(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource);
    }

    public Observable<PlaybackResult> playPosts(Observable<List<PropertySet>> playables, Urn initialTrack, int position,
                                                PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = playables.flatMap(playablesToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource);
    }

    @Deprecated
    // Please, use playTrackWithRecommendations instead.
    public Observable<PlaybackResult> playTrackWithRecommendationsLegacy(Urn track,
                                                                         PlaySessionSource playSessionSource) {
        // TODO : move to the alternative solution when playing the tracking story DROID-1028
        final Observable<PlayQueue> playQueue = Observable.just(track)
                                                          .toList()
                                                          .flatMap(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, track, 0, playSessionSource);
    }

    public Observable<PlaybackResult> playStation(Urn stationUrn,
                                                  List<StationTrack> stationTracks,
                                                  final PlaySessionSource playSessionSource,
                                                  final Urn clickedTrack, final int playQueuePosition) {

        if (isCurrentPlayQueueOrRecommendationState(clickedTrack, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        }

        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, stationTracks, playSessionSource);

        return playNewQueue(playQueue, playQueue.getUrn(playQueuePosition), playQueuePosition, playSessionSource);
    }

    public Observable<PlaybackResult> playTracksShuffled(Observable<List<Urn>> trackUrnsObservable,
                                                         final PlaySessionSource playSessionSource) {

        return trackUrnsObservable
                .filter(RxUtils.IS_NOT_EMPTY_LIST)
                .flatMap(toShuffledPlayQueue(playSessionSource))
                .flatMap(toPlaybackResult(0, playSessionSource))
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Func1<List<Urn>, Observable<PlayQueue>> toShuffledPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, Observable<PlayQueue>>() {
            @Override
            public Observable<PlayQueue> call(final List<Urn> urns) {
                return policyOperations.blockedStatuses(urns)
                                       .flatMap(createShuffledPlayQueue(urns, playSessionSource));
            }
        };
    }

    @NonNull
    private Func1<Map<Urn, Boolean>, Observable<PlayQueue>> createShuffledPlayQueue(
            final List<Urn> urns, final PlaySessionSource playSessionSource) {
        return new Func1<Map<Urn, Boolean>, Observable<PlayQueue>>() {
            @Override
            public Observable<PlayQueue> call(Map<Urn, Boolean> blockedTracksMap) {
                PlayQueue playQueue = PlayQueue.fromTrackUrnList(urns, playSessionSource, blockedTracksMap);
                PlayQueue shuffledPlayQueue = ShuffledPlayQueue.from(playQueue, 0, playQueue.size());
                return Observable.just(shuffledPlayQueue);
            }
        };
    }

    private Observable<PlaybackResult> playTracksList(Observable<PlayQueue> playQueue,
                                                      Urn initialTrack,
                                                      final int startPosition,
                                                      final PlaySessionSource playSessionSource) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            playSessionController.playCurrent();
            return Observable.just(PlaybackResult.success());
        } else {
            return playQueue
                    .doOnNext(addExplicitContentFromCurrentPlayQueue(startPosition))
                    .flatMap(toPlaybackResult(initialTrack, startPosition, playSessionSource))
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    private Action1<PlayQueue> addExplicitContentFromCurrentPlayQueue(final int startPosition) {
        return new Action1<PlayQueue>() {
            @Override
            public void call(PlayQueue playQueueItems) {
                List<PlayQueueItem> explicitQueueItems = playQueueManager.getUpcomingExplicitQueueItems();
                if (playQueueItems.size() <= startPosition) {
                    playQueueItems.insertAllItems(startPosition, explicitQueueItems);
                } else {
                    playQueueItems.insertAllItems(startPosition +1, explicitQueueItems);
                }
            }
        };
    }

    private Func1<List<PropertySet>, Observable<PlayQueue>> playablesToPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<PropertySet>, Observable<PlayQueue>>() {
            @Override
            public Observable<PlayQueue> call(final List<PropertySet> propertySets) {
                if (propertySets.isEmpty()) {
                    return Observable.just(PlayQueue.empty());
                } else {
                    return policyOperations.blockedStatuses(PropertySets.extractUrns(propertySets))
                                           .flatMap(new Func1<Map<Urn, Boolean>, Observable<PlayQueue>>() {
                                               @Override
                                               public Observable<PlayQueue> call(Map<Urn, Boolean> blockedTracksMap) {
                                                   return Observable.just(PlayQueue.fromPlayableList(propertySets,
                                                                                                     playSessionSource,
                                                                                                     blockedTracksMap));
                                               }
                                           });
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

    private Observable<PlaybackResult> playNewQueue(PlayQueue playQueue,
                                                    Urn initialTrack,
                                                    int startPosition,
                                                    PlaySessionSource playSessionSource) {
        if (playQueue.isEmpty()) {
            return Observable.just(PlaybackResult.error(MISSING_PLAYABLE_TRACKS));
        } else {
            return playSessionController.playNewQueue(playQueue, initialTrack, startPosition, playSessionSource);
        }
    }

    public Observable<PlaybackResult> startPlayback(Urn trackUrn, Screen screen) {
        playQueueManager.clearAll();

        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        return playTracksList(Observable.just(trackUrn).toList()
                                        .flatMap(urnsToPlayQueue(playSessionSource)), trackUrn, 0, playSessionSource);
    }

    @NonNull
    private Func1<List<Urn>, Observable<PlayQueue>> urnsToPlayQueue(final PlaySessionSource playSessionSource) {
        return new Func1<List<Urn>, Observable<PlayQueue>>() {
            @Override
            public Observable<PlayQueue> call(final List<Urn> urns) {
                if (urns.isEmpty()) {
                    return Observable.just(PlayQueue.empty());
                } else {
                    return policyOperations.blockedStatuses(urns)
                                           .flatMap(urnsToPlayQueueWithBlockedStati(urns, playSessionSource));
                }
            }
        };
    }

    @NonNull
    private Func1<Map<Urn, Boolean>, Observable<PlayQueue>> urnsToPlayQueueWithBlockedStati(final List<Urn> urns,
                                                                                            final PlaySessionSource playSessionSource) {
        return new Func1<Map<Urn, Boolean>, Observable<PlayQueue>>() {
            @Override
            public Observable<PlayQueue> call(Map<Urn, Boolean> blockedTracks) {
                return Observable.just(fromTrackUrnList(urns, playSessionSource, blockedTracks));
            }
        };
    }

    public Observable<PlaybackResult> startPlaybackWithRecommendations(Urn urn,
                                                                       Screen screen,
                                                                       SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracksList(Observable.just(urn).toList()
                                        .flatMap(urnsToPlayQueue(playSessionSource)), urn, 0, playSessionSource);
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
