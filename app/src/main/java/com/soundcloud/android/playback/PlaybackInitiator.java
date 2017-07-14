package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class PlaybackInitiator {

    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final PolicyOperations policyOperations;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public PlaybackInitiator(PlayQueueManager playQueueManager,
                             PlaySessionController playSessionController,
                             PolicyOperations policyOperations,
                             PerformanceMetricsEngine performanceMetricsEngine) {
        this.playQueueManager = playQueueManager;
        this.playSessionController = playSessionController;
        this.policyOperations = policyOperations;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public Single<PlaybackResult> startPlayback(Urn trackUrn, Screen screen) {
        playQueueManager.clearAll();
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        return playTracks(Single.just(Collections.singletonList(trackUrn)), trackUrn, 0, playSessionSource);
    }

    public Single<PlaybackResult> startPlaybackWithRecommendations(Urn urn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        return playTracks(Single.just(Collections.singletonList(urn)), urn, 0, playSessionSource);
    }

    @Deprecated
    // Please, use playTrackWithRecommendations instead.
    public Single<PlaybackResult> playTrackWithRecommendationsLegacy(Urn track, PlaySessionSource playSessionSource) {
        // TODO : move to the alternative solution when playing the tracking story DROID-1028
        return playTracks(Single.just(Collections.singletonList(track)), track, 0, playSessionSource);
    }

    public Single<PlaybackResult> playPosts(Single<List<PlayableWithReposter>> playables, Urn initialTrack, int position, PlaySessionSource playSessionSource) {
        Single<List<Urn>> urns = playables.map(playablesItem -> Lists.transform(playablesItem, PlayableWithReposter::getUrn));
        return playTracks(urns, initialTrack, position, playSessionSource);
    }

    public Single<PlaybackResult> playPosts(List<Urn> playableUrns, Urn initialTrack, int position, PlaySessionSource playSessionSource) {
        return playTracks(Single.just(playableUrns), initialTrack, position, playSessionSource);
    }

    public Single<PlaybackResult> playTracks(List<Urn> trackUrns, int position, PlaySessionSource playSessionSource) {
        if (trackUrns.isEmpty()) {
            return Single.just(PlaybackResult.error(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS));
        } else {
            return playTracks(Single.just(trackUrns), trackUrns.get(position), position, playSessionSource);
        }
    }

    public Single<PlaybackResult> playTracks(Single<List<Urn>> allTracks, Urn initialTrack, int initialPosition, PlaySessionSource playSessionSource) {
        if (!shouldChangePlayQueue(initialTrack, playSessionSource)) {
            playSessionController.playCurrent();
            return Single.just(PlaybackResult.success());
        } else {
            return allTracks.doOnSubscribe(__ -> startMeasuringPlaybackStarted(playSessionSource))
                            .flatMap(policyOperations.blockedStatuses())
                            .zipWith(allTracks, (blockedUrns, urns) -> PlayQueue.fromTrackUrnList(urns, playSessionSource, blockedUrns))
                            .map(addExplicitContentFromCurrentPlayQueue(initialPosition, initialTrack, playSessionSource))
                            .flatMap(playNewQueue(initialTrack, initialPosition, playSessionSource))
                            .observeOn(AndroidSchedulers.mainThread());
        }
    }

    @NonNull
    private Function<PlayQueue, Single<? extends PlaybackResult>> playNewQueue(final Urn initialTrack, final int initialPosition, final PlaySessionSource playSessionSource) {
        return playQueue -> {

            int positionOfFirstPlayable = PlaybackUtils.correctStartPosition(playQueue, initialPosition, initialTrack, playSessionSource);
            Urn urnOfFirstPlayable = initialTrack;

            List<PlayQueueItem> items = playQueue.items();
            for (int i = positionOfFirstPlayable; i < items.size(); i++) {
                PlayQueueItem playQueueItem = items.get(i);
                if (playQueueItem.isTrack()) {
                    if (!((TrackQueueItem) playQueueItem).isBlocked()) {
                        positionOfFirstPlayable = i;
                        urnOfFirstPlayable = playQueueItem.getUrn();
                        break;
                    }
                }
            }

            return playSessionController.playNewQueue(playQueue, urnOfFirstPlayable, positionOfFirstPlayable, playSessionSource);
        };
    }

    private void startMeasuringPlaybackStarted(PlaySessionSource playSessionSource) {
        String screen = playSessionSource.getOriginScreen();
        startMeasuringWithScreen(MetricType.TIME_TO_EXPAND_PLAYER, screen);
        startMeasuringWithScreen(MetricType.TIME_TO_PLAY, screen);
    }

    private void startMeasuringWithScreen(MetricType metricType, String screen) {
        PerformanceMetric timeToExpand = PerformanceMetric.builder()
                                                          .metricType(metricType)
                                                          .metricParams(new MetricParams().putString(MetricKey.SCREEN, screen))
                                                          .build();
        performanceMetricsEngine.startMeasuring(timeToExpand);
    }

    public Single<PlaybackResult> playStation(Urn stationUrn,
                                              List<StationTrack> stationTracks,
                                              final PlaySessionSource playSessionSource,
                                              final Urn clickedTrack, final int playQueuePosition) {

        if (isCurrentPlayQueueOrRecommendationState(clickedTrack, playSessionSource)) {
            return Single.just(PlaybackResult.success());
        }

        final PlayQueue playQueue = PlayQueue.fromStation(stationUrn, stationTracks, playSessionSource);

        return playSessionController.playNewQueue(playQueue, playQueue.getUrn(playQueuePosition), playQueuePosition, playSessionSource)
                                    .doOnSubscribe(__ -> startMeasuringPlaybackStarted(playSessionSource));
    }

    public Single<PlaybackResult> playTracksShuffled(Single<List<Urn>> allTracks, final PlaySessionSource playSessionSource) {
        return allTracks.flatMap(policyOperations.blockedStatuses())
                        .zipWith(allTracks, (blockedUrns, urns) -> {
                            PlayQueue playQueue = PlayQueue.fromTrackUrnList(urns, playSessionSource, blockedUrns);
                            return ShuffledPlayQueue.from(playQueue, 0, playQueue.size());
                        })
                        .flatMap(playQueue -> playSessionController.playNewQueue(playQueue, playQueue.getUrn(0), 0, playSessionSource))
                        .observeOn(AndroidSchedulers.mainThread());
    }

    private Function<PlayQueue, PlayQueue> addExplicitContentFromCurrentPlayQueue(final int startPosition, Urn initialTrack, PlaySessionSource playSessionSource) {
        return playQueueItems -> {
            final List<PlayQueueItem> explicitQueueItems = playQueueManager.getUpcomingExplicitQueueItems();
            final int updatedInitialPosition = PlaybackUtils.correctStartPosition(playQueueItems, startPosition, initialTrack, playSessionSource);
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
        return isCurrentTrack(trackUrn) && isCurrentScreenSource(playSessionSource) && playQueueManager.isCurrentCollection(playSessionSource.getCollectionUrn());
    }

}
