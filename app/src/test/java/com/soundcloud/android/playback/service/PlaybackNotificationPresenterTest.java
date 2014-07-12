package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
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

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private static final String TITLE = "TITLE";
    private static final String CREATOR = "CREATOR";

    private PlaybackNotificationPresenter presenter;
    private PropertySet propertySet;

    @Mock
    private NotificationPlaybackRemoteViews.Factory factory;
    @Mock
    private NotificationCompat.Builder notificationBuilder;
    @Mock
    private Context context;
    @Mock
    private Notification notification;

    @Before
    public void setUp() throws Exception {
        propertySet = PropertySet.from(
                TrackProperty.URN.bind(TRACK_URN),
                PlayableProperty.TITLE.bind(TITLE),
                PlayableProperty.CREATOR_NAME.bind(CREATOR));

        presenter = new PlaybackNotificationPresenter(context, new Provider<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return notificationBuilder;
            }
        });
    }

    @Test
    public void createTrackSeCallsSetTitleOnNotificationBuilder() throws Exception {
        presenter.createNotification(propertySet);
        verify(notificationBuilder).setContentTitle(TITLE);
    }

    @Test
    public void createTrackSeSetsUsernameAsTextOnNotificationBuilder() throws Exception {
        presenter.createNotification(propertySet);
        verify(notificationBuilder).setContentText(CREATOR);
    }

    @Test
    public void createTrackSeReturnsNotificationFromBuilder() throws Exception {
        Notification notification = new Notification();
        when(notificationBuilder.build()).thenReturn(notification);
        expect(presenter.createNotification(propertySet)).toBe(notification);
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