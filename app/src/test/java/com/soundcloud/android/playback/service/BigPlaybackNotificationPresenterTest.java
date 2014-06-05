package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private static final String TITLE = "TITLE";
    private static final String CREATOR = "CREATOR";

    private BigPlaybackNotificationPresenter presenter;
    private Notification notification = new Notification();
    private PropertySet propertySet;

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
        propertySet = PropertySet.from(
                TrackProperty.URN.bind(TRACK_URN),
                PlayableProperty.TITLE.bind(TITLE),
                PlayableProperty.CREATOR.bind(CREATOR));

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
    public void createTrackSetsRemoteViewsAsContentViewOnBuilder() throws Exception {
        presenter.createNotification(propertySet);
        expect(notification.bigContentView).toBe(bigRemoteViews);
    }

    @Test
    public void createTrackSetCurrentTrackOnRemoteViews() throws Exception {
        presenter.createNotification(propertySet);
        verify(bigRemoteViews).setCurrentTrackTitle(TITLE);
    }

    @Test
    public void createTrackSeCallsSetCurrentUserOnRemoteViews() throws Exception {
        presenter.createNotification(propertySet);
        verify(bigRemoteViews).setCurrentUsername(CREATOR);
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