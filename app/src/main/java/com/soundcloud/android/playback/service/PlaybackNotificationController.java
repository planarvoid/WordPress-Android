package com.soundcloud.android.playback.service;

import com.soundcloud.android.R;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackNotificationController {

    static final int PLAYBACKSERVICE_STATUS_ID = 1;

    private final Context context;
    private final PlaybackNotificationPresenter presenter;
    private final TrackOperations trackOperations;
    private final NotificationManager notificationManager;
    private final EventBus eventBus;
    private final ImageOperations imageOperations;

    private final int targetIconWidth;
    private final int targetIconHeight;

    private Observable<Notification> notificationObservable;
    private Subscription imageSubscription = Subscriptions.empty();

    private final Action1<Notification> notifyAction = new Action1<Notification>() {
        @Override
        public void call(Notification notification) {
            notificationManager.notify(PLAYBACKSERVICE_STATUS_ID, notification);
        }
    };

    private final Func1<PlayQueueEvent, Observable<Notification>> onPlayQueueEventFunc = new Func1<PlayQueueEvent, Observable<Notification>>() {
        @Override
        public Observable<Notification> call(PlayQueueEvent playQueueEvent) {
            imageSubscription.unsubscribe();
            notificationObservable = trackOperations.track(playQueueEvent.getCurrentTrackUrn()).observeOn(AndroidSchedulers.mainThread())
                    .mergeMap(notificationFunction).cache();
            return notificationObservable;
        }
    };

    private final Func1<PropertySet, Observable<Notification>> notificationFunction = new Func1<PropertySet, Observable<Notification>>() {
        @Override
        public Observable<Notification> call(final PropertySet propertySet) {
            final Notification notification = presenter.createNotification(propertySet);
            if (presenter.artworkCapable()) {
                loadAndSetArtwork(propertySet.get(TrackProperty.URN), notification);
            }
            return Observable.just(notification);
        }
    };

    @Inject
    public PlaybackNotificationController(Context context, TrackOperations trackOperations, PlaybackNotificationPresenter presenter,
                                          NotificationManager notificationManager, EventBus eventBus, ImageOperations imageOperations) {
        this.context = context;
        this.trackOperations = trackOperations;
        this.presenter = presenter;
        this.notificationManager = notificationManager;
        this.eventBus = eventBus;
        this.imageOperations = imageOperations;

        this.targetIconWidth = context.getResources().getDimensionPixelSize(R.dimen.notification_image_large_width);
        this.targetIconHeight = context.getResources().getDimensionPixelSize(R.dimen.notification_image_large_height);
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAY_QUEUE).mergeMap(onPlayQueueEventFunc).doOnNext(notifyAction).subscribe();
    }

    Observable<Notification> playingNotification() {
        return notificationObservable.map(presenter.updateToPlayingState());
    }

    boolean notifyIdleState() {
        return presenter.updateToIdleState(notificationObservable, notifyAction);
    }

    private ApiImageSize getApiImageSize() {
        return ApiImageSize.getListItemImageSize(context);
    }

    private void loadAndSetArtwork(final TrackUrn trackUrn, final Notification notification) {
        final ApiImageSize apiImageSize = getApiImageSize();
        final Bitmap cachedBitmap = imageOperations.getCachedBitmap(trackUrn, apiImageSize, targetIconWidth, targetIconHeight);

        if (cachedBitmap != null) {
            presenter.setIcon(notification, imageOperations.getLocalImageUri(trackUrn, apiImageSize));
        } else {
            presenter.clearIcon(notification);

            imageSubscription = imageOperations.image(trackUrn, getApiImageSize(), targetIconWidth, targetIconHeight, false)
                    .subscribe(new DefaultSubscriber<Bitmap>() {
                @Override
                public void onNext(Bitmap args) {
                    presenter.setIcon(notification, imageOperations.getLocalImageUri(trackUrn, getApiImageSize()));
                    notificationManager.notify(PLAYBACKSERVICE_STATUS_ID, notification);
                }
            });
        }
    }
}
