package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.expectedTrackForPlayer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.observers.Subscribers;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class BigPlaybackNotificationPresenterTest {

    private static final String PACKAGE_NAME = "package-name";

    private BigPlaybackNotificationPresenter presenter;
    private Notification notification = new Notification();

    @Mock private NotificationPlaybackRemoteViews.Factory factory;
    @Mock private NotificationPlaybackRemoteViews bigRemoteViews;
    @Mock private NotificationCompat.Builder notificationBuilder;
    @Mock private Context context;

    @Before
    public void setUp() throws Exception {
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(factory.create(PACKAGE_NAME)).thenReturn(Mockito.mock(NotificationPlaybackRemoteViews.class));
        when(factory.create(PACKAGE_NAME, R.layout.playback_status_large_v16)).thenReturn(bigRemoteViews);
        when(notificationBuilder.build()).thenReturn(notification);

        presenter = new BigPlaybackNotificationPresenter(context, factory, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        });
    }

    @Test
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void createTrackSetsRemoteViewsAsContentViewOnBuilder() {
        presenter.createNotification(expectedTrackForPlayer());

        expect(notification.bigContentView).toBe(bigRemoteViews);
    }

    @Test
    public void createTrackSetCurrentTrackOnRemoteViews() {
        presenter.createNotification(expectedTrackForPlayer());

        verify(bigRemoteViews).setCurrentTrackTitle(expectedTrackForPlayer().get(PlayableProperty.TITLE));
    }

    @Test
    public void createTrackSeCallsSetCurrentUserOnRemoteViews() {
        presenter.createNotification(expectedTrackForPlayer());

        verify(bigRemoteViews).setCurrentUsername(expectedTrackForPlayer().get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void updateToPlayingStateFunctionUpdatesPlayingStateToTrueOnRemoteViews() {
        notification.bigContentView = bigRemoteViews;
        notification.contentView = Mockito.mock(NotificationPlaybackRemoteViews.class);
        presenter.updateToPlayingState().call(notification);

        verify(bigRemoteViews).setPlaybackStatus(true);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void updateToIdleStateFunctionUpdatesPlayingStateToFalseOnRemoteViews() {
        notification.bigContentView = bigRemoteViews;
        notification.contentView = Mockito.mock(NotificationPlaybackRemoteViews.class);
        presenter.updateToIdleState(Observable.just(notification), Subscribers.<Notification>empty());

        verify(bigRemoteViews).setPlaybackStatus(false);
    }
}