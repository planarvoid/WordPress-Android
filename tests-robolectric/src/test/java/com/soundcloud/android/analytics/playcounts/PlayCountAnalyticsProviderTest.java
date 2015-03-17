package com.soundcloud.android.analytics.playcounts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlayCountAnalyticsProviderTest {

    private PlayCountAnalyticsProvider provider;

    @Mock EventTracker eventTracker;
    @Mock PlayCountUrlBuilder urlBuilder;
    @Captor ArgumentCaptor<TrackingRecord> trackingEventCaptor;

    @Before
    public void setup() {
        provider = new PlayCountAnalyticsProvider(eventTracker, urlBuilder);
    }

    @Test
    public void shouldTrackFirstPlayEvent() {
        PlaybackSessionEvent sessionEvent = TestEvents.playbackSessionPlayEventWithProgress(0);
        when(urlBuilder.buildUrl(sessionEvent)).thenReturn("url");

        provider.handleTrackingEvent(sessionEvent);

        verify(eventTracker).trackEvent(trackingEventCaptor.capture());
        expect(trackingEventCaptor.getValue().getBackend()).toEqual(PlayCountAnalyticsProvider.BACKEND_NAME);
        expect(trackingEventCaptor.getValue().getTimeStamp()).toEqual(1000L);
        expect(trackingEventCaptor.getValue().getData()).toEqual("url");
    }

    @Test
    public void shouldNotTrackStopEventsAgainstPlayCounts() throws CreateModelException {
        PlaybackSessionEvent stopEvent = TestEvents.playbackSessionStopEvent();

        provider.handleTrackingEvent(stopEvent);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void shouldImmediatelyRequestFlushForPlayEvents() {
        PlaybackSessionEvent sessionEvent = TestEvents.playbackSessionPlayEventWithProgress(0);
        when(urlBuilder.buildUrl(sessionEvent)).thenReturn("url");

        provider.handleTrackingEvent(sessionEvent);

        verify(eventTracker).flush(PlayCountAnalyticsProvider.BACKEND_NAME);
    }

    @Test
    public void shouldNotTrackSubsequentPlayEvents() {
        PlaybackSessionEvent sessionEvent = TestEvents.playbackSessionPlayEventWithProgress(1);
        when(urlBuilder.buildUrl(sessionEvent)).thenReturn("url");

        provider.handleTrackingEvent(sessionEvent);

        verify(eventTracker, never()).trackEvent(any(TrackingRecord.class));
    }

    @Test
    public void shouldForwardFlushEventToEventTracker() {
        provider.flush();

        verify(eventTracker).flush(PlayCountAnalyticsProvider.BACKEND_NAME);
    }
}