package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.google.common.collect.Maps;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.LegacyTrackOperations;
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.res.Resources;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Singleton
public class PlaySessionController {

    private static final StateTransition PLAY_QUEUE_COMPLETE_EVENT = new StateTransition(PlayaState.IDLE, Playa.Reason.PLAY_QUEUE_COMPLETE);
    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private final Resources resources;
    private final EventBus eventBus;
    private final PlaybackOperations playbackOperations;
    private final LegacyTrackOperations trackOperations;
    private final PlayQueueManager playQueueManager;
    private final IRemoteAudioManager audioManager;
    private final ImageOperations imageOperations;

    private Subscription currentTrackSubscription = Subscriptions.empty();
    private StateTransition lastStateTransition = StateTransition.DEFAULT;

    private TrackUrn currentPlayingUrn; // the track that is currently loaded in the playback service
    private Track currentPlayQueueTrack; // the track that is currently set in the queue

    private final Map<TrackUrn, PlaybackProgress> progressMap = Maps.newHashMap();

    @Inject
    public PlaySessionController(Resources resources, EventBus eventBus, PlaybackOperations playbackOperations,
                                 PlayQueueManager playQueueManager, LegacyTrackOperations trackOperations, Lazy<IRemoteAudioManager> audioManager,
                                 ImageOperations imageOperations) {
        this.resources = resources;
        this.eventBus = eventBus;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.audioManager = audioManager.get();
        this.imageOperations = imageOperations;

    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber());
    }

    public boolean isPlayingTrack(Track track) {
        return isPlayingTrack(track.getUrn());
    }

    public boolean isPlayingTrack(TrackUrn trackUrn) {
        return currentPlayingUrn != null && currentPlayingUrn.equals(trackUrn);
    }

    public boolean isPlaying() {
        return lastStateTransition.playSessionIsActive();
    }

    public PlaybackProgress getCurrentProgress() {
        return getCurrentProgress(currentPlayingUrn);
    }

    public PlaybackProgress getCurrentProgress(TrackUrn trackUrn) {
        final PlaybackProgress playbackProgress = progressMap.get(trackUrn);
        return playbackProgress == null ? PlaybackProgress.empty() : playbackProgress;
    }

    public boolean isProgressWithinTrackChangeThreshold() {
        return getCurrentProgress().getPosition() < PROGRESS_THRESHOLD_FOR_TRACK_CHANGE;
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {

            if (!StateTransition.DEFAULT.equals(stateTransition)) {
                lastStateTransition = stateTransition;
                currentPlayingUrn = stateTransition.getTrackUrn();
                progressMap.put(currentPlayingUrn, stateTransition.getProgress());

                audioManager.setPlaybackState(stateTransition.playSessionIsActive());

                if (stateTransition.isPlayerIdle() && !stateTransition.equals(PLAY_QUEUE_COMPLETE_EVENT)) {
                    if (stateTransition.trackEnded()) {
                        if (playQueueManager.autoNextTrack()) {
                            playbackOperations.playCurrent();
                        } else {
                            eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, PLAY_QUEUE_COMPLETE_EVENT);
                        }
                    }
                    saveProgress(stateTransition);
                }
            }
        }
    }

    private void saveProgress(StateTransition stateTransition) {
        playQueueManager.saveCurrentPosition(stateTransition.trackEnded() ? 0 : stateTransition.getProgress().getPosition());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            currentTrackSubscription.unsubscribe();
            currentTrackSubscription = trackOperations.loadTrack(playQueueManager.getCurrentTrackId(), AndroidSchedulers.mainThread())
                    .subscribe(new CurrentTrackSubscriber());

            if (lastStateTransition.playSessionIsActive()) {
                playbackOperations.playCurrent();
            }
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            progressMap.put(progress.getTrackUrn(), progress.getPlaybackProgress());
        }
    }

    private final class CurrentTrackSubscriber extends DefaultSubscriber<Track> {
        @Override
        public void onNext(Track track) {
            currentPlayQueueTrack = track;
            updateRemoteAudioManager();
        }
    }

    private final class ArtworkSubscriber extends DefaultSubscriber<Bitmap> {
        @Override
        public void onNext(Bitmap bitmap) {
            audioManager.onTrackChanged(currentPlayQueueTrack, bitmap);
        }
    }

    private void updateRemoteAudioManager() {
        if (audioManager.isTrackChangeSupported()) {
            audioManager.onTrackChanged(currentPlayQueueTrack, null); // set initial data without bitmap so it doesn't have to wait
            currentTrackSubscription = imageOperations.image(currentPlayQueueTrack.getUrn(), ApiImageSize.getFullImageSize(resources), true)
                    .subscribe(new ArtworkSubscriber());
        }
    }
}
