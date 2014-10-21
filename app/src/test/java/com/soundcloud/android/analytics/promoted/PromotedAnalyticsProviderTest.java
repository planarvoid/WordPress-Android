package com.soundcloud.android.analytics.promoted;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.LeaveBehindTrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
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
    private TrackSourceInfo trackSourceInfo;

    private Urn userUrn = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        analyticsProvider = new PromotedAnalyticsProvider(eventTracker);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
    }

    @Test
    public void shouldTrackPlaybackEventAsEventLoggerEvent() throws Exception {
        PlaybackSessionEvent playbackEvent = mock(PlaybackSessionEvent.class);
        when(playbackEvent.isAd()).thenReturn(true);
        when(playbackEvent.isFirstPlay()).thenReturn(true);
        when(playbackEvent.getTimeStamp()).thenReturn(12345L);
        when(playbackEvent.getAudioAdImpressionUrls()).thenReturn(newArrayList("url1", "url2"));

        analyticsProvider.handleTrackingEvent(playbackEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());
        final List<TrackingRecord> trackingRecords = captor.getAllValues();
        final TrackingRecord event1 = trackingRecords.get(0);
        final TrackingRecord event2 = trackingRecords.get(1);

        expect(event1.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event1.getTimeStamp()).toEqual(12345L);
        expect(event1.getUrl()).toEqual("url1");

        expect(event2.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event2.getTimeStamp()).toEqual(12345L);
        expect(event2.getUrl()).toEqual("url2");
    }

    @Test
    public void tracksAdClickthroughs() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        UIEvent event = UIEvent.fromAudioAdCompanionDisplayClick(audioAd, Urn.forTrack(777), userUrn, trackSourceInfo, 10000);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingRecord adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(event.getTimeStamp());
        expect(adEvent.getUrl()).toEqual("click1");

        expect(allValues.get(1).getUrl()).toEqual("click2");
    }

    @Test
    public void tracksAdSkips() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123));
        UIEvent event = UIEvent.fromSkipAudioAdClick(audioAd, Urn.forTrack(456), userUrn, trackSourceInfo, 10000);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingRecord adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(event.getTimeStamp());
        expect(adEvent.getUrl()).toEqual("skip1");

        expect(allValues.get(1).getUrl()).toEqual("skip2");
    }

    @Test
    public void doesNotTrackNavigationEvents() throws Exception {
        analyticsProvider.handleTrackingEvent(UIEvent.fromExploreNav());
        analyticsProvider.handleTrackingEvent(UIEvent.fromLikesNav());
        analyticsProvider.handleTrackingEvent(UIEvent.fromPlaylistsNav());
        analyticsProvider.handleTrackingEvent(UIEvent.fromProfileNav());

        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void tracksAdsFinishing() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent().withAudioAd(audioAd);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        expect(allValues.size()).toEqual(2);

        TrackingRecord adEvent = allValues.get(0);
        expect(adEvent.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(adEvent.getTimeStamp()).toEqual(event.getTimeStamp());
        expect(adEvent.getUrl()).toEqual("finish1");

        expect(allValues.get(1).getUrl()).toEqual("finish2");
    }

    @Test
    public void tracksAudioAdCompanionImpressions() {
        final PropertySet audioAdMetadata = TestPropertySets.audioAdProperties(Urn.forTrack(999));
        VisualAdImpressionEvent impressionEvent = new VisualAdImpressionEvent(
                audioAdMetadata, Urn.forTrack(888), Urn.forUser(777), trackSourceInfo, 333
        );

        analyticsProvider.handleTrackingEvent(impressionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        expect(event1.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event1.getTimeStamp()).toEqual(333l);
        expect(event1.getUrl()).toEqual("visual1");

        final TrackingRecord event2 = captor.getAllValues().get(1);
        expect(event2.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event2.getTimeStamp()).toEqual(333l);
        expect(event2.getUrl()).toEqual("visual2");
    }

    @Test
    public void tracksLeaveBehindImpressions() {
        final PropertySet audioAdMetadata = TestPropertySets.leaveBehindForPlayer();
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        LeaveBehindTrackingEvent impressionEvent = LeaveBehindTrackingEvent.forImpression(333, audioAdMetadata, Urn.forTrack(888), Urn.forUser(777), sourceInfo);

        analyticsProvider.handleTrackingEvent(impressionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        expect(event1.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event1.getTimeStamp()).toEqual(333l);
        expect(event1.getUrl()).toEqual("leaveBehindTrackingImpressionUrl1");

        final TrackingRecord event2 = captor.getAllValues().get(1);
        expect(event2.getBackend()).toEqual(PromotedAnalyticsProvider.BACKEND_NAME);
        expect(event2.getTimeStamp()).toEqual(333l);
        expect(event2.getUrl()).toEqual("leaveBehindTrackingImpressionUrl2");
    }


    @Test
    public void shouldForwardFlushCallToEventTracker() {
        analyticsProvider.flush();
        verify(eventTracker).flush(PromotedAnalyticsProvider.BACKEND_NAME);
    }

}