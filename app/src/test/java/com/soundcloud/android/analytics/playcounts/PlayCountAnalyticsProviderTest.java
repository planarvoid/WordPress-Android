package com.soundcloud.android.analytics.playcounts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.PropertySets;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
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
    @Captor ArgumentCaptor<TrackingEvent> trackingEventCaptor;

    @Before
    public void setup() {
        provider = new PlayCountAnalyticsProvider(eventTracker, urlBuilder);
    }

    @Test
    public void shouldTrackFirstPlayEvent() {
        final int progress = 0;
        PlaybackSessionEvent sessionEvent = PlaybackSessionEvent.forPlay(
                PropertySets.expectedTrackDataForAnalytics(Urn.forTrack(1L), "allow", 1000), Urn.forUser(1), null, progress, 1000L);
        when(urlBuilder.buildUrl(sessionEvent)).thenReturn("url");

        provider.handlePlaybackSessionEvent(sessionEvent);

        verify(eventTracker).trackEvent(trackingEventCaptor.capture());
        expect(trackingEventCaptor.getValue().getBackend()).toEqual(PlayCountAnalyticsProvider.BACKEND_NAME);
        expect(trackingEventCaptor.getValue().getTimeStamp()).toEqual(1000L);
        expect(trackingEventCaptor.getValue().getUrl()).toEqual("url");
    }

    @Test
    public void shouldNotTrackStopEventsAgainstPlayCounts() {
        final int progress = 0;
        final PropertySet trackData = PropertySets.expectedTrackDataForAnalytics(Urn.forTrack(1L), "allow", 1000);
        PlaybackSessionEvent previousPlayEvent = PlaybackSessionEvent.forPlay(trackData, Urn.forUser(1), null, progress, 1000L);
        PlaybackSessionEvent stopEvent = PlaybackSessionEvent.forStop(
                trackData, Urn.forUser(1), null, previousPlayEvent, PlaybackSessionEvent.STOP_REASON_BUFFERING, progress, 1000L);

        provider.handlePlaybackSessionEvent(stopEvent);

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void shouldImmediatelyRequestFlushForPlayEvents() {
        final int progress = 0;
        PlaybackSessionEvent sessionEvent = PlaybackSessionEvent.forPlay(
                PropertySets.expectedTrackDataForAnalytics(Urn.forTrack(1L), "allow", 1000), Urn.forUser(1), null, progress, 1000L);
        when(urlBuilder.buildUrl(sessionEvent)).thenReturn("url");

        provider.handlePlaybackSessionEvent(sessionEvent);

        verify(eventTracker).flush(PlayCountAnalyticsProvider.BACKEND_NAME);
    }

    @Test
    public void shouldNotTrackSubsequentPlayEvents() {
        final int progress = 1;
        PlaybackSessionEvent sessionEvent = PlaybackSessionEvent.forPlay(
                PropertySets.expectedTrackDataForAnalytics(Urn.forTrack(1L), "allow", 1000), Urn.forUser(1), null, progress, 1000L);
        when(urlBuilder.buildUrl(sessionEvent)).thenReturn("url");

        provider.handlePlaybackSessionEvent(sessionEvent);

        verify(eventTracker, never()).trackEvent(any(TrackingEvent.class));
    }

    @Test
    public void shouldForwardFlushEventToEventTracker() {
        provider.flush();

        verify(eventTracker).flush(PlayCountAnalyticsProvider.BACKEND_NAME);
    }
}