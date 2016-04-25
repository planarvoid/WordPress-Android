package com.soundcloud.android.playback.notification;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.app.NotificationManager;
import android.app.Service;
import android.content.res.Resources;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Provider;

class BackgroundPlaybackNotificationController implements PlaybackNotificationController.Strategy {

    private final PlaybackNotificationPresenter presenter;
    private final TrackRepository trackRepository;
    private final NotificationManager notificationManager;
    private final ImageOperations imageOperations;
    private final Provider<NotificationBuilder> builderProvider;
    private final PlaybackStateProvider playbackStateProvider;
    private final Resources resources;

    private final Func1<PropertySet, Observable<NotificationBuilder>> toNotification = new Func1<PropertySet, Observable<NotificationBuilder>>() {
        @Override
        public Observable<NotificationBuilder> call(final PropertySet trackProperties) {
            presenter.updateTrackInfo(notificationBuilder, trackProperties);
            if (trackProperties.get(AdProperty.IS_AUDIO_AD)) {
                notificationBuilder.setIcon(imageOperations.decodeResource(resources, R.drawable.notification_loading));
            } else {
                loadAndSetArtwork(SimpleImageResource.create(trackProperties), notificationBuilder);
            }
            return Observable.just(notificationBuilder);
        }
    };

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private NotificationBuilder notificationBuilder;

    @Inject
    BackgroundPlaybackNotificationController(TrackRepository trackRepository, PlaybackNotificationPresenter presenter,
                                             NotificationManager notificationManager, ImageOperations imageOperations,
                                             Provider<NotificationBuilder> builderProvider,
                                             PlaybackStateProvider playbackStateProvider, Resources resources) {
        this.trackRepository = trackRepository;
        this.presenter = presenter;
        this.notificationManager = notificationManager;
        this.imageOperations = imageOperations;
        this.builderProvider = builderProvider;
        this.playbackStateProvider = playbackStateProvider;
        this.resources = resources;
    }

    @Override
    public void setTrack(PlaybackService playbackService, PropertySet track) {
        createNotificationBuilder();
        subscriptions.unsubscribe();
        subscriptions = new CompositeSubscription(trackRepository
                .track(track.get(EntityProperty.URN))
                .observeOn(AndroidSchedulers.mainThread())
                .map(PropertySetFunctions.mergeInto(track))
                .flatMap(toNotification).cache()
                .subscribe(new NotificationSubscriber(playbackService)));
    }

    private void createNotificationBuilder() {
        notificationBuilder = builderProvider.get();
        presenter.init(notificationBuilder, playbackStateProvider.isSupposedToBePlaying());
    }

    @Override
    public void clear(PlaybackService playbackService) {
        playbackService.stopForeground(true);
        notificationManager.cancel(NotificationConstants.PLAYBACK_NOTIFY_ID);
    }

    private void loadAndSetArtwork(final ImageResource imageResource, final NotificationBuilder notificationBuilder) {
        final Bitmap cachedBitmap = getCachedBitmap(imageResource, notificationBuilder);
        if (cachedBitmap != null) {
            notificationBuilder.setIcon(cachedBitmap);
        } else {
            subscriptions.add(getBitmap(imageResource, notificationBuilder)
                    .subscribe(new DefaultSubscriber<Bitmap>() {
                        @Override
                        public void onNext(Bitmap bitmap) {
                            notificationBuilder.setIcon(bitmap);
                            notificationManager.notify(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
                        }
                    }));
        }
    }

    private Bitmap getCachedBitmap(final ImageResource imageResource, final NotificationBuilder notificationBuilder) {
        if (notificationBuilder.getTargetImageSize() == NotificationBuilder.NOT_SET) {
            return imageOperations.getCachedBitmap(imageResource, notificationBuilder.getImageSize());
        } else {
            return imageOperations.getCachedBitmap(imageResource, notificationBuilder.getImageSize(), notificationBuilder.getTargetImageSize(), notificationBuilder.getTargetImageSize());
        }
    }

    private Observable<Bitmap> getBitmap(final ImageResource imageResource, final NotificationBuilder notificationBuilder) {
        return imageOperations.artwork(imageResource, notificationBuilder.getImageSize(), notificationBuilder.getTargetImageSize(), notificationBuilder.getTargetImageSize());
    }

    @Override
    public void notifyPlaying(PlaybackService playbackService) {
        if (notificationBuilder != null){
            presenter.updateToPlayingState(notificationBuilder);
            playbackService.startForeground(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
        }

    }

    @Override
    public void notifyIdleState(PlaybackService playbackService) {
        final boolean removeNotification;
        if (notificationBuilder != null && notificationBuilder.hasPlayStateSupport()) {
            presenter.updateToIdleState(notificationBuilder);
            playbackService.startForeground(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
            removeNotification =  false;
        } else {
            removeNotification = true;
        }
        playbackService.stopForeground(removeNotification);
    }

    private class NotificationSubscriber extends DefaultSubscriber<NotificationBuilder> {

        private final Service playbackService;

        private NotificationSubscriber(Service playbackService) {
            this.playbackService = playbackService;
        }

        @Override
        public void onNext(NotificationBuilder notification) {
            notificationBuilder = notification;
            if (playbackStateProvider.isSupposedToBePlaying()) {
                playbackService.startForeground(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
            } else {
                notificationManager.notify(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
            }

        }
    }

}
