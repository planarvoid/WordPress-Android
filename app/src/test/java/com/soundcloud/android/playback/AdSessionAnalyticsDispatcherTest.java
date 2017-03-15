package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_ERROR;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_PAUSE;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED;
import static com.soundcloud.android.testsupport.fixtures.TestPlayStates.wrap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdViewabilityController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.StopReasonProvider.StopReason;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.List;

public class AdSessionAnalyticsDispatcherTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final long PROGRESS = 1001L;
    private static final long DURATION = 2001L;
    public static final String PAGE_NAME = "page name";

    private AdSessionAnalyticsDispatcher dispatcher;

    @Mock private AdsOperations adsOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private AppboyPlaySessionState appboyPlaySessionState;
    @Mock private StopReasonProvider stopReasonProvider;
    @Mock private AdViewabilityController adViewabilityController;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource("stream"));
        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(false);
        when(trackSourceInfo.getOriginScreen()).thenReturn(PAGE_NAME);
        when(stopReasonProvider.fromTransition(any(PlaybackStateTransition.class))).thenReturn(STOP_REASON_ERROR);

        dispatcher = new AdSessionAnalyticsDispatcher(eventBus, stopReasonProvider, adViewabilityController);
    }

    @Test
    public void progressEventsTriggerQuartileEvents() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        playTransition();
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100, TRACK_URN),
                                                                Urn.forAd("dfp", "809")));

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(3);
        assertThat(((AdPlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        AdPlaybackSessionEvent adEvent = (AdPlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(adEvent.clickName().get().key()).isEqualTo("ad::first_quartile");
    }

    @Test
    public void duplicateQuartileEventsAreNotPublished() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        playTransition();
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100, TRACK_URN),
                                                                Urn.forAd("dfp", "809")));
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100, TRACK_URN),
                                                                Urn.forAd("dfp", "809")));

        final List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events.size()).isEqualTo(3);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(((AdRichMediaSessionEvent) events.get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
        assertThat(((AdPlaybackSessionEvent) events.get(2)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.QUARTILE);
    }

    @Test
    public void stateChangeEventForStartPublishesPlayEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        PlaybackStateTransition playEvent = playTransition();

        final List<TrackingEvent> trackingEvents = eventBus.eventsOn(EventQueue.TRACKING);
        AdPlaybackSessionEvent playbackSessionEvent = (AdPlaybackSessionEvent) trackingEvents.get(trackingEvents.size() - 2);
        AdRichMediaSessionEvent richMediaSessionEvent = (AdRichMediaSessionEvent) trackingEvents.get(trackingEvents.size() - 1);
        assertCommonEventData(playEvent, richMediaSessionEvent);
        assertThat(playbackSessionEvent.eventKind()).isNotEqualTo(AdPlaybackSessionEvent.EventKind.FINISH);
        assertThat(playbackSessionEvent.adUrn()).isEqualTo(audioAd.getAdUrn());
        assertThat(playbackSessionEvent.monetizableTrackUrn()).isEqualTo(Optional.of(audioAd.getMonetizableTrackUrn()));
    }

    @Test
    public void resumeEventsSentInsteadOfStartOnSubsequentPlayTransistions() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        dispatcher.onPlayTransition(TestPlayStates.playing(), true);
        dispatcher.onStopTransition(TestPlayStates.idle(), false);
        dispatcher.onPlayTransition(TestPlayStates.playing(), true);

        final List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events.size()).isEqualTo(5);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(((AdRichMediaSessionEvent) events.get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
        assertThat(((AdRichMediaSessionEvent) events.get(2)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PAUSE);
        assertThat(((AdPlaybackSessionEvent) events.get(3)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.RESUME);
        assertThat(((AdRichMediaSessionEvent) events.get(4)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
    }

    @Test
    public void stateChangeEventForFinishPublishesStopEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        final PlaybackStateTransition stateTransitionFinished = stopTransition(PlaybackState.IDLE,
                                                                               PlayStateReason.PLAYBACK_COMPLETE,
                                                                               STOP_REASON_TRACK_FINISHED);


        when(stopReasonProvider.fromTransition(any(PlaybackStateTransition.class))).thenReturn(STOP_REASON_TRACK_FINISHED);

        playTransition();
        stopTransition(PlaybackState.BUFFERING, PlayStateReason.NONE); // make sure intermediate events don't matter
        playTransition();

        final List<TrackingEvent> trackingEvents = eventBus.eventsOn(EventQueue.TRACKING);
        AdPlaybackSessionEvent event = (AdPlaybackSessionEvent) trackingEvents.get(2);
        AdRichMediaSessionEvent richEvent = (AdRichMediaSessionEvent) trackingEvents.get(3);

        assertCommonEventData(stateTransitionFinished, richEvent);
        assertThat(event.adUrn()).isEqualTo(audioAd.getAdUrn());
        assertThat(event.monetizableTrackUrn()).isEqualTo(Optional.of(audioAd.getMonetizableTrackUrn()));
    }

    @Test
    public void publishesStopEventWhenUserSkipsBetweenTracksManually() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        playTransition();
        skipTransition();

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).adUrn()).isEqualTo(audioAd.getAdUrn());
        assertThat(((AdRichMediaSessionEvent) events.get(2)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PAUSE);
        assertThat(((AdRichMediaSessionEvent) events.get(2)).adUrn()).isEqualTo(audioAd.getAdUrn());
    }

    @Test
    public void publishesPauseEventWhenStopTransistionStopReasonIsPause() {
        when(stopReasonProvider.fromTransition(any(PlaybackStateTransition.class))).thenReturn(STOP_REASON_PAUSE);
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        dispatcher.onPlayTransition(TestPlayStates.playing(), true);
        dispatcher.onStopTransition(TestPlayStates.idle(), false);

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(4);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(((AdRichMediaSessionEvent) events.get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
        assertThat(((AdPlaybackSessionEvent) events.get(2)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PAUSE);
        assertThat(((AdRichMediaSessionEvent) events.get(3)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PAUSE);
    }

    @Test
    public void shouldPublishCheckpointEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        final PlaybackStateTransition transition = playTransition();
        dispatcher.onProgressCheckpoint(wrap(transition),
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000L, 30000L, TRACK_URN),
                                                                     transition.getUrn()));

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(((AdRichMediaSessionEvent) events.get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
        assertThat(((AdRichMediaSessionEvent) events.get(2)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_CHECKPOINT);
    }

    @Test
    public void shouldNotPublishCheckpointEventIfLastTransitionWasntPlayTransition() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        final PlaybackStateTransition transition = playTransition();
        stopTransition(PlaybackState.IDLE, PlayStateReason.NONE);
        dispatcher.onProgressCheckpoint(wrap(transition),
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000, 30000, TRACK_URN),
                                                                     transition.getUrn()));

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(((AdRichMediaSessionEvent) events.get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
        assertThat(((AdRichMediaSessionEvent) events.get(2)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PAUSE);
    }

    @Test
    public void shouldNotPublishCheckpointEventIfPlaybackProgressUrnDoesntMatchPlayTransition() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        dispatcher.setAdMetadata(audioAd, trackSourceInfo);

        final PlaybackStateTransition transition = playTransition();
        dispatcher.onProgressCheckpoint(wrap(transition),
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000, 30000, TRACK_URN),
                                                                     Urn.forTrack(101L)));

        assertThat(((AdPlaybackSessionEvent) eventBus.eventsOn(EventQueue.TRACKING).get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.START);
        assertThat(((AdRichMediaSessionEvent) eventBus.eventsOn(EventQueue.TRACKING).get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
    }

    @Test
    public void shouldForwardProgressQuartileEventsToAdViewabilityControllerForVideoAds() {
        when(stopReasonProvider.fromTransition(any(PlaybackStateTransition.class))).thenReturn(STOP_REASON_TRACK_FINISHED);

        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final Urn urn = videoAd.getAdUrn();

        dispatcher.setAdMetadata(videoAd, trackSourceInfo);
        dispatcher.onPlayTransition(TestPlayStates.playing(urn, 0, 100), false);
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100, urn), urn));
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(50, 100, urn), urn));
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(75, 100, urn), urn));
        dispatcher.onStopTransition(TestPlayStates.complete(urn, 100, 100), false);

        InOrder inOrder = inOrder(adViewabilityController);
        inOrder.verify(adViewabilityController).onProgressQuartileEvent(videoAd, PlayableAdData.ReportingEvent.START, 0);
        inOrder.verify(adViewabilityController).onProgressQuartileEvent(videoAd, PlayableAdData.ReportingEvent.FIRST_QUARTILE, 25);
        inOrder.verify(adViewabilityController).onProgressQuartileEvent(videoAd, PlayableAdData.ReportingEvent.SECOND_QUARTILE, 50);
        inOrder.verify(adViewabilityController).onProgressQuartileEvent(videoAd, PlayableAdData.ReportingEvent.THIRD_QUARTILE, 75);
        inOrder.verify(adViewabilityController).onProgressQuartileEvent(videoAd, PlayableAdData.ReportingEvent.FINISH, 100);
    }

    @Test
    public void shouldForwardPauseAndResumeEventsToAdViewabilityControllerForVideoAds() {
        when(stopReasonProvider.fromTransition(any(PlaybackStateTransition.class))).thenReturn(STOP_REASON_PAUSE);

        final VideoAd videoAd = AdFixtures.getVideoAd(TRACK_URN);
        final Urn urn = videoAd.getAdUrn();

        dispatcher.setAdMetadata(videoAd, trackSourceInfo);
        dispatcher.onPlayTransition(TestPlayStates.playing(urn, 0, 100), false);
        dispatcher.onStopTransition(TestPlayStates.idle(urn, 25, 100), false);
        dispatcher.onPlayTransition(TestPlayStates.playing(urn, 25, 100), false);

        InOrder inOrder = inOrder(adViewabilityController);
        inOrder.verify(adViewabilityController).onProgressQuartileEvent(videoAd, PlayableAdData.ReportingEvent.START, 0);
        inOrder.verify(adViewabilityController).onPaused(videoAd, 25);
        inOrder.verify(adViewabilityController).onResume(videoAd, 25);
    }

    private PlaybackStateTransition playTransition() {
        return withExtras(playTransitionForTrack(TRACK_URN));
    }

    private PlaybackStateTransition playTransitionForTrack(Urn trackUrn) {
        final PlaybackStateTransition startEvent = new PlaybackStateTransition(
                PlaybackState.PLAYING, PlayStateReason.NONE, trackUrn, PROGRESS, DURATION);

        dispatcher.onPlayTransition(wrap(withExtras(startEvent)), true);

        return startEvent;
    }

    private PlaybackStateTransition skipTransition() {
        final PlaybackStateTransition skipEvent = new PlaybackStateTransition(
                PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN, PROGRESS, DURATION);

        dispatcher.onSkipTransition(wrap(withExtras(skipEvent)));

        return skipEvent;
    }

    private PlaybackStateTransition stopTransition(PlaybackState newState, PlayStateReason reason) {
        final PlaybackStateTransition stopEvent = new PlaybackStateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");

        dispatcher.onStopTransition(wrap(withExtras(stopEvent)), false);

        return stopEvent;
    }

    private PlaybackStateTransition stopTransition(PlaybackState newState, PlayStateReason reason, StopReason stopReason) {
        final PlaybackStateTransition stopEvent = new PlaybackStateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        when(stopReasonProvider.fromTransition(stopEvent)).thenReturn(stopReason);

        dispatcher.onStopTransition(wrap(withExtras(stopEvent)), false);

        return stopEvent;
    }

    private static PlaybackStateTransition withExtras(PlaybackStateTransition transition) {
        return transition
                .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, "play er")
                .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
    }

    private void assertCommonEventData(PlaybackStateTransition stateTransition, AdRichMediaSessionEvent richMediaSessionEvent) {
        assertThat(richMediaSessionEvent.protocol()).isEqualTo(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL));
        assertThat(richMediaSessionEvent.clickEventId()).isNotEmpty();
        assertThat(richMediaSessionEvent.playheadPosition()).isEqualTo(PROGRESS);
        assertThat(richMediaSessionEvent.trackLength()).isEqualTo(DURATION);
        assertThat(richMediaSessionEvent.getTimestamp()).isGreaterThan(0L);
    }
}
