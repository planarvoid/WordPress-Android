package com.soundcloud.android.ads;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.VideoAdPlaybackItem;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

import rx.Subscription;

import static com.soundcloud.android.events.InlayAdEvent.InlayPlayStateTransition;

class InlayAdPlayer implements Player.PlayerListener {

    private final EventBus eventBus;
    private final PlaySessionController playSessionController;
    private final Player currentPlayer;
    private final CurrentDateProvider currentDateProvider;

    private Optional<VideoAd> currentAd = Optional.absent();

    private boolean shouldReturnToPlaySession;
    private boolean isPlayerMuted;

    private PlaybackStateTransition lastState = PlaybackStateTransition.DEFAULT;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public InlayAdPlayer(MediaPlayerAdapter mediaPlayerAdapter,
                         EventBus eventBus,
                         PlaySessionController playSessionController,
                         CurrentDateProvider currentDateProvider) {
        this.eventBus = eventBus;
        this.playSessionController = playSessionController;
        this.currentDateProvider = currentDateProvider;
        currentPlayer = mediaPlayerAdapter;
        currentPlayer.setListener(this);
    }

    void play(VideoAd videoAd) {
        final PlaybackItem playbackItem = VideoAdPlaybackItem.create(videoAd, 0L, 0.0f);
        if (isCurrentAd(videoAd) && wasPaused(playbackItem.getUrn())) {
            pausePlaySessionIfNeeded();
            currentPlayer.resume(playbackItem);
        } else if (!isCurrentAd(videoAd)) {
            isPlayerMuted = true; // Inlay ads begin muted
            currentAd = Optional.of(videoAd);
            currentPlayer.stopForTrackTransition();
            currentPlayer.play(playbackItem);

            subscription = eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED).subscribe(new PlayStateSubscriber());
        }
    }

    void toggleVolume() {
        toggleVolume(!isPlayerMuted);
    }

    private void toggleVolume(boolean mute) {
        isPlayerMuted = mute;
        currentPlayer.setVolume(mute ? 0.0f : 1.0f);

        if (mute && shouldReturnToPlaySession) {
            returnToPlaySession();
        } else if (!mute) {
            pausePlaySessionIfNeeded();
        }

        onPlaystateChanged(lastState);
    }

    private void pausePlaySessionIfNeeded() {
        if (!isPlayerMuted && playSessionController.isPlayingCurrentPlayQueueItem()) {
            playSessionController.pause();
            shouldReturnToPlaySession = true;
        }
    }

    private void returnToPlaySession() {
        playSessionController.play();
        shouldReturnToPlaySession = false;
    }

    void muteAndPause() {
        if (!isPlayerMuted) {
            toggleVolume(true);
        }
        currentPlayer.pause();
    }

    void pause() {
        if (shouldReturnToPlaySession) {
            returnToPlaySession();
        }
        currentPlayer.pause();
    }

    boolean isPlaying() {
        return lastState.isPlayerPlaying();
    }

    private boolean wasPaused(Urn urn) {
        return lastState.isForUrn(urn) && lastState.isPaused();
    }

    private boolean isCurrentAd(VideoAd videoAd) {
        return currentAd.isPresent() && currentAd.get().equals(videoAd);
    }

    @Override
    public void onPlaystateChanged(PlaybackStateTransition stateTransition) {
        lastState = stateTransition;

        if (lastState.playbackEnded() || lastState.wasError()) {
            if (shouldReturnToPlaySession) {
                returnToPlaySession();
            }
            subscription.unsubscribe();
        }

        if (currentAd.isPresent()) {
            final InlayPlayStateTransition event = InlayPlayStateTransition.create(currentAd.get(), lastState, isPlayerMuted, currentDateProvider.getCurrentDate());
            eventBus.publish(EventQueue.INLAY_AD, event);
        }
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        // no-op
    }

    private class PlayStateSubscriber extends DefaultSubscriber<PlayStateEvent> {
        @Override
        public void onNext(PlayStateEvent event) {
            if (event.isPlayerPlaying() && !isPlayerMuted) {
                toggleVolume(true);
            }
        }
    }
}
