package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class AdOrientationController extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final long ORIENTATION_UNLOCK_DELAY = TimeUnit.SECONDS.toMillis(5);

    private final AdsOperations adsOperations;
    private final EventBus eventBus;
    private final DeviceHelper deviceHelper;
    private final PlayQueueManager playQueueManager;

    private Subscription subscription = RxUtils.invalidSubscription();
    private Handler unlockHandler = new Handler();

    @Inject
    public AdOrientationController(AdsOperations adsOperations,
                                   EventBus eventBus,
                                   DeviceHelper deviceHelper,
                                   PlayQueueManager playQueueManager) {
        this.adsOperations = adsOperations;
        this.eventBus = eventBus;
        this.deviceHelper = deviceHelper;
        this.playQueueManager = playQueueManager;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                && adsOperations.isCurrentItemLetterboxVideoAd()) {
            unlockOrientationDelayed(activity);
        }
        subscription = new CompositeSubscription(
                eventBus.queue(EventQueue.PLAYER_COMMAND).subscribe(new PlayerCommandSubscriber(activity)),
                eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM).subscribe(new PlayQueueSubscriber(activity)));
    }

    /*
     * When a user taps "fullscreen" or "shrink", we temporarily lock orientation (giving them time to rotate the
     * device to the video orientation) before unlocking again so they still have control via rotation.
     */
    private void unlockOrientationDelayed(final Activity activity) {
        unlockHandler.postDelayed(() -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED), ORIENTATION_UNLOCK_DELAY);
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        if (adsOperations.isCurrentItemVideoAd() && activity.isChangingConfigurations()) {
            trackVideoSizeChange();
        }
        cancelOrientationUnlock();
        subscription.unsubscribe();
    }

    private void trackVideoSizeChange() {
        final VideoAd videoAd = (VideoAd) adsOperations.getCurrentTrackAdData().get();
        if (deviceHelper.isOrientation(Configuration.ORIENTATION_PORTRAIT)) {
            eventBus.publish(EventQueue.TRACKING,
                             UIEvent.fromVideoAdShrink(videoAd, playQueueManager.getCurrentTrackSourceInfo()));
        } else if (deviceHelper.isOrientation(Configuration.ORIENTATION_LANDSCAPE)) {
            eventBus.publish(EventQueue.TRACKING,
                             UIEvent.fromVideoAdFullscreen(videoAd, playQueueManager.getCurrentTrackSourceInfo()));
        }
    }

    private void cancelOrientationUnlock() {
        unlockHandler.removeCallbacksAndMessages(null);
    }

    private class PlayerCommandSubscriber extends DefaultSubscriber<PlayerUICommand> {

        final WeakReference<Activity> currentActivityRef;

        PlayerCommandSubscriber(Activity activity) {
            currentActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onNext(PlayerUICommand event) {
            final Activity currentActivity = currentActivityRef.get();
            if (currentActivity != null && adsOperations.isCurrentItemVideoAd()) {
                if (event.isForceLandscape()) {
                    currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (event.isForcePortrait()) {
                    currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        }
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {

        final WeakReference<Activity> currentActivityRef;

        PlayQueueSubscriber(Activity activity) {
            currentActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            final Activity activity = currentActivityRef.get();
            if (activity != null) {
                final PlayQueueItem currentItem = event.getCurrentPlayQueueItem();
                if (currentItem.isVideoAd()) {
                    lockOrientationInPortraitIfVertical(activity, (VideoAdQueueItem) currentItem);
                } else {
                    unlockOrientation(activity);
                }
            }
        }

        private void lockOrientationInPortraitIfVertical(Activity activity, VideoAdQueueItem videoItem) {
            if (videoItem.isVerticalVideo()) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        private void unlockOrientation(Activity activity) {
            cancelOrientationUnlock();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

}
