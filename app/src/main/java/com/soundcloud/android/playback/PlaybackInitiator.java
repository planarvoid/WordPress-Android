package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.Stations;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.net.Uri;

import javax.inject.Inject;
import java.util.List;

public class PlaybackInitiator {

    public static final boolean WITH_RELATED = true;
    public static final boolean WITHOUT_RELATED = false;

    private final TrackStorage trackStorage;
    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final PlaySessionController playSessionController;

    @Inject
    public PlaybackInitiator(TrackStorage trackStorage,
                             PlayQueueManager playQueueManager,
                             PlayQueueOperations playQueueOperations, PlaySessionController playSessionController) {
        this.trackStorage = trackStorage;
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
        return playTracksList(playQueue, trackUrn, position, playSessionSource, WITHOUT_RELATED);
    }

    public Observable<PlaybackResult> playTracks(Observable<List<Urn>> allTracks, Urn initialTrack, int position,
                                            PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = allTracks.map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource, WITHOUT_RELATED);
    }

    public Observable<PlaybackResult> playPosts(Observable<List<PropertySet>> allTracks, Urn initialTrack, int position,
                                                 PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = allTracks.map(tracksToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, position, playSessionSource, WITHOUT_RELATED);
    }

    @Deprecated
    public Observable<PlaybackResult> playTracksFromUri(Uri uri, int startPosition, Urn initialTrack,
                                                        PlaySessionSource playSessionSource) {
        final Observable<PlayQueue> playQueue = trackStorage.getTracksForUriAsync(uri).map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, initialTrack, startPosition, playSessionSource, WITHOUT_RELATED);
    }

    @Deprecated
    // Please, use playTrackWithRecommendations instead.
    public Observable<PlaybackResult> playTrackWithRecommendationsLegacy(Urn track, PlaySessionSource playSessionSource) {
        // TODO : move to the alternative solution when playing the tracking story DROID-1028
        final Observable<PlayQueue> playQueue = Observable.just(track).toList().map(urnsToPlayQueue(playSessionSource));
        return playTracksList(playQueue, track, 0, playSessionSource, WITH_RELATED);
    }

    public Observable<PlaybackResult> playTrackWithRecommendations(final Urn seedTrack, final PlaySessionSource playSessionSource, final int startPosition) {
        return playQueueOperations
                .relatedTracksPlayQueueWithSeedTrack(seedTrack)
                .flatMap(toPlaybackResult(startPosition, playSessionSource, false));
    }

    public Observable<PlaybackResult> playStation(Urn stationUrn, List<Urn> tracks, final PlaySessionSource playSessionSource, final int previousPosition) {
        // TODO : once we land the playback operations refactoring #3876
        // move this code to a proper stations builder.
        final int nextPosition;
        final Urn previousTrackUrn;
        if (previousPosition == Stations.NEVER_PLAYED)  {
            previousTrackUrn = Urn.NOT_SET;
            nextPosition = 0;
        } else {
            previousTrackUrn = tracks.get(previousPosition);
            nextPosition = (previousPosition + 1) % tracks.size();
        }

        if (isCurrentPlayQueueOrRecommendationState(previousTrackUrn, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        }

        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, tracks);
        return playNewQueue(playQueue, playQueue.getUrn(nextPosition), nextPosition, false, playSessionSource);
    }

    public Observable<PlaybackResult> playTracksShuffled(Observable<List<Urn>> trackUrnsObservable, final PlaySessionSource playSessionSource) {
        return trackUrnsObservable
                .map(toShuffledPlayQueue(playSessionSource))
                .flatMap(toPlaybackResult(Urn.NOT_SET, 0, playSessionSource, WITHOUT_RELATED))
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
                                                      final PlaySessionSource playSessionSource,
                                                      boolean loadRelated) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            return Observable.just(PlaybackResult.success());
        } else {
            return playQueue
                    .flatMap(toPlaybackResult(initialTrack, startPosition, playSessionSource, loadRelated))
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
                                                                          final PlaySessionSource playSessionSource,
                                                                          final boolean loadRelated) {
        return new Func1<PlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(PlayQueue playQueue) {
                return playNewQueue(playQueue, playQueue.getUrn(startPosition), startPosition, loadRelated, playSessionSource);
            }
        };
    }

    private Func1<PlayQueue, Observable<PlaybackResult>> toPlaybackResult(final Urn initialTrack,
                                                                          final int startPosition,
                                                                          final PlaySessionSource playSessionSource,
                                                                          final boolean loadRelated) {
        return new Func1<PlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(PlayQueue playQueue) {
                return playNewQueue(playQueue, initialTrack, startPosition, loadRelated, playSessionSource);
            }
        };
    }

    private Observable<PlaybackResult> playNewQueue(PlayQueue playQueue, Urn initialTrack, int startPosition, boolean loadRelated, PlaySessionSource playSessionSource) {
        if (playQueue.isEmpty()) {
            return Observable.just(PlaybackResult.error(MISSING_PLAYABLE_TRACKS));
        } else {
            return playSessionController.playNewQueue(playQueue, initialTrack, startPosition, loadRelated, playSessionSource);
        }
    }

    public Observable<PlaybackResult> startPlayback(PublicApiTrack track, Screen screen, boolean withRecommendations) {
        playQueueManager.clearAll();

        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        return playTracksList(Observable.just(track.getUrn()).toList()
                .map(urnsToPlayQueue(playSessionSource)), track.getUrn(), 0, playSessionSource, withRecommendations);
    }

    public Observable<PlaybackResult> startPlaybackWithRecommendations(Urn urn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracksList(Observable.just(urn).toList()
                .map(urnsToPlayQueue(playSessionSource)), urn, 0, playSessionSource, WITH_RELATED);
    }

    private boolean shouldChangePlayQueue(Urn trackUrn, PlaySessionSource playSessionSource) {
        return !isCurrentTrack(trackUrn) || !isCurrentScreenSource(playSessionSource) ||
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
