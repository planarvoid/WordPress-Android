package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.TestPropertySets.expectedTrackForPlayer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.functions.Action1;

import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class RichNotificationPresenterTest {

    private static final String PACKAGE_NAME = "package-name";

    private RichNotificationPresenter presenter;
    private PropertySet trackProperties;
    private Notification notification = new Notification();

    @Mock private NotificationPlaybackRemoteViews.Factory factory;
    @Mock private NotificationPlaybackRemoteViews remoteViews;
    @Mock private NotificationCompat.Builder notificationBuilder;
    @Mock private Context context;
    @Mock private Uri uri;

    @Before
    public void setUp() throws Exception {
        trackProperties = expectedTrackForPlayer();
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(factory.create(PACKAGE_NAME)).thenReturn(remoteViews);
        when(notificationBuilder.build()).thenReturn(notification);

        presenter = new RichNotificationPresenter(context, factory, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        });
    }

    @Test
    public void createTrackSetsRemoteViewsAsContentViewOnBuilder() throws Exception {
        presenter.createNotification(trackProperties);
        expect(notification.contentView).toBe(remoteViews);
    }


    @Test
    public void createTrackSetCurrentTrackOnRemoteViews() throws Exception {
        presenter.createNotification(trackProperties);
        verify(remoteViews).setCurrentTrackTitle(trackProperties.get(PlayableProperty.TITLE));
    }

    @Test
    public void createTrackSeCallsSetCurrentUserOnRemoteViews() throws Exception {
        presenter.createNotification(trackProperties);
        verify(remoteViews).setCurrentUsername(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void setIconSetsIconOnRemoteViews() throws Exception {
        notification.contentView = remoteViews;
        presenter.setIcon(notification, uri);
        verify(remoteViews).setIcon(uri);
    }

    @Test
    public void clearIconClearsIconOnRemoteViews() throws Exception {
        notification.contentView = remoteViews;
        presenter.clearIcon(notification);
        verify(remoteViews).clearIcon();
    }

    @Test
    public void updateToPlayingStateFunctionUpdatesPlayingStateToTrueOnRemoteViews() throws Exception {
        notification.contentView = remoteViews;
        presenter.updateToPlayingState().call(notification);
        verify(remoteViews).setPlaybackStatus(true);
    }

    @Test
    public void updateToPlayingStateFunctionEmitsSourceNotification() throws Exception {
        notification.contentView = remoteViews;
        expect(presenter.updateToPlayingState().call(notification)).toBe(notification);
    }

    @Test
    public void updateToIdleStateFunctionUpdatesPlayingStateToFalseOnRemoteViews() throws Exception {
        notification.contentView = remoteViews;
        presenter.updateToIdleState(Observable.just(notification), Mockito.mock(Action1.class));
        verify(remoteViews).setPlaybackStatus(false);
    }

    @Test
    public void updateToIdleStateFunctionEmotsNotificationToNotifyFunction() throws Exception {
        notification.contentView = remoteViews;
        final Action1<Notification> notifyAction = Mockito.mock(Action1.class);
        presenter.updateToIdleState(Observable.just(notification), notifyAction);
        verify(notifyAction).call(notification);
    }
}