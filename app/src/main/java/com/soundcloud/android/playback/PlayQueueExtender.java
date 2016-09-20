package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlayQueueManager.RepeatMode.REPEAT_ALL;
import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;

@Singleton
public class PlayQueueExtender {

    @VisibleForTesting
    static final int RECOMMENDED_LOAD_TOLERANCE = 5;

    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final StationsOperations stationsOperations;
    private final SharedPreferences sharedPreferences;
    private final EventBus eventBus;

    private Subscription loadRecommendedSubscription = RxUtils.invalidSubscription();

    @Inject
    public PlayQueueExtender(PlayQueueManager playQueueManager,
                             PlayQueueOperations playQueueOperations,
                             StationsOperations stationsOperations,
                             SharedPreferences sharedPreferences,
                             EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.stationsOperations = stationsOperations;
        this.sharedPreferences = sharedPreferences;
        this.eventBus = eventBus;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new PlayQueueTrackSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.isNewQueue()) {
                loadRecommendedSubscription.unsubscribe();
            }
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            if (withinRecommendedFetchTolerance() && isNotAlreadyLoadingRecommendations()) {
                final PlayQueueItem lastPlayQueueItem = playQueueManager.getLastPlayQueueItem();
                if (currentQueueAllowsRecommendations() && lastPlayQueueItem.isTrack()) {
                    loadRecommendedSubscription = playQueueOperations
                            .relatedTracksPlayQueue(lastPlayQueueItem.getUrn(), fromContinuousPlay(), playQueueManager.getCurrentPlaySessionSource())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new UpcomingTracksSubscriber());
                } else if (event.getCollectionUrn().isStation()) {
                    loadRecommendedSubscription = stationsOperations
                            .fetchUpcomingTracks(event.getCollectionUrn(), playQueueManager.getQueueSize(), playQueueManager.getCurrentPlaySessionSource())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new UpcomingTracksSubscriber());
                }
            }
        }
    }

    private boolean currentQueueAllowsRecommendations() {
        final PlaySessionSource currentPlaySessionSource = playQueueManager.getCurrentPlaySessionSource();
        final boolean isStation = playQueueManager.getCollectionUrn().isStation();
        final boolean isRepeatAll = REPEAT_ALL.equals(playQueueManager.getRepeatMode());
        final boolean isAutoplay = sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true);
        final boolean isFromExplore = currentPlaySessionSource.originatedInExplore();
        final boolean isDeeplink = Screen.DEEPLINK.get().equals(currentPlaySessionSource.getOriginScreen());

        return !isStation && !isRepeatAll && (isAutoplay || isFromExplore || isDeeplink);
    }

    private boolean withinRecommendedFetchTolerance() {
        return !playQueueManager.isQueueEmpty() &&
                playQueueManager.getPlayableQueueItemsRemaining() <= RECOMMENDED_LOAD_TOLERANCE;
    }

    private boolean isNotAlreadyLoadingRecommendations() {
        return loadRecommendedSubscription.isUnsubscribed();
    }

    // Hacky, but the similar sounds service needs to know if it is allowed to not fulfill this request. This should
    // only be allowed if we are not in explore, or serving a deeplink. This should be removed after rollout and we
    // have determined the service can handle the load we give it...
    private boolean fromContinuousPlay() {
        final PlaySessionSource currentPlaySessionSource = playQueueManager.getCurrentPlaySessionSource();
        return !(currentPlaySessionSource.originatedInExplore() ||
                currentPlaySessionSource.originatedFromDeeplink() ||
                currentPlaySessionSource.originatedInSearchSuggestions());
    }

    private class UpcomingTracksSubscriber extends DefaultSubscriber<PlayQueue> {
        @Override
        public void onNext(PlayQueue playQueue) {
            checkArgument(!playQueueManager.isQueueEmpty(), "Should not append to empty queue");
            playQueueManager.appendPlayQueueItems(playQueue);
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof IllegalArgumentException) {
                // we should not need this, as we should never get this far with an empty queue.
                // Just being defensive while we investigate
                // https://github.com/soundcloud/SoundCloud-Android/issues/3938

                final HashMap<String, String> valuePairs = new HashMap<>(2);
                valuePairs.put("Queue Size", String.valueOf(playQueueManager.getQueueSize()));
                valuePairs.put("PlaySessionSource", playQueueManager.getCurrentPlaySessionSource().toString());
                ErrorUtils.handleSilentException(e, valuePairs);
            } else {
                super.onError(e);
            }
        }
    }

}
