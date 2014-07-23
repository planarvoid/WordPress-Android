package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.LegacyTrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.res.Resources;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaySessionController {

    private final Resources resources;
    private final EventBus eventBus;
    private final PlaybackOperations playbackOperations;
    private final LegacyTrackOperations trackOperations;
    private final PlayQueueManager playQueueManager;
    private final IRemoteAudioManager audioManager;
    private final ImageOperations imageOperations;
    private final PlaySessionStateProvider playSessionStateProvider;

    private Subscription currentTrackSubscription = Subscriptions.empty();
    private PublicApiTrack currentPlayQueueTrack; // the track that is currently set in the queue

    @Inject
    public PlaySessionController(Resources resources, EventBus eventBus, PlaybackOperations playbackOperations,
                                 PlayQueueManager playQueueManager, LegacyTrackOperations trackOperations, Lazy<IRemoteAudioManager> audioManager,
                                 ImageOperations imageOperations, PlaySessionStateProvider playSessionStateProvider) {
        this.resources = resources;
        this.eventBus = eventBus;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.audioManager = audioManager.get();
        this.imageOperations = imageOperations;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK,  new PlayQueueTrackSubscriber());
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {

            if (!StateTransition.DEFAULT.equals(stateTransition)) {
                audioManager.setPlaybackState(stateTransition.playSessionIsActive());

                if ((stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete())) {
                    if (stateTransition.trackEnded()) {
                        if (!playQueueManager.autoNextTrack()) {
                            eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createPlayQueueCompleteEvent(stateTransition.getTrackUrn()));
                        }
                    }
                }
            }
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            currentTrackSubscription.unsubscribe();
            currentTrackSubscription = trackOperations
                    .loadTrack(event.getCurrentTrackUrn().numericId, AndroidSchedulers.mainThread())
                    .subscribe(new CurrentTrackSubscriber());

            if (playSessionStateProvider.isPlaying()) {
                playbackOperations.playCurrent();
            }
        }
    }

    private final class CurrentTrackSubscriber extends DefaultSubscriber<PublicApiTrack> {
        @Override
        public void onNext(PublicApiTrack track) {
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

    private StateTransition createPlayQueueCompleteEvent(TrackUrn trackUrn){
        return new StateTransition(PlayaState.IDLE, Playa.Reason.PLAY_QUEUE_COMPLETE, trackUrn);
    }
}
