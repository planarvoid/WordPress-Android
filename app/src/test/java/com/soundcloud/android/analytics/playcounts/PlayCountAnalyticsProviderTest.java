package com.soundcloud.android.analytics.playcounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class PlayCountAnalyticsProviderTest extends AndroidUnitTest {

    private PlayCountAnalyticsProvider provider;

    @Mock EventTracker eventTracker;
    @Mock PlayCountUrlBuilder urlBuilder;
    @Mock FeatureFlags featureFlags;
    @Captor ArgumentCaptor<TrackingRecord> trackingEventCaptor;

    @Before
    public void setup() {
        provider = new PlayCountAnalyticsProvider(eventTracker, urlBuilder, featureFlags);
        when(featureFlags.isDisabled(Flag.EVENTLOGGER_AUDIO_V1)).thenReturn(true);
    }

    @Test
    public void shouldTrackFirstPlayEvent() {
        PlaybackSessionEvent sessionEvent = TestEvents.playbackSessionPlayEventWithProgress(0);
        when(urlBuilder.buildUrl(sessionEvent)).thenReturn("url");

        provider.handleTrackingEvent(sessionEvent);

        verify(eventTracker).trackEvent(trackingEventCaptor.capture());
        assertThat(trackingEventCaptor.getValue().getBackend()).isEqualTo(PlayCountAnalyticsProvider.BACKEND_NAME);
        assertThat(trackingEventCaptor.getValue().getTimeStamp()).isEqualTo(1000L);
        assertThat(trackingEventCaptor.getValue().getData()).isEqualTo("url");
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
        PlaybackSessionEvent sessionEvent = TestEvents.playbackSessionPlayEventWithProgress(PlaybackSessionEvent.FIRST_PLAY_MAX_PROGRESS + 1);
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