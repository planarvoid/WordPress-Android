package com.soundcloud.android.playback.notification;

import static com.soundcloud.android.playback.notification.NotificationBuilder.NOT_SET;

import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.PropertySetFunctions;
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
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class PlaybackNotificationController {

    public static final int PLAYBACKSERVICE_STATUS_ID = 1;

    private final Resources resources;
    private final PlaybackNotificationPresenter presenter;
    private final TrackRepository trackRepository;
    private final NotificationManager notificationManager;
    private final EventBus eventBus;
    private final ImageOperations imageOperations;
    private final Provider<NotificationBuilder> builderProvider;
    private final PlaybackStateProvider playbackStateProvider;

    private final Action1<CurrentPlayQueueTrackEvent> createNotificationBuilder = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
            createNotificationBuilder();
        }
    };
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
    /**
     * NOTE : this requires this class to be instantiated before the playback service or it will not receive the first
     * * One way to fix this would be to make the queue a replay queue, but its currently not necessary
     */
    private PlayerLifeCycleEvent lastPlayerLifecycleEvent = PlayerLifeCycleEvent.forDestroyed();
    private final Func1<CurrentPlayQueueTrackEvent, Observable<NotificationBuilder>> onPlayQueueEventFunc = new Func1<CurrentPlayQueueTrackEvent, Observable<NotificationBuilder>>() {
        @Override
        public Observable<NotificationBuilder> call(CurrentPlayQueueTrackEvent playQueueEvent) {
            imageSubscription.unsubscribe();
            return trackRepository
                    .track(playQueueEvent.getCurrentTrackUrn()).observeOn(AndroidSchedulers.mainThread())
                    .map(PropertySetFunctions.mergeInto(playQueueEvent.getCurrentMetaData()))
                    .flatMap(toNotification).cache();
        }
    };
    private Subscription imageSubscription = Subscriptions.empty();
    private NotificationBuilder notificationBuilder;

    @Inject
    public PlaybackNotificationController(Resources resources, TrackRepository trackRepository, PlaybackNotificationPresenter presenter,
                                          NotificationManager notificationManager, EventBus eventBus, ImageOperations imageOperations,
                                          Provider<NotificationBuilder> builderProvider, PlaybackStateProvider playbackStateProvider) {
        this.resources = resources;
        this.trackRepository = trackRepository;
        this.presenter = presenter;
        this.notificationManager = notificationManager;
        this.eventBus = eventBus;
        this.imageOperations = imageOperations;
        this.builderProvider = builderProvider;
        this.playbackStateProvider = playbackStateProvider;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK)
                .doOnNext(createNotificationBuilder)
                .flatMap(onPlayQueueEventFunc)
                .subscribe(new PlaylistSubscriber());

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

    private void loadAndSetArtwork(final Urn trackUrn, final NotificationBuilder notificationBuilder) {
        final Bitmap cachedBitmap = getCachedBitmap(trackUrn, notificationBuilder);
        if (cachedBitmap != null) {
            notificationBuilder.setIcon(cachedBitmap);
        } else {
            notificationBuilder.setIcon(imageOperations.decodeResource(resources, R.drawable.notification_loading));
            imageSubscription = getBitmap(trackUrn, notificationBuilder)
                    .subscribe(new DefaultSubscriber<Bitmap>() {
                        @Override
                        public void onNext(Bitmap bitmap) {
                            notificationBuilder.setIcon(bitmap);
                            if (lastPlayerLifecycleEvent.isServiceRunning()) {
                                notificationManager.notify(PLAYBACKSERVICE_STATUS_ID, notificationBuilder.build());
                            }
                        }
                    });
        }
    }

    private Bitmap getCachedBitmap(final Urn trackUrn, final NotificationBuilder notificationBuilder) {
        if (notificationBuilder.getTargetImageSize() == NOT_SET) {
            return imageOperations.getCachedBitmap(trackUrn, notificationBuilder.getImageSize());
        } else {
            return imageOperations.getCachedBitmap(trackUrn, notificationBuilder.getImageSize(), notificationBuilder.getTargetImageSize(), notificationBuilder.getTargetImageSize());
        }
    }

    private Observable<Bitmap> getBitmap(final Urn trackUrn, final NotificationBuilder notificationBuilder) {
        return imageOperations.artwork(trackUrn, notificationBuilder.getImageSize(), notificationBuilder.getTargetImageSize(), notificationBuilder.getTargetImageSize());
    }

    void createNotificationBuilder() {
        notificationBuilder = builderProvider.get();
        presenter.init(notificationBuilder, playbackStateProvider.isSupposedToBePlaying());
    }

    public Notification playingNotification() {
        presenter.updateToPlayingState(notificationBuilder);
        return notificationBuilder.build();
    }

    public boolean notifyIdleState() {
        if (notificationBuilder.hasPlayStateSupport()) {
            presenter.updateToIdleState(notificationBuilder);
            notificationManager.notify(PLAYBACKSERVICE_STATUS_ID, notificationBuilder.build());
            return true;
        }
        return false;
    }

    private class PlaylistSubscriber extends DefaultSubscriber<NotificationBuilder> {
        @Override
        public void onNext(NotificationBuilder notification) {
            if (lastPlayerLifecycleEvent.isServiceRunning()) {
                notificationBuilder = notification;
                notificationManager.notify(PLAYBACKSERVICE_STATUS_ID, notificationBuilder.build());
            }
        }
    }
}
