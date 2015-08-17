package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.Playa.PlayaState;
import static com.soundcloud.android.playback.Playa.StateTransition;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;

@Singleton
public class PlaySessionController {

    @VisibleForTesting
    static final int RECOMMENDED_LOAD_TOLERANCE = 5;

    private final Resources resources;
    private final EventBus eventBus;
    private final PlaybackOperations playbackOperations;
    private final PlayQueueOperations playQueueOperations;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private final IRemoteAudioManager audioManager;
    private final ImageOperations imageOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastConnectionHelper castConnectionHelper;
    private final SharedPreferences sharedPreferences;
    private final Func1<Bitmap, Bitmap> copyBitmap = new Func1<Bitmap, Bitmap>() {
        @Override
        public Bitmap call(Bitmap bitmap) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
    };

    private Subscription currentTrackSubscription = RxUtils.invalidSubscription();
    private Subscription loadRecommendedSubscription = RxUtils.invalidSubscription();

    private PropertySet currentPlayQueueTrack; // the track that is currently set in the queue
    private boolean stopContinuousPlayback; // killswitch. If the api returns no tracks, stop asking for them

    @Inject
    public PlaySessionController(Resources resources,
                                 EventBus eventBus,
                                 PlaybackOperations playbackOperations,
                                 PlayQueueManager playQueueManager,
                                 TrackRepository trackRepository,
                                 Lazy<IRemoteAudioManager> audioManager,
                                 PlayQueueOperations playQueueOperations,
                                 ImageOperations imageOperations,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 CastConnectionHelper castConnectionHelper,
                                 SharedPreferences sharedPreferences) {
        this.resources = resources;
        this.eventBus = eventBus;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.playQueueOperations = playQueueOperations;
        this.sharedPreferences = sharedPreferences;
        this.audioManager = audioManager.get();
        this.imageOperations = imageOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castConnectionHelper = castConnectionHelper;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK,  new PlayQueueTrackSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE,  new PlayQueueSubscriber());
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            if (!StateTransition.DEFAULT.equals(stateTransition)) {
                audioManager.setPlaybackState(stateTransition.playSessionIsActive());
                skipOnTrackFinishOrUnplayable(stateTransition);
            }
        }
    }

    private void skipOnTrackFinishOrUnplayable(StateTransition stateTransition) {

        if (stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete()
                && (stateTransition.trackEnded() || unrecoverableErrorDuringAutoplay(stateTransition))) {
            
            tryToSkipTrack(stateTransition);
            if (!stateTransition.playSessionIsActive()) {
                playbackOperations.playCurrent();
            }
        }
    }

    private boolean unrecoverableErrorDuringAutoplay(StateTransition stateTransition) {
        final TrackSourceInfo currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        return stateTransition.wasError() && !stateTransition.wasGeneralFailure() &&
                currentTrackSourceInfo != null && !currentTrackSourceInfo.getIsUserTriggered();
    }

    private void tryToSkipTrack(StateTransition stateTransition) {
        if (!playQueueManager.autoNextTrack()) {
            eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createPlayQueueCompleteEvent(stateTransition.getTrackUrn()));
        }
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.isNewQueue()){
                loadRecommendedSubscription.unsubscribe();
                stopContinuousPlayback = false;
            }
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            if (currentQueueAllowsRecommendations()
                    && withinRecommendedFetchTolerance()
                    && isNotAlreadyLoadingRecommendations()) {
                loadRecommendations();
            }

            currentTrackSubscription.unsubscribe();
            currentTrackSubscription = trackRepository
                    .track(event.getCurrentTrackUrn())
                    .map(PropertySetFunctions.mergeInto(event.getCurrentMetaData()))
                    .subscribe(new CurrentTrackSubscriber());
        }
    }

    private boolean currentQueueAllowsRecommendations() {
        if (stopContinuousPlayback) {
            return false;
        } else {
            final PlaySessionSource currentPlaySessionSource = playQueueManager.getCurrentPlaySessionSource();
            return sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true) ||
                    currentPlaySessionSource.originatedInExplore() ||
                    Screen.DEEPLINK.get().equals(currentPlaySessionSource.getOriginScreen());
        }
    }

    private boolean withinRecommendedFetchTolerance() {
        return !playQueueManager.isQueueEmpty() &&
                playQueueManager.getQueueSize() - playQueueManager.getCurrentPosition() <= RECOMMENDED_LOAD_TOLERANCE;
    }

    private boolean isNotAlreadyLoadingRecommendations() {
        return loadRecommendedSubscription.isUnsubscribed();
    }

    private void loadRecommendations() {
        loadRecommendedSubscription = playQueueOperations
                .relatedTracksPlayQueue(playQueueManager.getLastTrackUrn(), fromContinuousPlay())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RecommendationTracksSubscriber());
    }

    // Hacky, but the similar sounds service needs to know if it is allowed to not fulfill this request. This should
    // only be allowed if we are not in explore, or serving a deeplink. This should be removed after rollout and we
    // have determined the service can handle the load we give it...
    private boolean fromContinuousPlay() {
        final PlaySessionSource currentPlaySessionSource = playQueueManager.getCurrentPlaySessionSource();
        return !(currentPlaySessionSource.originatedInExplore() ||
                currentPlaySessionSource.originatedFromDeeplink() ||
                currentPlaySessionSource.originatedInSearchSuggestions());
    }

    private final class CurrentTrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            if (castConnectionHelper.isCasting()) {
                playIfTrackChanged(track);
            } else if (playSessionStateProvider.isPlaying()) {
                playbackOperations.playCurrent();
            }

            currentPlayQueueTrack = track;
            updateRemoteAudioManager();
        }

        private void playIfTrackChanged(PropertySet newCurrentTrack) {
            Urn newCurrentTrackUrn = newCurrentTrack.get(TrackProperty.URN);
            Urn previousCurrentTrackUrn = getCurrentPlayQueueTrackUrn();
            if (playSessionStateProvider.isPlaying() &&
                    !newCurrentTrackUrn.equals(previousCurrentTrackUrn)) {
                playbackOperations.playCurrent();
            }
        }
    }

    private Urn getCurrentPlayQueueTrackUrn() {
        return currentPlayQueueTrack == null ? Urn.NOT_SET : currentPlayQueueTrack.get(TrackProperty.URN);
    }

    private final class ArtworkSubscriber extends DefaultSubscriber<Bitmap> {
        @Override
        public void onNext(Bitmap bitmap) {
            try {
                audioManager.onTrackChanged(currentPlayQueueTrack, bitmap);
            } catch (IllegalArgumentException e){
                logArtworkException(bitmap, e);
            }
        }

        private void logArtworkException(Bitmap bitmap, IllegalArgumentException e) {
            final String bitmapSize = bitmap == null ? "null" : bitmap.getWidth() + "x" + bitmap.getHeight();
            ErrorUtils.handleSilentException(e, Collections.singletonMap("bitmap_size", bitmapSize));
        }
    }

    private void updateRemoteAudioManager() {
        if (audioManager.isTrackChangeSupported()) {
            audioManager.onTrackChanged(currentPlayQueueTrack, null); // set initial data without bitmap so it doesn't have to wait
            final Urn resourceUrn = currentPlayQueueTrack.get(TrackProperty.URN);
            currentTrackSubscription = imageOperations.artwork(resourceUrn, ApiImageSize.getFullImageSize(resources))
                    .filter(validateBitmap(resourceUrn))
                    .map(copyBitmap)
                    .subscribe(new ArtworkSubscriber());
        }
    }

    // Trying to debug : https://github.com/soundcloud/SoundCloud-Android/issues/2984
    private Func1<Bitmap, Boolean> validateBitmap(final Urn resourceUrn) {
        return new Func1<Bitmap, Boolean>() {
            @Override
            public Boolean call(Bitmap bitmap) {
                if (bitmap == null){
                    ErrorUtils.handleSilentException(new IllegalArgumentException("Artwork bitmap is null " + resourceUrn));
                    return false;
                } else if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0){
                    ErrorUtils.handleSilentException(new IllegalArgumentException("Artwork bitmap has no size " + resourceUrn));
                    return false;
                } else {
                    return true;
                }
            }
        };
    }

    private StateTransition createPlayQueueCompleteEvent(Urn trackUrn){
        return new StateTransition(PlayaState.IDLE, Playa.Reason.PLAY_QUEUE_COMPLETE, trackUrn);
    }

    private class RecommendationTracksSubscriber extends DefaultSubscriber<PlayQueue> {
        @Override
        public void onNext(PlayQueue playQueue) {
            try {
                playQueueManager.appendUniquePlayQueueItems(playQueue);
                stopContinuousPlayback = playQueue.isEmpty();

            } catch (UnsupportedOperationException e) {
                // we should not need this, as we should never get this far with an empty queue.
                // Just being defensive while we investigate

                final HashMap<String, String> valuePairs = new HashMap<>(2);
                valuePairs.put("Queue Size" , String.valueOf(playQueueManager.getQueueSize()));
                valuePairs.put("PlaySessionSource", playQueueManager.getCurrentPlaySessionSource().toString());
                ErrorUtils.handleSilentException(e, valuePairs);
            }
        }
    }
}
