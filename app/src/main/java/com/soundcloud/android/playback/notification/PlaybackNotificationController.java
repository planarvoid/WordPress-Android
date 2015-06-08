package com.soundcloud.android.playback.notification;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.propeller.PropertySet;
import rx.functions.Func1;

import android.app.Notification;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackNotificationController extends DefaultLightCycleActivity<AppCompatActivity> {

    interface Delegate {
        void setTrack(PropertySet track);

        void clear();

        @Nullable
        Notification notifyPlaying();

        boolean notifyIdleState();
    }

    private static final Func1<CurrentPlayQueueTrackEvent, PropertySet> toTrack = new Func1<CurrentPlayQueueTrackEvent, PropertySet>() {
        @Override
        public PropertySet call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
            return currentPlayQueueTrackEvent
                    .getCurrentMetaData()
                    .put(EntityProperty.URN, currentPlayQueueTrackEvent.getCurrentTrackUrn());
        }
    };

    private static final int DEFAULT_DELAY_MILLIS = 200;

    private final Runnable resetPlayback = new Runnable() {
        @Override
        public void run() {
            resetPlayback();
        }
    };

    private final Handler handler;
    private final EventBus eventBus;
    private final Delegate backgroundController;
    private final Delegate foregroundController;
    private final int delayMillis;

    /**
     * NOTE : this requires this class to be instantiated before the playback service or it will not receive the first
     * * One way to fix this would be to make the queue a replay queue, but its currently not necessary
     */
    private PlayerLifeCycleEvent lastPlayerLifecycleEvent = PlayerLifeCycleEvent.forDestroyed();
    private PropertySet playbackContext;
    private boolean isPlaying;
    private Delegate currentController;

    @Inject
    public PlaybackNotificationController(EventBus eventBus,
                                          BackgroundPlaybackNotificationController backgroundController,
                                          ForegroundPlaybackNotificationController foregroundController) {

        this(eventBus, backgroundController, foregroundController, DEFAULT_DELAY_MILLIS);
    }

    public PlaybackNotificationController(EventBus eventBus,
                                          BackgroundPlaybackNotificationController backgroundController,
                                          ForegroundPlaybackNotificationController foregroundController,
                                          int delayMillis) {
        this.eventBus = eventBus;
        this.backgroundController = backgroundController;
        this.foregroundController = foregroundController;
        this.currentController = backgroundController;
        this.delayMillis = delayMillis;
        this.handler = new Handler();
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK).map(toTrack).subscribe(new CurrentTrackSubscriber());
        eventBus.subscribe(EventQueue.PLAYER_LIFE_CYCLE, new PlayerLifeCycleSubscriber());
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        this.currentController = foregroundController;
        resetPlaybackContextDelayed();
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        this.currentController = backgroundController;
        resetPlaybackContextDelayed();
    }

    private void resetPlaybackContextDelayed() {
        if (delayMillis > 0) {
            handler.removeCallbacks(resetPlayback);
            handler.postDelayed(resetPlayback, delayMillis);
        } else {
            resetPlayback();
        }
    }

    private void resetPlayback() {
        if (lastPlayerLifecycleEvent.isServiceRunning()) {
            currentController.setTrack(playbackContext);
        } else {
            currentController.clear();
        }
        if (isPlaying) {
            currentController.notifyPlaying();
        } else {
            currentController.notifyIdleState();
        }
    }

    public Notification notifyPlaying() {
        isPlaying = true;
        return currentController.notifyPlaying();
    }

    public boolean notifyIdleState() {
        isPlaying = false;
        return currentController.notifyIdleState();
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            playbackContext = track;
            if (lastPlayerLifecycleEvent.isServiceRunning()) {
                currentController.setTrack(playbackContext);
            }
        }
    }

    private class PlayerLifeCycleSubscriber extends DefaultSubscriber<PlayerLifeCycleEvent> {
        @Override
        public void onNext(PlayerLifeCycleEvent playerLifecycleEvent) {
            lastPlayerLifecycleEvent = playerLifecycleEvent;
            if (!playerLifecycleEvent.isServiceRunning()) {
                currentController.clear();
            }
        }
    }
}
