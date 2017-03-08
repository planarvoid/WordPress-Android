package com.soundcloud.android.analytics.promoted;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_PAUSE;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehindAd;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.EventTrackingManager;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdSessionEventArgs;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.List;

public class PromotedAnalyticsProviderTest extends AndroidUnitTest {

    private PromotedAnalyticsProvider analyticsProvider;

    @Mock private EventTrackingManager eventTrackingManager;
    private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() {
        initMocks(this);
        analyticsProvider = new PromotedAnalyticsProvider(eventTrackingManager);
        trackSourceInfo = new TrackSourceInfo("origin screen", true);
    }

    @Test
    public void tracksAdClickthroughs() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        UIEvent event = UIEvent.fromPlayableClickThrough(audioAd, trackSourceInfo);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> allValues = captor.getAllValues();
        assertThat(allValues.size()).isEqualTo(2);

        TrackingRecord adEvent = allValues.get(0);
        assertPromotedTrackingRecord(adEvent, "comp_click1", event.getTimestamp());
        assertThat(allValues.get(1).getData()).isEqualTo("comp_click2");
    }

    @Test
    public void tracksAudioAdClickthrough() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent event = UIEvent.fromPlayableClickThrough(videoAd, trackSourceInfo);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> events = captor.getAllValues();
        assertThat(events.size()).isEqualTo(2);
        assertPromotedTrackingRecord(events.get(0), "video_click1", event.getTimestamp());
        assertPromotedTrackingRecord(events.get(1), "video_click2", event.getTimestamp());
    }

    @Test
    public void tracksVideoAdSkips() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent event = UIEvent.fromSkipAdClick(videoAd, trackSourceInfo);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> events = captor.getAllValues();
        assertThat(events.size()).isEqualTo(2);
        assertPromotedTrackingRecord(events.get(0), "video_skip1", event.getTimestamp());
        assertPromotedTrackingRecord(events.get(1), "video_skip2", event.getTimestamp());
    }

    @Test
    public void tracksVideoAdClickthrough() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        UIEvent event = UIEvent.fromPlayableClickThrough(videoAd, trackSourceInfo);

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        List<TrackingRecord> events = captor.getAllValues();
        assertThat(events.size()).isEqualTo(2);
        assertPromotedTrackingRecord(events.get(0), "video_click1", event.getTimestamp());
        assertPromotedTrackingRecord(events.get(1), "video_click2", event.getTimestamp());
    }

    @Test
    public void tracksAudioAdCompanionImpressions() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(999L));
        VisualAdImpressionEvent impressionEvent = VisualAdImpressionEvent.create(
                audioAd, Urn.forUser(777), trackSourceInfo
        );

        analyticsProvider.handleTrackingEvent(impressionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "comp_impression1", impressionEvent.timestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "comp_impression2", impressionEvent.timestamp());
    }

    @Test
    public void tracksInlayAdImpressions() {
        final AppInstallAd appInstall = AdFixtures.getAppInstalls().get(0);
        final InlayAdImpressionEvent impressionEvent = InlayAdImpressionEvent.create(appInstall, 42, 9876543210L);
        final ArgumentCaptor<TrackingRecord> eventCaptor = ArgumentCaptor.forClass(TrackingRecord.class);

        analyticsProvider.handleTrackingEvent(impressionEvent);

        verify(eventTrackingManager, times(2)).trackEvent(eventCaptor.capture());

        final List<TrackingRecord> promotedEvents = eventCaptor.getAllValues();
        assertPromotedTrackingRecord(promotedEvents.get(0), "app_impression1", 9876543210L);
        assertPromotedTrackingRecord(promotedEvents.get(1), "app_impression2", 9876543210L);
    }

    @Test
    public void tracksLeaveBehindImpressions() {
        final LeaveBehindAd leaveBehindAd = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        AdOverlayTrackingEvent impressionEvent = AdOverlayTrackingEvent.forImpression(333,
                                                                                      leaveBehindAd,
                                                                                      Urn.forTrack(888),
                                                                                      Urn.forUser(777),
                                                                                      sourceInfo);

        analyticsProvider.handleTrackingEvent(impressionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "leave_impression1", 333L);
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "leave_impression2", 333L);
    }

    @Test
    public void tracksPromotedTrackUrls() {
        PromotedListItem track = PlayableFixtures.expectedPromotedTrack();
        PromotedTrackingEvent event = PromotedTrackingEvent.forItemClick(track, "stream");

        analyticsProvider.handleTrackingEvent(event);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "promoted1", event.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "promoted2", event.getTimestamp());
    }

    @Test
    public void tracksPromotedTrackPlayUrls() {
        PlaybackSessionEvent playbackEvent = mock(PlaybackSessionEvent.class);
        when(playbackEvent.isPromotedTrack()).thenReturn(true);
        when(playbackEvent.isPlayAdShouldReportAdStart()).thenReturn(true);
        when(playbackEvent.getTimestamp()).thenReturn(12345L);
        when(playbackEvent.promotedPlayUrls()).thenReturn(Optional.of(asList("promoPlay1", "promoPlay2")));

        analyticsProvider.handleTrackingEvent(playbackEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "promoPlay1", 12345L);
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "promoPlay2", 12345L);
    }

    @Test
    public void tracksFirstQuartileAdProgressEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackSessionEvent playbackProgressEvent = AdPlaybackSessionEvent.forFirstQuartile(videoAd,
                                                                                                     sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_quartile1_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_quartile1_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksSecondQuartileAdProgressEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackSessionEvent playbackProgressEvent = AdPlaybackSessionEvent.forSecondQuartile(videoAd,
                                                                                                      sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_quartile2_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_quartile2_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksThirdQuartileAdProgressEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackSessionEvent playbackProgressEvent = AdPlaybackSessionEvent.forThirdQuartile(videoAd,
                                                                                                     sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_quartile3_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_quartile3_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksFirstQuartileAdProgressEventsForAudio() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackSessionEvent playbackProgressEvent = AdPlaybackSessionEvent.forFirstQuartile(audioAd,
                                                                                                     sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "audio_quartile1_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "audio_quartile1_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksSecondQuartileAdProgressEventsForAudio() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackSessionEvent playbackProgressEvent = AdPlaybackSessionEvent.forSecondQuartile(audioAd,
                                                                                                      sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "audio_quartile2_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "audio_quartile2_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksThirdQuartileAdProgressEventsForAudio() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final AdPlaybackSessionEvent playbackProgressEvent = AdPlaybackSessionEvent.forThirdQuartile(audioAd,
                                                                                                     sourceInfo);

        analyticsProvider.handleTrackingEvent(playbackProgressEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "audio_quartile3_1", playbackProgressEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "audio_quartile3_2", playbackProgressEvent.getTimestamp());
    }

    @Test
    public void tracksFullscreenAdEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final UIEvent fullscreenEvent = UIEvent.fromVideoAdFullscreen(videoAd, sourceInfo);

        analyticsProvider.handleTrackingEvent(fullscreenEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_fullscreen1", fullscreenEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_fullscreen2", fullscreenEvent.getTimestamp());
    }

    @Test
    public void tracksShrinkAdEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final TrackSourceInfo sourceInfo = new TrackSourceInfo("page source", true);
        final UIEvent fullscreenEvent = UIEvent.fromVideoAdShrink(videoAd, sourceInfo);

        analyticsProvider.handleTrackingEvent(fullscreenEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_exit_full1", fullscreenEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_exit_full2", fullscreenEvent.getTimestamp());
    }

    @Test
    public void tracksImpressionAndStartAdEventsTogetherForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdSessionEventArgs args = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.playing(), "123");
        final AdPlaybackSessionEvent playbackSessionEvent = AdPlaybackSessionEvent.forPlay(videoAd, args);

        analyticsProvider.handleTrackingEvent(playbackSessionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(4)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_impression1", playbackSessionEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_impression2", playbackSessionEvent.getTimestamp());
        final TrackingRecord event3 = captor.getAllValues().get(2);
        assertPromotedTrackingRecord(event3, "video_start1", playbackSessionEvent.getTimestamp());
        final TrackingRecord event4 = captor.getAllValues().get(3);
        assertPromotedTrackingRecord(event4, "video_start2", playbackSessionEvent.getTimestamp());
    }

    @Test
    public void tracksResumeAdEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        videoAd.setEventReported(PlayableAdData.ReportingEvent.START_EVENT);
        final AdSessionEventArgs args = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.playing(), "123");
        final AdPlaybackSessionEvent playbackSessionEvent = AdPlaybackSessionEvent.forPlay(videoAd, args);

        analyticsProvider.handleTrackingEvent(playbackSessionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_resume1", playbackSessionEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_resume2", playbackSessionEvent.getTimestamp());
    }

    @Test
    public void tracksPauseAdEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdSessionEventArgs args = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.idle(), "123");
        final AdPlaybackSessionEvent playbackSessionEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                                           args,
                                                                                           STOP_REASON_PAUSE);

        analyticsProvider.handleTrackingEvent(playbackSessionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_pause1", playbackSessionEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_pause2", playbackSessionEvent.getTimestamp());
    }

    @Test
    public void tracksFinishAdEventsForVideo() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdSessionEventArgs args = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.idle(), "123");
        final AdPlaybackSessionEvent playbackSessionEvent = AdPlaybackSessionEvent.forStop(videoAd,
                                                                                           args,
                                                                                           STOP_REASON_TRACK_FINISHED);

        analyticsProvider.handleTrackingEvent(playbackSessionEvent);

        ArgumentCaptor<TrackingRecord> captor = ArgumentCaptor.forClass(TrackingRecord.class);
        verify(eventTrackingManager, times(2)).trackEvent(captor.capture());

        final TrackingRecord event1 = captor.getAllValues().get(0);
        assertPromotedTrackingRecord(event1, "video_finish1", playbackSessionEvent.getTimestamp());
        final TrackingRecord event2 = captor.getAllValues().get(1);
        assertPromotedTrackingRecord(event2, "video_finish2", playbackSessionEvent.getTimestamp());
    }

    @Test
    public void forwardsFlushCallToEventTracker() {
        analyticsProvider.flush();
        verify(eventTrackingManager).flush(PromotedAnalyticsProvider.BACKEND_NAME);
    }

    @Test
    public void sendsTrackingEventAsap() {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123L));
        final AdSessionEventArgs args = AdSessionEventArgs.create(trackSourceInfo, TestPlayerTransitions.playing(), "123");
        final AdPlaybackSessionEvent playbackSessionEvent = AdPlaybackSessionEvent.forPlay(videoAd, args);

        analyticsProvider.handleTrackingEvent(playbackSessionEvent);

        verify(eventTrackingManager, times(4)).trackEvent(any(TrackingRecord.class));
        verify(eventTrackingManager).flush(PromotedAnalyticsProvider.BACKEND_NAME);
    }

    private void assertPromotedTrackingRecord(TrackingRecord trackingRecord, String data, long timeStamp) {
        assertThat(trackingRecord.getBackend()).isEqualTo(PromotedAnalyticsProvider.BACKEND_NAME);
        assertThat(trackingRecord.getData()).isEqualTo(data);
        assertThat(trackingRecord.getTimeStamp()).isEqualTo(timeStamp);
    }
}
