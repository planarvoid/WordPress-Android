package com.soundcloud.android.analytics.localytics;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.localytics.LocalyticsAnalyticsProvider;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderTest {
    private LocalyticsAnalyticsProvider localyticsProvider;
    @Mock
    private LocalyticsSession localyticsSession;
    private Track track;
    private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp(){
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession);
    }

    @Test
    public void shouldOpenSession(){
        localyticsProvider.openSession();
        verify(localyticsSession).open();
    }

    @Test
    public void shouldUploadDataWhenClosingSession(){
        localyticsProvider.closeSession();
        verify(localyticsSession).upload();
    }

    @Test
    public void shouldCloseSession(){
        localyticsProvider.closeSession();
        verify(localyticsSession).close();
    }

    @Test
    public void shouldTrackScreenWithGivenName() {
        localyticsProvider.trackScreen("main:explore");
        verify(localyticsSession).tagScreen(eq("main:explore"));
    }

    @Test
    public void trackPlayEventTracksListenEventWithListenAction() throws CreateModelException {
        PlaybackEventData stop = getStopEventData();
        localyticsProvider.trackPlaybackEvent(stop);
        verify(localyticsSession).tagEvent(eq("Listen"));
    }

    private PlaybackEventData getStopEventData() throws CreateModelException {
        track = TestHelper.getModelFactory().createModel(Track.class);
        trackSourceInfo = new TrackSourceInfo(Screen.YOUR_LIKES.get(), true);
        PlaybackEventData play = PlaybackEventData.forPlay(track, 123L, trackSourceInfo);
        return PlaybackEventData.forStop(track, 123L, trackSourceInfo, play);
    }
}
