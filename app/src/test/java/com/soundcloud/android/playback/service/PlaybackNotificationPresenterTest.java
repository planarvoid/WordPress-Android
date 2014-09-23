package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackNotificationPresenterTest {
    private PlaybackNotificationPresenter presenter;
    private PropertySet trackProperties;

    @Mock private NotificationPlaybackRemoteViews.Factory factory;
    @Mock private NotificationCompat.Builder notificationBuilder;
    @Mock private Context context;
    @Mock private Notification notification;

    @Before
    public void setUp() throws Exception {
        trackProperties = TestPropertySets.expectedTrackForPlayer();

        presenter = new PlaybackNotificationPresenter(context, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        });
    }

    @Test
    public void createTrackSeCallsSetTitleOnNotificationBuilder() {
        presenter.createNotification(trackProperties);
        verify(notificationBuilder).setContentTitle(trackProperties.get(PlayableProperty.TITLE));
    }

    @Test
    public void createTrackSeSetsUsernameAsTextOnNotificationBuilder() {
        presenter.createNotification(trackProperties);
        verify(notificationBuilder).setContentText(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void createTrackSeReturnsNotificationFromBuilder() {
        Notification notification = new Notification();
        when(notificationBuilder.build()).thenReturn(notification);
        expect(presenter.createNotification(trackProperties)).toBe(notification);
    }

    @Test
    public void updateToPlayingStateEmitsOriginalNotification() {
        expect(presenter.updateToPlayingState().call(notification)).toBe(notification);
    }

    @Test
    public void updateToIdleStateReturnsFalse() {
        expect(presenter.updateToIdleState(Observable.just(notification), new TestSubscriber<Notification>())).toBeFalse();
    }
}