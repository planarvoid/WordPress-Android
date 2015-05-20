package com.soundcloud.android.playback.notification;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.audioAdProperties;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackNotificationControllerTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private PlaybackNotificationController controller;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private Context context;
    @Mock private TrackRepository trackRepository;
    @Mock private ImageOperations imageOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlaybackNotificationPresenter playbackNotificationPresenter;
    @Mock private NotificationManager notificationManager;
    @Mock private NotificationBuilder notificationBuilder;
    @Mock private Bitmap bitmap;
    @Mock private Uri uri;
    @Mock private Subscription subscription;
    @Mock private PlaybackStateProvider playbackStateProvider;
    @Mock private Resources resources;
    @Captor private ArgumentCaptor<PropertySet> propertySetCaptor;
    private PropertySet trackProperties;

    @Before
    public void setUp() throws Exception {
        trackProperties = expectedTrackForPlayer();
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(trackProperties));

        controller = new PlaybackNotificationController(
                resources,
                trackRepository,
                playbackNotificationPresenter,
                notificationManager,
                eventBus,
                imageOperations,
                new Provider<NotificationBuilder>() {
                    @Override
                    public NotificationBuilder get() {
                        return notificationBuilder;
                    }
                },
                playbackStateProvider
        );
    }

    @Test
    public void playQueueEventDoesNotCreateNotificationBeforePlaybackServiceCreated() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotCreateNotificationAfterPlaybackServiceDestroyed() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
    }

    @Test
    public void playQueueEventUpdatesNotificationAfterPlaybackServiceWasStoppedAndStartedAgain() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStopped());
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager).notify(eq(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID), any(Notification.class));
    }

    @Test
    public void serviceDestroyedEventCancelsAnyCurrentNotification() {
        controller.subscribe();

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        verify(notificationManager).cancel(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID);
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromNewPlayQueueEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(notificationManager).notify(eq(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID), any(Notification.class));
    }


    @Test
    public void shouldMergeTrackAndAudioAdPropertiesWhenCurrentSoundIsAnAd() {
        final PropertySet audioAdMetaDAta = audioAdProperties(Urn.forTrack(123L));
        final PropertySet expectedProperties = audioAdMetaDAta.merge(trackProperties);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN, audioAdMetaDAta));

        verify(playbackNotificationPresenter).updateTrackInfo(notificationBuilder, expectedProperties);
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromTrackChangeEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager).notify(eq(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotCreateNewNotificationFromQueueUpdateEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(notificationManager, never()).notify(eq(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotCheckBitmapCacheIfPresenterNotArtworkCapable() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(false);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(imageOperations, never()).getCachedBitmap(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt());
    }

    @Test
    public void playQueueEventCreatesNewNotificationWithBitmapFromImageCache() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.getCachedBitmap(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(bitmap);
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationBuilder).setIcon(bitmap);
        verify(imageOperations, never()).artwork(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt());
    }

    @Test
    public void playQueueEventClearsExistingBitmapWhenArtworkCapableAndNoCachedBitmap() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.artwork(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationBuilder).clearIcon();
    }

    @Test
    public void playQueueEventSetsLoadedBitmapWithPresenterWhenArtworkCapableAndNoCachedBitmap() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.artwork(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationBuilder).setIcon(bitmap);
    }

    @Test
    public void playQueueEventNotifiesAgainAfterBitmapLoaded() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.artwork(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager, times(2)).notify(eq(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotifiesAgainAfterBitmapLoadedIfServiceNotCreated() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.artwork(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotifiesAgainAfterBitmapLoadedIfServiceDestroyed() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.artwork(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
    }

    @Test
    public void playQueueEventUnsubscribesExistingImageLoadingObservable() {
        Observable<Bitmap> imageObservable = TestObservables.endlessObservablefromSubscription(subscription);
        when(imageOperations.artwork(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(imageObservable, Observable.<Bitmap>never());
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));
        // loading an image for a different track should cancel existing image tasks
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(subscription).unsubscribe();
    }

    @Test
    public void notifyToIdleStateCallsUpdateToIdleStateOnPresenter() {
        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        controller.notifyIdleState();

        verify(playbackNotificationPresenter).updateToIdleState(notificationBuilder);
    }

    @Test
    public void notifyToIdleStateUpdatesNotificationViaUpdateAction() {
        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        controller.notifyIdleState();

        verify(playbackNotificationPresenter).updateToIdleState(notificationBuilder);

        // twice because the playqueue changed event will result in the first notification
        verify(notificationManager, times(2)).notify(eq(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID), any(Notification.class));
    }

    @Test
    public void notifyToIdleStateReturnsPresenterUpdateIdleState() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);
        expect(controller.notifyIdleState()).toBeTrue();
    }

}
