package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
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

    private PropertySet propertySet;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private Context context;
    @Mock
    private TrackOperations trackOperations;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private PlaybackNotificationPresenter playbackNotificationPresenter;
    @Mock
    private NotificationManager notificationManager;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private Notification notification;
    @Mock
    private Bitmap bitmap;
    @Mock
    private Uri uri;
    @Mock
    private Subscription subscription;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(TrackProperty.URN.bind(TRACK_URN));

        when(context.getResources()).thenReturn(Robolectric.getShadowApplication().getResources());
        when(playbackNotificationPresenter.createNotification(propertySet)).thenReturn(notification);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.just(propertySet));

        controller = new PlaybackNotificationController(context, trackOperations, playbackNotificationPresenter,
                notificationManager, eventBus, imageOperations);
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromNewPlayQueueEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(TRACK_URN));

        verify(notificationManager).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromTrackChangeEvent() {
        publishTrackChangedEvent();

        verify(notificationManager).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void playQueueEventDoesNotCheckBitmapCacheIfPresenterNotArtworkCapable() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(false);

        publishTrackChangedEvent();

        verify(imageOperations, never()).getCachedBitmap(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt());
    }

    @Test
    public void playQueueEventCreatesNewNotificationWithBitmapFromImageCache() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.getCachedBitmap(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(bitmap);
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        publishTrackChangedEvent();

        verify(playbackNotificationPresenter).setIcon(notification, uri);
        verify(imageOperations, never()).image(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    public void playQueueEventClearsExistingBitmapWhenArtworkCapableAndNoCachedBitmap() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);

        publishTrackChangedEvent();

        verify(playbackNotificationPresenter).clearIcon(notification);
    }

    @Test
    public void playQueueEventSetsLoadedBitmapWithPresenterWhenArtworkCapableAndNoCachedBitmap() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.image(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        publishTrackChangedEvent();

        verify(playbackNotificationPresenter).setIcon(notification, uri);
    }

    @Test
    public void playQueueEventNotifiesAgainAfterBitmapLoaded() {
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);
        when(imageOperations.image(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(Observable.just(bitmap));
        when(imageOperations.getLocalImageUri(eq(TRACK_URN), any(ApiImageSize.class))).thenReturn(uri);

        publishTrackChangedEvent();

        verify(notificationManager, times(2)).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void playQueueEventUnsubscribesExistingImageLoadingObservable() {
        Observable<Bitmap> imageObservable = TestObservables.endlessObservablefromSubscription(subscription);
        when(imageOperations.image(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt(), eq(false))).thenReturn(imageObservable, Observable.<Bitmap>never());
        when(playbackNotificationPresenter.artworkCapable()).thenReturn(true);

        publishTrackChangedEvent();
        // loading an image for a different track should cancel existing image tasks
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK_URN));

        verify(subscription).unsubscribe();
    }

    @Test
    public void playingNotificationEmitsExistingNotificationWithUpdatedPlayState() {
        publishTrackChangedEvent();

        when(playbackNotificationPresenter.updateToPlayingState()).thenReturn(Functions.<Notification>identity());
        expect(controller.playingNotification().toBlockingObservable().lastOrDefault(null)).toBe(notification);
    }

    @Test
    public void playingNotificationWithUpdatedPlayState() {
        publishTrackChangedEvent();

        when(playbackNotificationPresenter.updateToPlayingState()).thenReturn(Functions.<Notification>identity());
        controller.playingNotification().toBlockingObservable().lastOrDefault(null);
        verify(playbackNotificationPresenter).updateToPlayingState();
    }

    @Test
    public void notifyToIdleStateCallsUpdateToIdleStateOnPresenterWithExistingNotification() {
        publishTrackChangedEvent();

        controller.notifyIdleState();

        ArgumentCaptor<Observable> captor = ArgumentCaptor.forClass(Observable.class);
        verify(playbackNotificationPresenter).updateToIdleState(captor.capture(), any(Action1.class));
        expect(captor.getValue().toBlockingObservable().lastOrDefault(null)).toBe(notification);
    }

    @Test
    public void notifyToIdleStateUpdatesNotificationViaUpdateAction() {
        publishTrackChangedEvent();

        controller.notifyIdleState();

        ArgumentCaptor<Action1> captor = ArgumentCaptor.forClass(Action1.class);
        verify(playbackNotificationPresenter).updateToIdleState(any(Observable.class), captor.capture());
        captor.getValue().call(notification);

        // twice because the playqueue changed event will result in the first notification
        verify(notificationManager, times(2)).notify(PlaybackNotificationController.PLAYBACKSERVICE_STATUS_ID, notification);
    }

    @Test
    public void notifyToIdleStateReturnsPresenterUpdateIdleState() {
        publishTrackChangedEvent();
        when(playbackNotificationPresenter.updateToIdleState(any(Observable.class), any(Action1.class))).thenReturn(true);
        expect(controller.notifyIdleState()).toBeTrue();
    }

    private void publishTrackChangedEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(TRACK_URN));
    }
}