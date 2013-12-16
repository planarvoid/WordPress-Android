package com.soundcloud.android.analytics.localytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderTest {
    private LocalyticsAnalyticsProvider localyticsProvider;
    @Mock
    private LocalyticsSession localyticsSession;
    @Captor
    private ArgumentCaptor<Map<String, String>> stopEventAttributes;

    private Track track;
    private TrackSourceInfo trackSourceInfo;
    private PlaybackEventData startEvent, stopEvent;

    @Before
    public void setUp() throws CreateModelException {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession);
        track = TestHelper.getModelFactory().createModel(Track.class);
        trackSourceInfo = new TrackSourceInfo(Screen.YOUR_LIKES.get(), true);

        track.genre = "clown step";

        long startTime = System.currentTimeMillis();
        long stopTime = startTime + 1000L;

        startEvent = PlaybackEventData.forPlay(track, 123L, trackSourceInfo, startTime);
        stopEvent = PlaybackEventData.forStop(track, 123L, trackSourceInfo, startEvent, stopTime);
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
    public void shouldNotTrackPlaybackStartEvents() throws CreateModelException {
        localyticsProvider.trackPlaybackEvent(startEvent);
        verifyZeroInteractions(localyticsSession);
    }

    @Test
    public void playbackEventDataForStopEventShouldContainBasicAttributes() throws CreateModelException {
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq("Listen"), stopEventAttributes.capture());

        expect(stopEventAttributes.getValue().get("context")).toEqual(Screen.YOUR_LIKES.get());
        expect(stopEventAttributes.getValue().get("duration")).toEqual("1000");
        expect(stopEventAttributes.getValue().get("track_length_ms")).toEqual(String.valueOf(track.duration));
        expect(stopEventAttributes.getValue().get("track_id")).toEqual(String.valueOf(track.getId()));
        expect(stopEventAttributes.getValue().get("tag")).toEqual(String.valueOf(track.getGenreOrTag()));
    }
}
