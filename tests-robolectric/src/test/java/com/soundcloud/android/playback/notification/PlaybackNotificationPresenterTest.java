package com.soundcloud.android.playback.notification;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackNotificationPresenterTest {
    private PlaybackNotificationPresenter presenter;
    private PropertySet trackProperties;

    @Mock private NotificationPlaybackRemoteViews.Factory factory;
    @Mock private NotificationBuilder notificationBuilder;
    @Mock private Context context;

    @Before
    public void setUp() throws Exception {
        trackProperties = TestPropertySets.expectedTrackForPlayer();

        presenter = new PlaybackNotificationPresenter(context);
    }

    @Test
    public void createTrackSeSetsUsernameAsTextOnNotificationBuilder() {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);
        verify(notificationBuilder).setCreatorName(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void updateToPlayingState() throws Exception {
        presenter.updateToPlayingState(notificationBuilder);

        verify(notificationBuilder).setPlayingStatus(true);
    }

    @Test
    public void updateToIdleState() throws Exception {
        presenter.updateToIdleState(notificationBuilder);

        verify(notificationBuilder).setPlayingStatus(false);
    }
}