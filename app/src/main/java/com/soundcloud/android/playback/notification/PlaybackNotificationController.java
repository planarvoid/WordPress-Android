package com.soundcloud.android.playback.notification;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.propeller.PropertySet;

import android.app.Notification;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackNotificationController extends DefaultLightCycleActivity<AppCompatActivity> {

    interface Strategy {
        void setTrack(PropertySet track);

        void clear();

        @Nullable
        Notification notifyPlaying();

        boolean notifyIdleState();
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

    private PropertySet playbackContext;
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

    public void subscribe() {
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK).subscribe(new CurrentTrackSubscriber());
        eventBus.subscribe(EventQueue.PLAYER_LIFE_CYCLE, new PlayerLifeCycleSubscriber());
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
        activeStrategy.clear();
        activeStrategy = nextStrategy;
        activeStrategy.setTrack(playbackContext);
    }

    @Nullable
    public Notification notifyPlaying() {
        return activeStrategy.notifyPlaying();
    }

    public boolean notifyIdleState() {
        return activeStrategy.notifyIdleState();
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            playbackContext = event
                    .getCurrentMetaData()
                    .put(EntityProperty.URN, event.getCurrentTrackUrn());

            activeStrategy.setTrack(playbackContext);
        }
    }

    private class PlayerLifeCycleSubscriber extends DefaultSubscriber<PlayerLifeCycleEvent> {
        @Override
        public void onNext(PlayerLifeCycleEvent playerLifecycleEvent) {
            if (!playerLifecycleEvent.isServiceRunning()) {
                handler.removeCallbacksAndMessages(null);
                activeStrategy.clear();
            }
        }
    }
}
