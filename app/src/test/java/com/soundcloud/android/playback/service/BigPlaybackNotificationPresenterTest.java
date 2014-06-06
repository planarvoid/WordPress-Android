package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.functions.Action1;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class BigPlaybackNotificationPresenterTest {

    private static final String PACKAGE_NAME = "package-name";

    private BigPlaybackNotificationPresenter presenter;
    private Track track;
    private Notification notification = new Notification();

    @Mock
    private NotificationPlaybackRemoteViews.Factory factory;
    @Mock
    private NotificationPlaybackRemoteViews bigRemoteViews;
    @Mock
    private NotificationCompat.Builder notificationBuilder;
    @Mock
    private Context context;

    @Before
    public void setUp() throws Exception {
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(factory.create(PACKAGE_NAME)).thenReturn(Mockito.mock(NotificationPlaybackRemoteViews.class));
        when(factory.create(PACKAGE_NAME, R.layout.playback_status_large_v16)).thenReturn(bigRemoteViews);
        when(notificationBuilder.build()).thenReturn(notification);

        track = TestHelper.getModelFactory().createModel(Track.class);
        presenter = new BigPlaybackNotificationPresenter(context, factory, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        });
    }

    @Test
    public void createTrackSetsRemoteViewsAsContentViewOnBuilder() throws Exception {
        presenter.createNotification(track);
        expect(notification.bigContentView).toBe(bigRemoteViews);
    }

    @Test
    public void createTrackSetCurrentTrackOnRemoteViews() throws Exception {
        presenter.createNotification(track);
        verify(bigRemoteViews).setCurrentTrackTitle(track.getTitle());
    }

    @Test
    public void createTrackSeCallsSetCurrentUserOnRemoteViews() throws Exception {
        presenter.createNotification(track);
        verify(bigRemoteViews).setCurrentUsername(track.getUsername());
    }

    @Test
    public void updateToPlayingStateFunctionUpdatesPlayingStateToTrueOnRemoteViews() throws Exception {
        notification.bigContentView = bigRemoteViews;
        notification.contentView = Mockito.mock(NotificationPlaybackRemoteViews.class);
        presenter.updateToPlayingState().call(notification);
        verify(bigRemoteViews).setPlaybackStatus(true);
    }

    @Test
    public void updateToIdleStateFunctionUpdatesPlayingStateToFalseOnRemoteViews() throws Exception {
        notification.bigContentView = bigRemoteViews;
        notification.contentView = Mockito.mock(NotificationPlaybackRemoteViews.class);
        presenter.updateToIdleState(Observable.just(notification), Mockito.mock(Action1.class));
        verify(bigRemoteViews).setPlaybackStatus(false);
    }

}