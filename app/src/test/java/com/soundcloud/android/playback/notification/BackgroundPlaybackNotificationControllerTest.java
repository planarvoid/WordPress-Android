package com.soundcloud.android.playback.notification;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.audioAdProperties;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.playback.PlaybackStateProvider;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

public class BackgroundPlaybackNotificationControllerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private BackgroundPlaybackNotificationController controller;

    @Mock private Context context;
    @Mock private TrackRepository trackRepository;
    @Mock private ImageOperations imageOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlaybackNotificationPresenter playbackNotificationPresenter;
    @Mock private NotificationManager notificationManager;
    @Mock private NotificationBuilder notificationBuilder;
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

        controller = new BackgroundPlaybackNotificationController(
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

        controller.setTrack(playbackService, audioAdMetaDAta);

        verify(playbackNotificationPresenter).updateTrackInfo(notificationBuilder, expectedProperties);
    }

    @Test
    public void playQueueEventCreatesNewNotificationWhenTrackSet() {
        controller.setTrack(playbackService, trackProperties);

        verify(notificationManager).notify(eq(NotificationConstants.PLAYBACK_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void playQueueEventStartsForegroundWhenTrackSetAndPlaying() {
        when(playbackStateProvider.isSupposedToBePlaying()).thenReturn(true);

        controller.setTrack(playbackService, trackProperties);

        verify(playbackService).startForeground(eq(NotificationConstants.PLAYBACK_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void playQueueEventDoesNotCheckBitmapCacheIfPresenterNotArtworkCapable() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(false);

        controller.setTrack(playbackService, PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));

        verify(imageOperations, never()).getCachedBitmap(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt());
    }

    @Test
    public void playQueueEventSetsBitmapWhenArtworkCapableAndNoCachedBitmap() {
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);
        when(imageOperations.artwork(eq(TRACK_URN), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(Observable.just(bitmap));

        controller.setTrack(playbackService, PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));

        verify(notificationBuilder).setIcon(bitmap);
    }

    @Test
    public void playQueueEventUnsubscribesExistingImageLoadingObservable() {
        PublishSubject<Bitmap> imageObservable = PublishSubject.create();
        when(imageOperations.artwork(any(Urn.class), any(ApiImageSize.class), anyInt(), anyInt())).thenReturn(imageObservable, Observable.<Bitmap>never());
        when(notificationBuilder.hasArtworkSupport()).thenReturn(true);

        controller.setTrack(playbackService, PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));
        controller.setTrack(playbackService, PropertySet.from(EntityProperty.URN.bind(TRACK_URN)));

        assertThat(imageObservable.hasObservers()).isFalse();
    }

    @Test
    public void notifyToIdleStateCallsUpdateToIdleStateOnPresenter() {
        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);

        controller.setTrack(playbackService, trackProperties);
        controller.notifyIdleState(playbackService);

        verify(playbackNotificationPresenter).updateToIdleState(notificationBuilder);
    }

    @Test
    public void notifyToIdleStateStartsForegroundWithUpdatedNotificationViaUpdateAction() {
        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);

        controller.setTrack(playbackService, trackProperties);
        controller.notifyIdleState(playbackService);

        verify(playbackNotificationPresenter).updateToIdleState(notificationBuilder);
        // twice because the playqueue changed event will result in the first notification
        verify(playbackService).startForeground(eq(NotificationConstants.PLAYBACK_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void notifyToIdleStateReturnsPresenterUpdateIdleState() {
        controller.setTrack(playbackService, trackProperties);
        controller.notifyIdleState(playbackService);

        when(notificationBuilder.hasPlayStateSupport()).thenReturn(true);
        controller.notifyIdleState(playbackService);

        verify(playbackService).stopForeground(true);
    }

    @Test
    public void notifyPlayingUpdatesPlayState() {
        controller.setTrack(playbackService, trackProperties);

        controller.notifyPlaying(playbackService);

        verify(playbackNotificationPresenter).updateToPlayingState(notificationBuilder);
    }

    @Test
    public void notifyPlayingStartsForeground() {
        controller.setTrack(playbackService, trackProperties);

        controller.notifyPlaying(playbackService);

        verify(playbackService).startForeground(eq(NotificationConstants.PLAYBACK_NOTIFY_ID), any(Notification.class));
    }

    @Test
    public void notifyPlayingWithNoTrackSetDoesNothing() {
        controller.notifyPlaying(playbackService);

        verifyZeroInteractions(playbackService);
        verifyZeroInteractions(playbackNotificationPresenter);
    }

}
