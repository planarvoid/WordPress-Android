package com.soundcloud.android.analytics.promoted;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackProgressEvent;
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
        assertPromotedTrackingRecord(event1, "url1", 12345L);
        final TrackingRecord event2 = trackingRecords.get(1);
        assertPromotedTrackingRecord(event2, "url2", 12345L);
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
        assertPromotedTrackingRecord(adEvent, "comp_click1", event.getTimestamp());
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
        assertPromotedTrackingRecord(adEvent, "audio_skip1", event.getTimestamp());
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
        assertPromotedTrackingRecord(adEvent, "audio_finish1", event.getTimestamp());
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
        assertPromotedTrackingRecord(event1, "comp_impression1", 333l);
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "comp_impression2", 333l);
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
        assertPromotedTrackingRecord(event1, "leave_impression1", 333l);
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "leave_impression2", 333l);
    }

    @Test
    public void tracksPromotedTrackUrls() {
        PromotedListItem track = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        PromotedTrackingEvent event = PromotedTrackingEvent.forItemClick(track, "stream");

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "promoted1", event.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "promoted2", event.getTimestamp());
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
        assertPromotedTrackingRecord(event1, "promoPlay1", 12345L);
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "promoPlay2", 12345L);
    }

    @Test
    public void tracksFirstQuartileAdProgressEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackProgressEvent playbackProgressEvent = AdPlaybackProgressEvent.forFirstQuartile(Urn.forAd("dfp", "809"), videoAd, sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_quartile1_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_quartile1_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksSecondQuartileAdProgressEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackProgressEvent playbackProgressEvent = AdPlaybackProgressEvent.forSecondQuartile(Urn.forAd("dfp", "809"), videoAd, sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_quartile2_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_quartile2_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksThirdQuartileAdProgressEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackProgressEvent playbackProgressEvent = AdPlaybackProgressEvent.forThirdQuartile(Urn.forAd("dfp", "809"), videoAd, sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTracker, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_quartile3_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_quartile3_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void noQuartileEventsForAudioAdsAreTracked() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackProgressEvent playbackProgressEvent = AdPlaybackProgressEvent.forThirdQuartile(Urn.forAd("dfp", "809"), audioAd, sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        verify(eventTracker, never()).trackEvent(any(TrackingRecord.class));
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

    private void assertPromotedTrackingRecord(TrackingRecord trackingRecord, String data, long timeStamp) {
        assertThat(trackingRecord.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(trackingRecord.getData()).isEqualTo(data);
        assertThat(trackingRecord.getTimeStamp()).isEqualTo(timeStamp);
    }
}
