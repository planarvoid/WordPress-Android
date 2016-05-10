package com.soundcloud.android.playback.mediasession;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_HEADSETHOOK;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.external.PlaybackActionController;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MediaSessionListener extends MediaSessionCompat.Callback {
    @VisibleForTesting
    public static final int HEADSET_DELAY_MS = 300;

    private final MediaSessionController mediaSessionController;
    private final PlaybackActionController playbackActionController;
    private final Scheduler scheduler;
    private final Context context;

    private int clicks;
    private Subscription subscription = RxUtils.invalidSubscription();

    public MediaSessionListener(MediaSessionController mediaSessionController,
                                PlaybackActionController playbackActionController,
                                Context context) {
        this.mediaSessionController = mediaSessionController;
        this.playbackActionController = playbackActionController;
        this.context = context;
        this.scheduler = Schedulers.newThread();
    }

    @VisibleForTesting
    public MediaSessionListener(MediaSessionController mediaSessionController,
                                PlaybackActionController playbackActionController,
                                Context context,
                                Scheduler scheduler) {
        this.mediaSessionController = mediaSessionController;
        this.playbackActionController = playbackActionController;
        this.context = context;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        String intentAction = mediaButtonEvent.getAction();

        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (isHeadsetHookEvent(event)) {
                handleHeadsetHook(event);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onPause() {
        handleAction(PlaybackAction.PAUSE);
    }

    @Override
    public void onPlay() {
        if (!mediaSessionController.isPlayingVideoAd()) {
            handleAction(PlaybackAction.PLAY);
        } else {
            Toast.makeText(context, R.string.ads_reopen_app_to_continue, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStop() {
        handleAction(PlaybackAction.CLOSE);
    }

    @Override
    public void onSkipToNext() {
        handleAction(PlaybackAction.NEXT);
        mediaSessionController.onSkip();
    }

    @Override
    public void onSkipToPrevious() {
        handleAction(PlaybackAction.PREVIOUS);
        mediaSessionController.onSkip();
    }

    private void onTogglePlayback() {
        handleAction(PlaybackAction.TOGGLE_PLAYBACK);
    }

    private boolean isHeadsetHookEvent(KeyEvent event) {
        return event != null && event.getKeyCode() == KEYCODE_HEADSETHOOK;
    }

    private void handleHeadsetHook(KeyEvent event) {
        if (event.getAction() == ACTION_DOWN) {
            onHeadsetHook();
        }
    }

    private void onHeadsetHook() {
        subscription.unsubscribe();
        clicks += 1;

        if (clicks == 3) {
            onSkipToPrevious();
        }

        subscription = Observable
                .timer(HEADSET_DELAY_MS, TimeUnit.MILLISECONDS, scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OnHeadsetTimeoutSubscriber());
    }

    private void handleAction(String action) {
        playbackActionController.handleAction(action, PlaybackActionReceiver.SOURCE_REMOTE);
    }

    private class OnHeadsetTimeoutSubscriber extends DefaultSubscriber<Long> {
        @Override
        public void onNext(Long ignored) {
            if (clicks == 1) {
                onTogglePlayback();
            } else if (clicks == 2) {
                onSkipToNext();
            }
            clicks = 0;
        }
    }
}
