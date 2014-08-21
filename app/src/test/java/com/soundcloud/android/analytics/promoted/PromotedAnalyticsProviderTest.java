package com.soundcloud.android.analytics.promoted;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingEvent;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PromotedAnalyticsProviderTest {

    private PromotedAnalyticsProvider analyticsProvider;

    @Mock private EventTracker eventTracker;

    private UserUrn userUrn = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        analyticsProvider = new PromotedAnalyticsProvider(eventTracker);
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        PlaybackSessionEvent playbackEvent = mock(PlaybackSessionEvent.class);
        when(playbackEvent.isAd()).thenReturn(true);
        when(playbackEvent.isFirstPlay()).thenReturn(true);
        when(playbackEvent.getTimeStamp()).thenReturn(12345L);
        when(playbackEvent.getAudioAdImpressionUrls()).thenReturn(newArrayList("url1", "url2"));

        analyticsProvider.handlePlaybackSessionEvent(playbackEvent);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());
        final List<TrackingEvent> trackingEvents = captor.getAllValues();
        final TrackingEvent event1 = trackingEvents.get(0);
        final TrackingEvent event2 = trackingEvents.get(1);

        expect(event1.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event1.getTimeStamp()).toEqual(12345L);
        expect(event1.getUrl()).toEqual("url1");

        expect(event2.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event2.getTimeStamp()).toEqual(12345L);
        expect(event2.getUrl()).toEqual("url2");
    }

    @Test
    public void tracksAudioAdCompanionImpressions() {
        final PropertySet audioAdMetadata = TestPropertySets.audioAdProperties(TrackUrn.forTrack(999));
        AudioAdCompanionImpressionEvent impressionEvent = new AudioAdCompanionImpressionEvent(
                audioAdMetadata, TrackUrn.forTrack(888), UserUrn.forUser(777), 333
        );

        analyticsProvider.handleAudioAdCompanionImpression(impressionEvent);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingEvent event1 = captor.getAllValues().get(0);
        expect(event1.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event1.getTimeStamp()).toEqual(333l);
        expect(event1.getUrl()).toEqual("visualUrl");

        final TrackingEvent event2 = captor.getAllValues().get(1);
        expect(event2.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event2.getTimeStamp()).toEqual(333l);
        expect(event2.getUrl()).toEqual("visualUrl2");
    }


}