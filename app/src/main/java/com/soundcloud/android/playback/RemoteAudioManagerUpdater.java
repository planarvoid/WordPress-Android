package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdFunctions;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.res.Resources;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RemoteAudioManagerUpdater {

    private final TrackRepository trackRepository;
    private final IRemoteAudioManager audioManager;
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final ImageOperations imageOperations;
    private final Resources resources;

    private Subscription currentTrackSubscription = RxUtils.invalidSubscription();

    private final Func1<Bitmap, Bitmap> copyBitmap = new Func1<Bitmap, Bitmap>() {
        @Override
        public Bitmap call(Bitmap bitmap) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
    };

    private final Func1<PropertySet, Observable<TrackAndBitmap>> loadArtwork = new Func1<PropertySet, Observable<TrackAndBitmap>>() {
        @Override
        public Observable<TrackAndBitmap> call(final PropertySet track) {
            final Urn resourceUrn = track.get(TrackProperty.URN);
            return imageOperations.artwork(resourceUrn, ApiImageSize.getFullImageSize(resources))
                    .filter(validateBitmap(resourceUrn))
                    .map(copyBitmap)
                    .map(new Func1<Bitmap, TrackAndBitmap>() {
                        @Override
                        public RemoteAudioManagerUpdater.TrackAndBitmap call(Bitmap bitmap) {
                            return new RemoteAudioManagerUpdater.TrackAndBitmap(track, bitmap);
                        }
                    });
        }
    };

    @Inject
    public RemoteAudioManagerUpdater(TrackRepository trackRepository,
                                     Lazy<IRemoteAudioManager> audioManager,
                                     EventBus eventBus,
                                     PlayQueueManager playQueueManager,
                                     ImageOperations imageOperations,
                                     Resources resources) {
        this.trackRepository = trackRepository;
        this.resources = resources;
        this.audioManager = audioManager.get();
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.imageOperations = imageOperations;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new TrackChangedSubscriber());
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(new Func1<Player.StateTransition, Boolean>() {
                    @Override
                    public Boolean call(Player.StateTransition stateTransition) {
                        return playQueueManager.isCurrentTrack(stateTransition.getUrn());
                    }
                })
                .subscribe(new PlaybackStateSubscriber());
    }

    private class TrackChangedSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {

        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            currentTrackSubscription.unsubscribe();

            final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
            if (playQueueItem.isTrack()) {
                final boolean isAudioAd = AdFunctions.IS_AUDIO_AD_ITEM.apply(playQueueItem);
                currentTrackSubscription = trackRepository
                        .track(playQueueItem.getUrn())
                        .map(PropertySetFunctions.mergeWith(PropertySet.from(AdProperty.IS_AUDIO_AD.bind(isAudioAd))))
                        .filter(new Func1<PropertySet, Boolean>() {
                            @Override
                            public Boolean call(PropertySet propertyBindings) {
                                return audioManager.isTrackChangeSupported();
                            }
                        })
                        .doOnNext(new Action1<PropertySet>() {
                            @Override
                            public void call(PropertySet propertyBindings) {
                                // set initial data without bitmap so it doesn't have to wait
                                audioManager.onTrackChanged(propertyBindings, null);
                            }
                        })
                        .flatMap(loadArtwork)
                        .subscribe(new ArtworkSubscriber());
            }
        }
    }

    private final class ArtworkSubscriber extends DefaultSubscriber<TrackAndBitmap> {
        @Override
        public void onNext(TrackAndBitmap trackAndBitmap) {
            audioManager.onTrackChanged(trackAndBitmap.track, trackAndBitmap.bitmap);
        }
    }

    // Trying to debug : https://github.com/soundcloud/SoundCloud-Android/issues/2984
    private Func1<Bitmap, Boolean> validateBitmap(final Urn resourceUrn) {
        return new Func1<Bitmap, Boolean>() {
            @Override
            public Boolean call(Bitmap bitmap) {
                if (bitmap == null) {
                    ErrorUtils.handleSilentException(new IllegalArgumentException("Artwork bitmap is null " + resourceUrn));
                    return false;
                } else if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                    ErrorUtils.handleSilentException(new IllegalArgumentException("Artwork bitmap has no size " + resourceUrn));
                    return false;
                } else {
                    return true;
                }
            }
        };
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<Player.StateTransition> {
        @Override
        public void onNext(Player.StateTransition stateTransition) {
            audioManager.setPlaybackState(stateTransition.playSessionIsActive());
        }
    }

    private final class TrackAndBitmap {
        private final PropertySet track;
        private final Bitmap bitmap;

        private TrackAndBitmap(PropertySet track, Bitmap bitmap) {
            this.track = track;
            this.bitmap = bitmap;
        }
    }

}
