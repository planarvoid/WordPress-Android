package com.soundcloud.android.playback.service;

import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.graphics.Bitmap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaybackNotificationController {

    static final int PLAYBACKSERVICE_STATUS_ID = 1;

    private final Resources resources;
    private final PlaybackNotificationPresenter presenter;
    private final TrackOperations trackOperations;
    private final NotificationManager notificationManager;
    private final EventBus eventBus;
    private final ImageOperations imageOperations;
    private PlayQueueManager playQueueManager;

    private final int targetIconWidth;
    private final int targetIconHeight;

    /** NOTE : this requires this class to be instantiated before the playback service or it will not receive the first
     ** One way to fix this would be to make the queue a replay queue, but its currently not necessary */
    private PlayerLifeCycleEvent lastPlayerLifecycleEvent = PlayerLifeCycleEvent.forDestroyed();

    private Observable<Notification> notificationObservable;
    private Subscription imageSubscription = Subscriptions.empty();

    private final Action1<Notification> notifyAction = new Action1<Notification>() {
        @Override
        public void call(Notification notification) {
            if (lastPlayerLifecycleEvent.isServiceRunning()){
                notificationManager.notify(PLAYBACKSERVICE_STATUS_ID, notification);
            }
        }
    };

    private final Func1<CurrentPlayQueueTrackEvent, Observable<Notification>> onPlayQueueEventFunc = new Func1<CurrentPlayQueueTrackEvent, Observable<Notification>>() {
        @Override
        public Observable<Notification> call(CurrentPlayQueueTrackEvent playQueueEvent) {
            imageSubscription.unsubscribe();
            notificationObservable = trackOperations
                    .track(playQueueEvent.getCurrentTrackUrn()).observeOn(AndroidSchedulers.mainThread())
                    .map(mergeMetaData(playQueueEvent.getCurrentMetaData()))
                    .mergeMap(toNotification).cache();

            return notificationObservable;
        }
    };

    private Func1<PropertySet, PropertySet> mergeMetaData(final PropertySet metaData) {
        return new Func1<PropertySet, PropertySet>() {
            @Override
            public PropertySet call(PropertySet propertySet) {
                return propertySet.merge(metaData);
            }
        };
    }

    private final Func1<PropertySet, Observable<Notification>> toNotification = new Func1<PropertySet, Observable<Notification>>() {
        @Override
        public Observable<Notification> call(final PropertySet propertySet) {
            final Notification notification = presenter.createNotification(propertySet);
            // TODO: put model creation back in presenter, that will fix the test back.
            if (presenter.artworkCapable()) {
                loadAndSetArtwork(propertySet.get(TrackProperty.URN), notification);
            }
            return Observable.just(notification);
        }
    };


    @Inject
    public PlaybackNotificationController(Resources resources, TrackOperations trackOperations, PlaybackNotificationPresenter presenter,
                                          NotificationManager notificationManager, EventBus eventBus, ImageOperations imageOperations,
                                          PlayQueueManager playQueueManager) {
        this.resources = resources;
        this.trackOperations = trackOperations;
        this.presenter = presenter;
        this.notificationManager = notificationManager;
        this.eventBus = eventBus;
        this.imageOperations = imageOperations;
        this.playQueueManager = playQueueManager;

        this.targetIconWidth = resources.getDimensionPixelSize(R.dimen.notification_image_large_width);
        this.targetIconHeight = resources.getDimensionPixelSize(R.dimen.notification_image_large_height);
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .mergeMap(onPlayQueueEventFunc).doOnNext(notifyAction).subscribe(new DefaultSubscriber<Notification>());

        eventBus.subscribe(EventQueue.PLAYER_LIFE_CYCLE, new DefaultSubscriber<PlayerLifeCycleEvent>() {
            @Override
            public void onNext(PlayerLifeCycleEvent args) {
                lastPlayerLifecycleEvent = args;
                if (!args.isServiceRunning()) {
                    notificationManager.cancel(PLAYBACKSERVICE_STATUS_ID);
                }
            }
        });
    }

    Observable<Notification> playingNotification() {
        return notificationObservable.map(presenter.updateToPlayingState());
    }

    boolean notifyIdleState() {
        return presenter.updateToIdleState(notificationObservable, notifyAction);
    }

    private ApiImageSize getApiImageSize() {
        return ApiImageSize.getListItemImageSize(resources);
    }

    private void loadAndSetArtwork(final TrackUrn trackUrn, final Notification notification) {
        final ApiImageSize apiImageSize = getApiImageSize();
        final Bitmap cachedBitmap = imageOperations.getCachedBitmap(trackUrn, apiImageSize, targetIconWidth, targetIconHeight);

        if (cachedBitmap != null) {
            presenter.setIcon(notification, cachedBitmap);
        } else {
            presenter.clearIcon(notification);

            imageSubscription = imageOperations.artwork(trackUrn, getApiImageSize(), targetIconWidth, targetIconHeight)
                    .subscribe(new DefaultSubscriber<Bitmap>() {
                @Override
                public void onNext(Bitmap bitmap) {
                    presenter.setIcon(notification, bitmap);
                    if (lastPlayerLifecycleEvent.isServiceRunning()){
                        notificationManager.notify(PLAYBACKSERVICE_STATUS_ID, notification);
                    }
                }
            });
        }
    }
}
