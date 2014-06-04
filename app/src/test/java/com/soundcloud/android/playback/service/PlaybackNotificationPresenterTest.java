package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action1;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackNotificationPresenterTest extends TestCase {

    private PlaybackNotificationPresenter presenter;

    @Mock
    private NotificationPlaybackRemoteViews.Factory factory;
    @Mock
    private NotificationCompat.Builder notificationBuilder;
    @Mock
    private Context context;
    @Mock
    private Notification notification;

    private Track track;

    @Before
    public void setUp() throws Exception {
        track = TestHelper.getModelFactory().createModel(Track.class);
        presenter = new PlaybackNotificationPresenter(context, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        });
    }

    @Test
    public void createTrackSeCallsSetTitleOnNotificationBuilder() throws Exception {
        presenter.createNotification(track);
        verify(notificationBuilder).setContentTitle(track.getTitle());
    }

    @Test
    public void createTrackSeSetsUsernameAsTextOnNotificationBuilder() throws Exception {
        presenter.createNotification(track);
        verify(notificationBuilder).setContentText(track.getUserName());
    }

    @Test
    public void createTrackSeReturnsNotificationFromBuilder() throws Exception {
        Notification notification = new Notification();
        when(notificationBuilder.build()).thenReturn(notification);
        expect(presenter.createNotification(track)).toBe(notification);
    }

    @Test
    public void updateToPlayingStateEmitsOriginalNotification() throws Exception {
        expect(presenter.updateToPlayingState().call(notification)).toBe(notification);
    }

    @Test
    public void updateToIdleStateReturnsFalse() throws Exception {
        expect(presenter.updateToIdleState(Observable.just(notification), mock(Action1.class))).toBeFalse();
    }
}