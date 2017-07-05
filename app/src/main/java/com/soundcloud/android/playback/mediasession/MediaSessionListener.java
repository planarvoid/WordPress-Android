package com.soundcloud.android.playback.mediasession;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_HEADSETHOOK;
import static com.soundcloud.android.rx.observers.LambdaObserver.onNext;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackActionSource;
import com.soundcloud.android.playback.PlayerInteractionsTracker;
import com.soundcloud.android.playback.external.PlaybackAction;
import com.soundcloud.android.playback.external.PlaybackActionController;
import com.soundcloud.android.rx.RxUtils;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
    private final PlayerInteractionsTracker playerInteractionsTracker;
    private final PlaySessionStateProvider playSessionStateProvider;

    private int clicks;
    private Disposable disposable = RxUtils.invalidDisposable();

    public MediaSessionListener(MediaSessionController mediaSessionController,
                                PlaybackActionController playbackActionController,
                                Context context,
                                PlayerInteractionsTracker playerInteractionsTracker,
                                PlaySessionStateProvider playSessionStateProvider) {
        this.mediaSessionController = mediaSessionController;
        this.playbackActionController = playbackActionController;
        this.context = context;
        this.playerInteractionsTracker = playerInteractionsTracker;
        this.playSessionStateProvider = playSessionStateProvider;
        this.scheduler = Schedulers.io();
    }

    @VisibleForTesting
    public MediaSessionListener(MediaSessionController mediaSessionController,
                                PlaybackActionController playbackActionController,
                                Context context,
                                Scheduler scheduler,
                                PlayerInteractionsTracker playerInteractionsTracker,
                                PlaySessionStateProvider playSessionStateProvider) {
        this.mediaSessionController = mediaSessionController;
        this.playbackActionController = playbackActionController;
        this.context = context;
        this.scheduler = scheduler;
        this.playerInteractionsTracker = playerInteractionsTracker;
        this.playSessionStateProvider = playSessionStateProvider;
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
        playerInteractionsTracker.pause(PlaybackActionSource.NOTIFICATION);
        handleAction(PlaybackAction.PAUSE);
    }

    @Override
    public void onPlay() {
        playerInteractionsTracker.play(PlaybackActionSource.NOTIFICATION);
        if (!mediaSessionController.isPlayingVideoAd()) {
            handleAction(PlaybackAction.PLAY);
        } else {
            Toast.makeText(context, R.string.ads_reopen_to_continue, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStop() {
        handleAction(PlaybackAction.CLOSE);
    }

    @Override
    public void onSkipToNext() {
        skipToNext(PlaybackActionSource.NOTIFICATION);
    }

    private void skipToNext(PlaybackActionSource playerType) {
        playerInteractionsTracker.clickForward(playerType);
        handleAction(PlaybackAction.NEXT);
        mediaSessionController.onSkip();
    }

    @Override
    public void onSkipToPrevious() {
        skipToPrevious(PlaybackActionSource.NOTIFICATION);
    }

    private void skipToPrevious(PlaybackActionSource playerType) {
        playerInteractionsTracker.clickBackward(playerType);
        handleAction(PlaybackAction.PREVIOUS);
        mediaSessionController.onSkip();
    }

    private void togglePlayBack() {
        trackTogglePlayback();
        handleAction(PlaybackAction.TOGGLE_PLAYBACK);
    }

    private void trackTogglePlayback() {
        if (playSessionStateProvider.isPlaying()) {
            playerInteractionsTracker.pause(PlaybackActionSource.OTHER);
        } else {
            playerInteractionsTracker.play(PlaybackActionSource.OTHER);
        }
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
        disposable.dispose();
        clicks += 1;

        if (clicks == 3) {
            skipToPrevious(PlaybackActionSource.OTHER);
        }

        disposable = Observable
                .timer(HEADSET_DELAY_MS, TimeUnit.MILLISECONDS, scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(onNext(__ -> handleHeadsetTimeout()));
    }

    private void handleAction(String action) {
        playbackActionController.handleAction(action, PlaybackActionSource.NOTIFICATION);
    }

    private void handleHeadsetTimeout() {
        if (clicks == 1) {
            togglePlayBack();
        } else if (clicks == 2) {
            skipToNext(PlaybackActionSource.OTHER);
        }
        clicks = 0;
    }
}
