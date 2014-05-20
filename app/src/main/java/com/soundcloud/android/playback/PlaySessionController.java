package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaySessionController {

    private static final StateTransition PLAY_QUEUE_COMPLETE_EVENT = new StateTransition(PlayaState.IDLE, Playa.Reason.PLAY_QUEUE_COMPLETE);
    private final Context context;
    private final EventBus eventBus;
    private final PlaybackOperations playbackOperations;
    private final TrackOperations trackOperations;
    private final PlayQueueManager playQueueManager;
    private final IRemoteAudioManager audioManager;
    private final ImageOperations imageOperations;


    private Subscription currentTrackSubscription = Subscriptions.empty();
    private StateTransition lastStateTransition = StateTransition.DEFAULT;

    private TrackUrn currentPlayingUrn; // the track that is currently loaded in the playback service
    private Track currentPlayQueueTrack; // the track that is currently set in the queue

    private PlaybackProgressEvent currentProgress;

    @Inject
    public PlaySessionController(Context context, EventBus eventBus, PlaybackOperations playbackOperations,
                                 PlayQueueManager playQueueManager, TrackOperations trackOperations, Lazy<IRemoteAudioManager> audioManager,
                                 ImageOperations imageOperations) {
        this.context = context;
        this.eventBus = eventBus;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.audioManager = audioManager.get();
        this.imageOperations = imageOperations;

    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    public boolean isPlayingTrack(Track track){
        return currentPlayingUrn != null && currentPlayingUrn.equals(track.getUrn());
    }

    public PlaybackProgressEvent getCurrentProgress() {
        return currentProgress;
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            lastStateTransition = stateTransition;
            currentPlayingUrn = stateTransition.getTrackUrn();

            audioManager.setPlaybackState(stateTransition.playSessionIsActive());

            if (stateTransition.trackEnded()){
                if (playQueueManager.autoNextTrack()){
                    playbackOperations.playCurrent(context);
                } else {
                    eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, PLAY_QUEUE_COMPLETE_EVENT);
                }
            }
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            currentTrackSubscription.unsubscribe();
            currentTrackSubscription = trackOperations.loadTrack(playQueueManager.getCurrentTrackId(), AndroidSchedulers.mainThread())
                    .subscribe(new CurrentTrackSubscriber());

            if (lastStateTransition.playSessionIsActive()) {
                playbackOperations.playCurrent(context);
            }
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            currentProgress = progress;
        }
    }

    private class CurrentTrackSubscriber extends DefaultSubscriber<Track> {
        @Override
        public void onNext(Track track) {
            currentPlayQueueTrack = track;
            updateRemoteAudioManager();
        }
    }

    private void updateRemoteAudioManager() {
        if (audioManager.isTrackChangeSupported()) {
            audioManager.onTrackChanged(currentPlayQueueTrack, null); // set initial data without bitmap so it doesn't have to wait
            currentTrackSubscription = imageOperations.loadLockscreenImage(context.getResources(), currentPlayQueueTrack.getUrn())
                    .subscribe(new DefaultSubscriber<Bitmap>() {
                        @Override
                        public void onNext(Bitmap bitmap) {
                            audioManager.onTrackChanged(currentPlayQueueTrack, bitmap);
                        }
                    });
        }
    }
}
