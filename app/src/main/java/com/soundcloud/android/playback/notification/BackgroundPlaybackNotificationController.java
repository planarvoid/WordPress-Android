package com.soundcloud.android.playback.notification;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.PropertySetFunctions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Provider;

class BackgroundPlaybackNotificationController implements PlaybackNotificationController.Delegate {
    private final Resources resources;
    private final PlaybackNotificationPresenter presenter;
    private final TrackRepository trackRepository;
    private final NotificationManager notificationManager;
    private final ImageOperations imageOperations;
    private final Provider<NotificationBuilder> builderProvider;
    private final PlaybackStateProvider playbackStateProvider;
    private final Func1<PropertySet, Observable<NotificationBuilder>> toNotification = new Func1<PropertySet, Observable<NotificationBuilder>>() {
        @Override
        public Observable<NotificationBuilder> call(final PropertySet trackProperties) {
            presenter.updateTrackInfo(notificationBuilder, trackProperties);
            if (notificationBuilder.hasArtworkSupport()) {
                loadAndSetArtwork(trackProperties.get(TrackProperty.URN), notificationBuilder);
            }
            return Observable.just(notificationBuilder);
        }
    };

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private NotificationBuilder notificationBuilder;

    @Inject
    BackgroundPlaybackNotificationController(Resources resources, TrackRepository trackRepository, PlaybackNotificationPresenter presenter,
                                             NotificationManager notificationManager, ImageOperations imageOperations,
                                             Provider<NotificationBuilder> builderProvider, PlaybackStateProvider playbackStateProvider) {
        this.resources = resources;
        this.trackRepository = trackRepository;
        this.presenter = presenter;
        this.notificationManager = notificationManager;
        this.imageOperations = imageOperations;
        this.builderProvider = builderProvider;
        this.playbackStateProvider = playbackStateProvider;
    }

    @Override
    public void setTrack(PropertySet track) {
        createNotificationBuilder();
        subscriptions.unsubscribe();
        subscriptions = new CompositeSubscription(trackRepository
                .track(track.get(EntityProperty.URN))
                .observeOn(AndroidSchedulers.mainThread())
                .map(PropertySetFunctions.mergeInto(track))
                .flatMap(toNotification).cache()
                .subscribe(new NotificationSubscriber()));
    }

    private void createNotificationBuilder() {
        notificationBuilder = builderProvider.get();
        presenter.init(notificationBuilder, playbackStateProvider.isSupposedToBePlaying());
    }

    @Override
    public void clear() {
        notificationManager.cancel(NotificationConstants.PLAYBACK_NOTIFY_ID);
    }

    private void loadAndSetArtwork(final Urn trackUrn, final NotificationBuilder notificationBuilder) {
        final Bitmap cachedBitmap = getCachedBitmap(trackUrn, notificationBuilder);
        if (cachedBitmap != null) {
            notificationBuilder.setIcon(cachedBitmap);
        } else {
            notificationBuilder.setIcon(imageOperations.decodeResource(resources, R.drawable.notification_loading));
            subscriptions.add(getBitmap(trackUrn, notificationBuilder)
                    .subscribe(new DefaultSubscriber<Bitmap>() {
                        @Override
                        public void onNext(Bitmap bitmap) {
                            notificationBuilder.setIcon(bitmap);
                            notificationManager.notify(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
                        }
                    }));
        }
    }

    private Bitmap getCachedBitmap(final Urn trackUrn, final NotificationBuilder notificationBuilder) {
        if (notificationBuilder.getTargetImageSize() == NotificationBuilder.NOT_SET) {
            return imageOperations.getCachedBitmap(trackUrn, notificationBuilder.getImageSize());
        } else {
            return imageOperations.getCachedBitmap(trackUrn, notificationBuilder.getImageSize(), notificationBuilder.getTargetImageSize(), notificationBuilder.getTargetImageSize());
        }
    }

    private Observable<Bitmap> getBitmap(final Urn trackUrn, final NotificationBuilder notificationBuilder) {
        return imageOperations.artwork(trackUrn, notificationBuilder.getImageSize(), notificationBuilder.getTargetImageSize(), notificationBuilder.getTargetImageSize());
    }

    @Override
    public Notification notifyPlaying() {
        presenter.updateToPlayingState(notificationBuilder);
        return notificationBuilder.build();
    }

    @Override
    public boolean notifyIdleState() {
        if (notificationBuilder != null && notificationBuilder.hasPlayStateSupport()) {
            presenter.updateToIdleState(notificationBuilder);
            notificationManager.notify(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
            return true;
        }
        return false;
    }

    private class NotificationSubscriber extends DefaultSubscriber<NotificationBuilder> {
        @Override
        public void onNext(NotificationBuilder notification) {
            notificationBuilder = notification;
            notificationManager.notify(NotificationConstants.PLAYBACK_NOTIFY_ID, notificationBuilder.build());
        }
    }

}