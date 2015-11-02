package com.soundcloud.android.analytics.promoted;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.List;

public class PromotedAnalyticsProviderTest extends AndroidUnitTest {

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
    public void tracksAdPlayUrls() throws Exception {
        PlaybackSessionEvent playbackEvent = mock(PlaybackSessionEvent.class);
        when(playbackEvent.isAd()).thenReturn(true);
        when(playbackEvent.isFirstPlay()).thenReturn(true);
        when(playbackEvent.getTimestamp()).thenReturn(12345L);
        when(playbackEvent.getAudioAdImpressionUrls()).thenReturn(asList("url1", "url2"));

        analyticsProvider.handleTrackingEvent(playbackEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());
        final List<TrackingRecord> trackingRecords = captor.getAllValues();
        final TrackingRecord event1 = trackingRecords.get(0);
        final TrackingRecord event2 = trackingRecords.get(1);

        assertThat(event1.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event1.getTimeStamp()).isEqualTo(12345L);
        assertThat(event1.getData()).isEqualTo("url1");

        assertThat(event2.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event2.getTimeStamp()).isEqualTo(12345L);
        assertThat(event2.getData()).isEqualTo("url2");
    }

    @Test
    public void tracksAdClickthroughs() throws Exception {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        UIEvent event = UIEvent.fromAudioAdCompanionDisplayClick(audioAd, Urn.forTrack(777), userUrn, trackSourceInfo, 10000);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);

        TrackingRecord adEvent = allValues.get(0);
        assertThat(adEvent.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(adEvent.getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(adEvent.getData()).isEqualTo("comp_click1");

        assertThat(allValues.get(1).getData()).isEqualTo("comp_click2");
    }

    @Test
    public void tracksAdSkips() throws Exception {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        UIEvent event = UIEvent.fromSkipAudioAdClick(audioAd, Urn.forTrack(456), userUrn, trackSourceInfo, 10000);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);

        TrackingRecord adEvent = allValues.get(0);
        assertThat(adEvent.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(adEvent.getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(adEvent.getData()).isEqualTo("audio_skip1");

        assertThat(allValues.get(1).getData()).isEqualTo("audio_skip2");
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
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        PlaybackSessionEvent event = TestEvents.playbackSessionTrackFinishedEvent().withAudioAd(audioAd);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);

        TrackingRecord adEvent = allValues.get(0);
        assertThat(adEvent.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(adEvent.getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(adEvent.getData()).isEqualTo("audio_finish1");

        assertThat(allValues.get(1).getData()).isEqualTo("audio_finish2");
    }

    @Test
    public void tracksAudioAdCompanionImpressions() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(999L));
        VisualAdImpressionEvent impressionEvent = new VisualAdImpressionEvent(
                audioAd, Urn.forTrack(888), Urn.forUser(777), trackSourceInfo, 333
        );

        analyticsProvider.handleTrackingEvent(impressionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertThat(event1.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event1.getTimeStamp()).isEqualTo(333l);
        assertThat(event1.getData()).isEqualTo("comp_impression1");

        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertThat(event2.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event2.getTimeStamp()).isEqualTo(333l);
        assertThat(event2.getData()).isEqualTo("comp_impression2");
    }

    @Test
    public void tracksLeaveBehindImpressions() {
        final LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        AdOverlayTrackingEvent impressionEvent = AdOverlayTrackingEvent.forImpression(333, leaveBehindAd, Urn.forTrack(888), Urn.forUser(777), sourceInfo);

        analyticsProvider.handleTrackingEvent(impressionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertThat(event1.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event1.getTimeStamp()).isEqualTo(333l);
        assertThat(event1.getData()).isEqualTo("leave_impression1");

        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertThat(event2.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event2.getTimeStamp()).isEqualTo(333l);
        assertThat(event2.getData()).isEqualTo("leave_impression2");
    }

    @Test
    public void tracksPromotedTrackUrls() {
        PromotedListItem track = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackingEvent event = PromotedTrackingEvent.forItemClick(track, "stream");

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertThat(event1.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event1.getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(event1.getData()).isEqualTo("promoted1");

        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertThat(event2.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event2.getTimeStamp()).isEqualTo(event.getTimestamp());
        assertThat(event2.getData()).isEqualTo("promoted2");
    }

    @Test
    public void tracksPromotedTrackPlayUrls() {
        PlaybackSessionEvent playbackEvent = mock(PlaybackSessionEvent.class);
        when(playbackEvent.isPromotedTrack()).thenReturn(true);
        when(playbackEvent.isFirstPlay()).thenReturn(true);
        when(playbackEvent.getTimestamp()).thenReturn(12345L);
        when(playbackEvent.getPromotedPlayUrls()).thenReturn(asList("promoPlay1", "promoPlay2"));

        analyticsProvider.handleTrackingEvent(playbackEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertThat(event1.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event1.getTimeStamp()).isEqualTo(12345L);
        assertThat(event1.getData()).isEqualTo("promoPlay1");

        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertThat(event2.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(event2.getTimeStamp()).isEqualTo(12345L);
        assertThat(event2.getData()).isEqualTo("promoPlay2");
    }

    @Test
    public void forwardsFlushCallToEventTracker() {
        analyticsProvider.flush();
        verify(eventTracker).flush(PromotedAnalyticsProvider.BACKEND_NAME);
    }

    @Test
    public void sendsTrackingEventAsap() throws CreateModelException {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final PlaybackSessionEvent event = TestEvents.playbackSessionPlayEventWithProgress(0).withAudioAd(audioAd);
        analyticsProvider.handleTrackingEvent(event);

        verify(eventTracker, times(2)).trackEvent(any(TrackingRecord.class));
        verify(eventTracker).flush(PromotedAnalyticsProvider.BACKEND_NAME);
    }
}
