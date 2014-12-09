package com.soundcloud.android.playback.notification;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.views.NotificationPlaybackRemoteViews;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackNotificationPresenterTest {
    private static final String DEVICE_NAME = "device-name";
    private static final String CASTING_TO = "casting-to";
    private PlaybackNotificationPresenter presenter;
    private PropertySet trackProperties;

    @Mock private NotificationPlaybackRemoteViews.Factory factory;
    @Mock private NotificationBuilder notificationBuilder;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private Context context;

    @Before
    public void setUp() throws Exception {
        trackProperties = TestPropertySets.expectedTrackForPlayer();

        presenter = new PlaybackNotificationPresenter(context, castConnectionHelper);
    }

    @Test
    public void createTrackSeSetsUsernameAsTextOnNotificationBuilder() {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);

        verify(notificationBuilder).setHeader(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void updateToPlayingState() throws Exception {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);
        presenter.updateToPlayingState(notificationBuilder);

        verify(notificationBuilder).setPlayingStatus(true);
    }

    @Test
    public void updateToIdleState() throws Exception {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);
        presenter.updateToIdleState(notificationBuilder);

        verify(notificationBuilder).setPlayingStatus(false);
    }

    @Test
    public void updateToPlayingStateSetsCreatorNameIfNotCasting() throws Exception {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);
        presenter.updateToPlayingState(notificationBuilder);

        verify(notificationBuilder, times(2)).setHeader(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void updateToIdleStateSetsCreatorNameIfNotCasting() throws Exception {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);
        presenter.updateToIdleState(notificationBuilder);

        verify(notificationBuilder, times(2)).setHeader(trackProperties.get(PlayableProperty.CREATOR_NAME));
    }

    @Test
    public void createTrackSeSetsCastNameAsTextOnNotificationBuilder() {
        configureCastMode();
        presenter.updateTrackInfo(notificationBuilder, trackProperties);

        verify(notificationBuilder).setHeader(CASTING_TO);
    }

    @Test
    public void updateToPlayingStateSetsCastNameIfCasting() throws Exception {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);
        configureCastMode();
        presenter.updateToPlayingState(notificationBuilder);

        verify(notificationBuilder).setHeader(CASTING_TO);
    }

    @Test
    public void updateToIdleStateSetsCastNameIfCasting() throws Exception {
        presenter.updateTrackInfo(notificationBuilder, trackProperties);
        configureCastMode();
        presenter.updateToIdleState(notificationBuilder);

        verify(notificationBuilder).setHeader(CASTING_TO);
    }

    private void configureCastMode() {
        when(castConnectionHelper.isConnected()).thenReturn(true);
        when(castConnectionHelper.getCastingDeviceName()).thenReturn(DEVICE_NAME);
        when(context.getString(R.string.casting_to_device, DEVICE_NAME)).thenReturn(CASTING_TO);
    }

}