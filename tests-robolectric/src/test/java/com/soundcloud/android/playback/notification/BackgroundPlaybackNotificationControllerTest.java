package com.soundcloud.android.playback.notification;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.audioAdProperties;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.testsupport.InjectionSupport;
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

@RunWith(SoundCloudTestRunner.class)
public class BackgroundPlaybackNotificationControllerTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private BackgroundPlaybackNotificationController controller;

    @Mock private Context context;
    @Mock private TrackRepository trackRepository;
    @Mock private ImageOperations imageOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlaybackNotificationPresenter playbackNotificationPresenter;
    @Mock private NotificationManager notificationManager;
    @Mock private NotificationBuilder notificationBuilder;
    @Mock private Resources resources;
    @Mock private Bitmap bitmap;
    @Mock private Bitmap loadingBitmap;
    @Mock private Uri uri;
    @Mock private Subscription subscription;
    @Mock private PlaybackStateProvider playbackStateProvider;
    @Mock private PlaybackService playbackService;
    @Captor private ArgumentCaptor<PropertySet> propertySetCaptor;
    private PropertySet trackProperties;

    @Before
    public void setUp() throws Exception {
        trackProperties = expectedTrackForPlayer();
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(trackProperties));
        when(imageOperations.decodeResource(resources, R.drawable.notification_loading)).thenReturn(loadingBitmap);

        controller = new BackgroundPlaybackNotificationController(
                resources,
                trackRepository,
                playbackNotificationPresenter,
                notificationManager,
                imageOperations,
                InjectionSupport.providerOf(notificationBuilder),
                playbackStateProvider
        );
    }

    @Test
    public void mergeTrackAndAudioAdPropertiesWhenCurrentSoundIsAnAd() {
        final PropertySet audioAdMetaDAta = audioAdProperties(Urn.forTrack(123L)).put(EntityProperty.URN, Urn.forTrack(123L));
        final PropertySet expectedProperties = audioAdMetaDAta.merge(trackProperties);

        controller.setTrack(audioAdMetaDAta);

        verify(playbackNotificationPresenter).updateTrackInfo(notificationBuilder, expectedProperties);
    }

    @Test
    public void playQueueEventCreatesNewNotificationWhenTrackSet() {
        controller.setTrack(trackProperties);

        verify(notificationManager).notify(eq(NotificationConstants.PLAYBACK_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotCheckBitmapCacheIfPresenterNotArtworkCapable() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(false);

        controller.setTrack(PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));

        verify(imageOperations, never()).getCachedBitmap(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt());
    }

    @Test
    public void playQueueEventSetsDefaultBitmapWhenArtworkCapableAndNoCachedBitmap() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.artwork(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));

        controller.setTrack(PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));

        verify(notificationBuilder).setIcon(loadingBitmap);
    }

    @Test
    public void playQueueEventUnsubscribesExistingImageLoadingObservable() {
        Observable<Bitmap> imageObservable = TestObservables.endlessObservablefromSubscription(subscription);
        when(imageOperations.artwork(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(imageObservable, Observable.<Bitmap>never());
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);

        controller.setTrack(PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));
        controller.setTrack(PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));

        verify(subscription).unsubscribe();
    }

    @Test
    public void notifyToIdleStateCallsUpdateToIdleStateOnPresenter() {
        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);

        controller.setTrack(trackProperties);
        controller.notifyIdleState(playbackService);

        verify(playbackNotificationPresenter).updateToIdleState(notificationBuilder);
    }

    @Test
    public void notifyToIdleStateUpdatesNotificationViaUpdateAction() {
        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);

        controller.setTrack(trackProperties);
        controller.notifyIdleState(playbackService);

        verify(playbackNotificationPresenter).updateToIdleState(notificationBuilder);
        // twice because the playqueue changed event will result in the first notification
        verify(notificationManager, times(2)).notify(eq(NotificationConstants.PLAYBACK_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void notifyToIdleStateReturnsPresenterUpdateIdleState() {
        controller.setTrack(trackProperties);
        controller.notifyIdleState(playbackService);

        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);
        controller.notifyIdleState(playbackService);

        verify(playbackService).stopForeground(true);
    }

}
