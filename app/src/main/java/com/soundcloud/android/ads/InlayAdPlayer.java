package com.soundcloud.android.ads;

import static com.soundcloud.android.events.InlayAdEvent.InlayPlayStateTransition;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playback.VideoAdPlaybackItem;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class InlayAdPlayer implements Player.PlayerListener {

    private final EventBus eventBus;
    private final AdViewabilityController adViewabilityController;
    private final InlayAdAnalyticsController analyticsController;
    private final InlayAdStateProvider stateProvider;
    private final PlaySessionController playSessionController;
    private final Player currentPlayer;
    private final CurrentDateProvider currentDateProvider;

    private Optional<VideoAd> currentAd = Optional.absent();

    private boolean shouldReturnToPlaySession;
    private boolean isUserInitiated;
    private boolean isPlayerMuted;

    private PlaybackStateTransition lastState = PlaybackStateTransition.DEFAULT;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    InlayAdPlayer(MediaPlayerAdapter mediaPlayerAdapter,
                  EventBus eventBus,
                  AdViewabilityController adViewabilityController,
                  InlayAdAnalyticsController analyticsController,
                  InlayAdStateProvider inlayAdStateProvider,
                  PlaySessionController playSessionController,
                  CurrentDateProvider currentDateProvider) {
        this.eventBus = eventBus;
        this.adViewabilityController = adViewabilityController;
        this.analyticsController = analyticsController;
        this.stateProvider = inlayAdStateProvider;
        this.playSessionController = playSessionController;
        this.currentDateProvider = currentDateProvider;
        currentPlayer = mediaPlayerAdapter;
        currentPlayer.setListener(this);
    }

    public void autoplay(VideoAd videoAd) {
        if (!isPausedByUser(videoAd)) {
            play(videoAd, false);
        }
    }

    Optional<VideoAd> getCurrentAd() {
        return currentAd;
    }

    void play(VideoAd videoAd, boolean isUserInitiated) {
        final PlaybackItem playbackItem = VideoAdPlaybackItem.create(videoAd, 0L, 0.0f);
        if (isCurrentAd(videoAd) && wasPaused(playbackItem.getUrn())) {
            pausePlaySessionIfNeeded();
            setUserInitiated(isUserInitiated);
            currentPlayer.resume(playbackItem);
        } else if (!isCurrentAd(videoAd)) {
            isPlayerMuted = true; // Inlay ads begin muted
            currentAd = Optional.of(videoAd);
            setUserInitiated(isUserInitiated);
            currentPlayer.stopForTrackTransition();
            currentPlayer.play(playbackItem);

            subscription = eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED).subscribe(new PlayStateSubscriber());
        }
    }

    public void togglePlayback(VideoAd videoAd) {
        if (isPlaying()) {
            pause();
        } else {
            if (wasPlaybackComplete(videoAd.getAdUrn())) {
                currentAd = Optional.absent();
            }
            play(videoAd, true);
        }
    }

    void toggleVolume() {
        toggleVolume(!isPlayerMuted);
    }

    private void toggleVolume(boolean mute) {
        isPlayerMuted = mute;
        currentPlayer.setVolume(mute ? 0.0f : 1.0f);

        publishVolumeToggleEvent(mute);
        if (mute && shouldReturnToPlaySession) {
            returnToPlaySession();
        } else if (!mute) {
            pausePlaySessionIfNeeded();
        }

        onPlaystateChanged(lastState);
    }

    private void publishVolumeToggleEvent(boolean mute) {
        if (currentAd.isPresent()) {
            final VideoAd ad = currentAd.get();
            final UIEvent event = mute ? UIEvent.fromVideoMute(ad, getSourceInfo())
                                       : UIEvent.fromVideoUnmute(ad, getSourceInfo());
            adViewabilityController.onVolumeToggle(ad, mute);
            eventBus.publish(EventQueue.TRACKING, event);
        }
    }

    private void pausePlaySessionIfNeeded() {
        if (!isPlayerMuted && playSessionController.isPlaying()) {
            playSessionController.pause();
            shouldReturnToPlaySession = true;
        }
    }

    private void returnToPlaySession() {
        playSessionController.play();
        shouldReturnToPlaySession = false;
    }

    void autopause(boolean shouldMute) {
        setUserInitiated(false);
        if (!isPlayerMuted && shouldMute) {
            toggleVolume(true);
        }
        currentPlayer.pause();
    }

    void pause() {
        setUserInitiated(true);
        if (shouldReturnToPlaySession) {
            returnToPlaySession();
        }
        currentPlayer.pause();
    }

    void reset() {
        shouldReturnToPlaySession = false;
        isUserInitiated = false;
        isPlayerMuted = false;
        lastState = PlaybackStateTransition.DEFAULT;
        currentAd = Optional.absent();
        subscription.unsubscribe();
        currentPlayer.destroy();
    }

    boolean isPlaying() {
        return lastState.isPlayerPlaying();
    }

    private boolean isPausedByUser(VideoAd videoAd) {
        return currentAd.isPresent()
                && currentAd.get().equals(videoAd)
                && lastState.isPaused()
                && isUserInitiated;
    }

    private boolean wasPaused(Urn urn) {
        return lastState.isForUrn(urn) && lastState.isPaused();
    }

    private boolean wasPlaybackComplete(Urn urn) {
        return lastState.isForUrn(urn) && lastState.playbackEnded();
    }

    private boolean isCurrentAd(VideoAd videoAd) {
        return currentAd.isPresent() && currentAd.get().equals(videoAd);
    }

    private void setUserInitiated(boolean isUserInitiated) {
        this.isUserInitiated = isUserInitiated;
    }

    private TrackSourceInfo getSourceInfo() {
        return new TrackSourceInfo(Screen.STREAM.get(), isUserInitiated);
    }

    @Override
    public void onPlaystateChanged(PlaybackStateTransition stateTransition) {
        Log.d(Log.ADS_TAG, String.format("InlayAdPlayer: Muted: %s, %s", isPlayerMuted, stateTransition.toString()));
        lastState = stateTransition;

        if (lastState.playbackEnded() || lastState.wasError()) {
            if (shouldReturnToPlaySession) {
                returnToPlaySession();
            }
            subscription.unsubscribe();
        }

        if (currentAd.isPresent()) {
            final VideoAd ad = currentAd.get();
            final PlayStateEvent playState = PlayStateEvent.create(stateTransition, stateTransition.getProgress().getDuration());
            final InlayPlayStateTransition stateEvent = InlayPlayStateTransition.create(ad, lastState, isPlayerMuted, currentDateProvider.getCurrentDate());
            analyticsController.onStateTransition(Screen.STREAM, isUserInitiated, ad, playState);
            stateProvider.put(ad.getUuid(), stateEvent);
            eventBus.publish(EventQueue.INLAY_AD, stateEvent);
        }
    }

    @Override
    public void onProgressEvent(long progress, long duration) {
        if (currentAd.isPresent()) {
            final VideoAd adData = currentAd.get();
            final Urn urn = adData.getAdUrn();
            analyticsController.onProgressEvent(adData, PlaybackProgressEvent.create(new PlaybackProgress(progress, duration, urn), urn));
        }
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
