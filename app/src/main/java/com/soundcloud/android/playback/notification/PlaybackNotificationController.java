package com.soundcloud.android.playback.notification;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackNotificationController extends DefaultActivityLightCycle<AppCompatActivity> {

    interface Strategy {
        void setTrack(PropertySet track);

        void clear(PlaybackService playbackService);

        void notifyPlaying(PlaybackService playbackService);

        void notifyIdleState(PlaybackService playbackService);
    }

    // Estimated time between onPause and onResume. This to avoid cancelling a notification
    // while switching from one activity to the user. Over DEFAULT_DELAY_MILLIS we estimate
    // the app is either foreground or background.
    private static final int DEFAULT_DELAY_MILLIS = 300;

    private final EventBus eventBus;
    private final Strategy backgroundStrategy;
    private final Strategy foregroundStrategy;
    private final Handler handler;
    private final int delayMillis;

    private Subscription subscriptions = RxUtils.invalidSubscription();
    private PlaybackService playbackService;
    private Strategy activeStrategy;

    @Inject
    public PlaybackNotificationController(EventBus eventBus,
                                          BackgroundPlaybackNotificationController backgroundStrategy,
                                          ForegroundPlaybackNotificationController foregroundStrategy) {

        this(eventBus, backgroundStrategy, foregroundStrategy, DEFAULT_DELAY_MILLIS);
    }

    public PlaybackNotificationController(EventBus eventBus,
                                          BackgroundPlaybackNotificationController backgroundStrategy,
                                          ForegroundPlaybackNotificationController foregroundStrategy,
                                          int delayMillis) {
        this.eventBus = eventBus;
        this.backgroundStrategy = backgroundStrategy;
        this.foregroundStrategy = foregroundStrategy;
        this.activeStrategy = backgroundStrategy;
        this.delayMillis = delayMillis;
        this.handler = new Handler();
    }

    public void subscribe(PlaybackService service) {
        if (!hasRunningPlaybackService()) {
            playbackService = service;
            startStrategy();
        }
    }

    public void unsubscribe() {
        subscriptions.unsubscribe();
        if (hasRunningPlaybackService()) {
            activeStrategy.clear(playbackService);
            playbackService = null;
        }
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        delayedSwitchStrategyTo(foregroundStrategy);
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        delayedSwitchStrategyTo(backgroundStrategy);
    }

    private void delayedSwitchStrategyTo(Strategy nextStrategy) {
        if (delayMillis > 0) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(createSwitchStrategyToRunnable(nextStrategy), delayMillis);
        } else {
            switchStrategyTo(nextStrategy);
        }
    }

    private Runnable createSwitchStrategyToRunnable(final Strategy nextStrategy) {
        return new Runnable() {
            @Override
            public void run() {
                switchStrategyTo(nextStrategy);
            }
        };
    }

    private void switchStrategyTo(Strategy nextStrategy) {
        final Strategy previousStrategy = activeStrategy;
        activeStrategy = nextStrategy;
        if (hasRunningPlaybackService()) {
            previousStrategy.clear(playbackService);
            startStrategy();
        }
    }

    private boolean hasRunningPlaybackService() {
        return playbackService != null;
    }

    private void startStrategy() {
        subscriptions.unsubscribe();
        subscriptions = new CompositeSubscription(
                eventBus.queue(EventQueue.PLAY_QUEUE_TRACK).subscribe(new CurrentTrackSubscriber(activeStrategy)),
                eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED).subscribe(new PlaybackStateSubscriber(playbackService, activeStrategy))
        );
    }

    private static class CurrentTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        private final Strategy strategy;

        public CurrentTrackSubscriber(Strategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            PropertySet trackData = event
                    .getCurrentMetaData()
                    .put(EntityProperty.URN, event.getCurrentTrackUrn());

            strategy.setTrack(trackData);
        }
    }

    private static class PlaybackStateSubscriber extends DefaultSubscriber<Player.StateTransition> {
        private final Strategy strategy;
        private final PlaybackService playbackService;

        public PlaybackStateSubscriber(PlaybackService playbackService, Strategy strategy) {
            this.playbackService = playbackService;
            this.strategy = strategy;
        }

        @Override
        public void onNext(Player.StateTransition stateTransition) {
            if (stateTransition.playSessionIsActive()) {
                strategy.notifyPlaying(playbackService);
            } else {
                strategy.notifyIdleState(playbackService);
            }
        }
    }
}
