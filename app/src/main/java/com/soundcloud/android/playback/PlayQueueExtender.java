package com.soundcloud.android.playback;

import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.android.stations.StationsOperations;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlayQueueExtender {

    @VisibleForTesting
    static final int RECOMMENDED_LOAD_TOLERANCE = 5;

    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final StationsOperations stationsOperations;
    private final CastConnectionHelper castConnectionHelper;

    private Disposable loadRecommendedDisposable = RxUtils.invalidDisposable();
    private boolean isLoadingRecommendations;

    @Inject
    PlayQueueExtender(PlayQueueManager playQueueManager,
                      PlayQueueOperations playQueueOperations,
                      StationsOperations stationsOperations,
                      CastConnectionHelper castConnectionHelper) {
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.stationsOperations = stationsOperations;
        this.castConnectionHelper = castConnectionHelper;
    }

    void onPlayQueueEvent(PlayQueueEvent event) {
        if (event.isNewQueue()) {
            isLoadingRecommendations = false;
            loadRecommendedDisposable.dispose();
            extendPlayQueue(event.getCollectionUrn());
        } else if (event.isAutoPlayEnabled()) {
            loadRecommendations(event.getCollectionUrn());
        }
    }

    void loadRecommendations(Urn collectionUrn) {
        if (!isLoadingRecommendations && withinRecommendedFetchTolerance()) {
            extendPlayQueue(collectionUrn);
        }
    }

    private void extendPlayQueue(Urn collectionUrn) {
        if (!castConnectionHelper.isCasting() && !playQueueManager.isQueueEmpty()) {
            final PlayQueueItem lastPlayQueueItem = playQueueManager.getLastPlayQueueItem();

            if (!playQueueManager.getCollectionUrn().isStation() && lastPlayQueueItem.isTrack()) {
                loadRecommendedDisposable = playQueueOperations
                        .relatedTracksPlayQueue(lastPlayQueueItem.getUrn(),
                                                fromContinuousPlay(),
                                                playQueueManager.getCurrentPlaySessionSource())
                        .doOnSubscribe(__ -> isLoadingRecommendations = true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(() -> isLoadingRecommendations = false)
                        .subscribeWith(LambdaSingleObserver.onSuccess(playQueueManager::appendPlayQueueItems));
            } else if (collectionUrn.isStation()) {
                loadRecommendedDisposable = stationsOperations
                        .fetchUpcomingTracks(collectionUrn,
                                             playQueueManager.getQueueSize(),
                                             playQueueManager.getCurrentPlaySessionSource())
                        .doOnSubscribe(__ -> isLoadingRecommendations = true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(() -> isLoadingRecommendations = false)
                        .subscribeWith(LambdaSingleObserver.onSuccess(playQueueManager::appendPlayQueueItems));
            }
        }
    }

    private boolean withinRecommendedFetchTolerance() {
        return playQueueManager.getPlayableQueueItemsRemaining() <= RECOMMENDED_LOAD_TOLERANCE;
    }

    // Hacky, but the similar sounds service needs to know if it is allowed to not fulfill this request. This should
    // only be allowed if we are not in explore, or serving a deeplink. This should be removed after rollout and we
    // have determined the service can handle the load we give it...
    private boolean fromContinuousPlay() {
        final PlaySessionSource currentPlaySessionSource = playQueueManager.getCurrentPlaySessionSource();
        return !(currentPlaySessionSource.originatedFromDeeplink() ||
                currentPlaySessionSource.originatedInSearchSuggestions());
    }
}
