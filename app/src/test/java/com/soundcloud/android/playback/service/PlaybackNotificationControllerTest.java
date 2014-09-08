package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.TestPropertySets.audioAdProperties;
import static com.soundcloud.android.TestPropertySets.expectedTrackForPlayer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Functions;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackNotificationControllerTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private PlaybackNotificationController controller;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private Context context;
    @Mock private TrackOperations trackOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlaybackNotificationPresenter playbackNotificationPresenter;
    @Mock private NotificationManager notificationManager;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private Notification notification;
    @Mock private Bitmap bitmap;
    @Mock private Uri uri;
    @Mock private Subscription subscription;
    @Captor private ArgumentCaptor<PropertySet> propertySetCaptor;
    private PropertySet trackProperties;

    @Before
    public void setUp() throws Exception {
        trackProperties = expectedTrackForPlayer();
        when(playbackNotificationPresenter.createNotification(trackProperties)).thenReturn(notification);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.just(trackProperties));

        controller = new PlaybackNotificationController(
                Robolectric.application.getResources(),
                trackOperations,
                playbackNotificationPresenter,
                notificationManager,
                eventBus,
                imageOperations,
                playQueueManager);
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

        verify(notificationManager).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }


    @Test
    public void shouldMergeTrackAndAudioAdPropertiesWhenCurrentSoundIsAnAd() {
        final PropertySet audioAdMetaDAta = audioAdProperties(Urn.forTrack(123L));
        final PropertySet expectedProperties = audioAdMetaDAta.merge(trackProperties);

        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getAudioAd()).thenReturn(audioAdMetaDAta);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(playbackNotificationPresenter).createNotification(eq(expectedProperties));
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromTrackChangeEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void playQueueEventDoesNotCreateNewNotificationFromQueueUpdateEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(notificationManager, never()).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void playQueueEventDoesNotCheckBitmapCacheIfPresenterNotArtworkCapable() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(false);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(imageOperations, never()).getCachedBitmap(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt());
    }

    @Test
    public void playQueueEventCreatesNewNotificationWithBitmapFromImageCache() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.getCachedBitmap(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(bitmap);
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(playbackNotificationPresenter).setIcon(notification, uri);
        verify(imageOperations, never()).image(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    public void playQueueEventClearsExistingBitmapWhenArtworkCapableAndNoCachedBitmap() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.image(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(Observable.just(bitmap));

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(playbackNotificationPresenter).clearIcon(notification);
    }

    @Test
    public void playQueueEventSetsLoadedBitmapWithPresenterWhenArtworkCapableAndNoCachedBitmap() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.image(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(playbackNotificationPresenter).setIcon(notification, uri);
    }

    @Test
    public void playQueueEventNotifiesAgainAfterBitmapLoaded() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.image(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager, times(2)).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void playQueueEventDoesNotifiesAgainAfterBitmapLoadedIfServiceNotCreated() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.image(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(notificationManager, never()).notify(anyInt(), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotifiesAgainAfterBitmapLoadedIfServiceDestroyed() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.image(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(Observable.just(bitmap));
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
        when(imageOperations.image(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(imageObservable, Observable.<Bitmap>never());
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));
        // loading an image for a different track should cancel existing image tasks
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(subscription).unsubscribe();
    }

    @Test
    public void playingNotificationEmitsExistingNotificationWithUpdatedPlayState() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        when(playbackNotificationPresenter.updateToPlayingState()).thenReturn(Functions.<Notification>identity());
        expect(controller.playingNotification().toBlockingObservable().lastOrDefault(null)).toBe(notification);
    }

    @Test
    public void playingNotificationWithUpdatedPlayState() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        when(playbackNotificationPresenter.updateToPlayingState()).thenReturn(Functions.<Notification>identity());
        controller.playingNotification().toBlockingObservable().lastOrDefault(null);
        verify(playbackNotificationPresenter).updateToPlayingState();
    }

    @Test
    public void notifyToIdleStateCallsUpdateToIdleStateOnPresenterWithExistingNotification() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        controller.notifyIdleState();

        ArgumentCaptor<Observable> captor = ArgumentCaptor.forClass(Observable.class);
        verify(playbackNotificationPresenter).updateToIdleState(captor.capture(), any(Subscriber.class));
        expect(captor.getValue().toBlockingObservable().lastOrDefault(null)).toBe(notification);
    }

    @Test
    public void notifyToIdleStateUpdatesNotificationViaUpdateAction() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        controller.notifyIdleState();

        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        verify(playbackNotificationPresenter).updateToIdleState(any(Observable.class), captor.capture());
        captor.getValue().onNext(notification);

        // twice because the playqueue changed event will result in the first notification
        verify(notificationManager, times(2)).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void notifyToIdleStateReturnsPresenterUpdateIdleState() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));
        when(playbackNotificationPresenter.updateToIdleState(any(Observable.class), any(Subscriber.class))).thenReturn(true);
        expect(controller.notifyIdleState()).toBeTrue();
    }

}