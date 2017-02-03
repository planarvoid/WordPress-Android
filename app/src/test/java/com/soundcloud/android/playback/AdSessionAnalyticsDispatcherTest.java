package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_ERROR;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED;
import static com.soundcloud.android.testsupport.fixtures.TestPlayStates.wrap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.StopReasonProvider.StopReason;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
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

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource("stream"));
        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(false);
        when(trackSourceInfo.getOriginScreen()).thenReturn(PAGE_NAME);
        when(stopReasonProvider.fromTransition(any(PlaybackStateTransition.class))).thenReturn(STOP_REASON_ERROR);

        dispatcher = new AdSessionAnalyticsDispatcher(eventBus, playQueueManager, adsOperations, stopReasonProvider);
    }

    @Test
    public void progressEventsTriggerQuartileEvents() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        playTransition();
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100, TRACK_URN),
                                                                Urn.forAd("dfp", "809")));

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(3);
        assertThat(((AdPlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PLAY);
        AdPlaybackSessionEvent adEvent = (AdPlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(adEvent.clickName().get().toString()).isEqualTo("ad::first_quartile");
    }

    @Test
    public void duplicateQuartileEventsAreNotPublished() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        playTransition();
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100, TRACK_URN),
                                                                Urn.forAd("dfp", "809")));
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100, TRACK_URN),
                                                                Urn.forAd("dfp", "809")));

        assertThat(((AdPlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PLAY);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(AdPlaybackSessionEvent.class);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(3);
    }

    @Test
    public void stateChangeEventForStartPublishesPlayEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        PlaybackStateTransition playEvent = playTransition();

        final List<TrackingEvent> trackingEvents = eventBus.eventsOn(EventQueue.TRACKING);
        AdPlaybackSessionEvent playbackSessionEvent = (AdPlaybackSessionEvent) trackingEvents.get(trackingEvents.size() - 2);
        AdRichMediaSessionEvent richMediaSessionEvent = (AdRichMediaSessionEvent) trackingEvents.get(trackingEvents.size() - 1);
        assertCommonEventData(playEvent, richMediaSessionEvent);
        assertThat(playbackSessionEvent.eventKind()).isNotEqualTo(AdPlaybackSessionEvent.EventKind.STOP);
        assertThat(playbackSessionEvent.adUrn()).isEqualTo(audioAd.getAdUrn());
        assertThat(playbackSessionEvent.monetizableTrackUrn()).isEqualTo(audioAd.getMonetizableTrackUrn());
    }

    @Test
    public void duplicateStartEventsReportFalseForFirstPlay() {
        when(adsOperations.isCurrentItemVideoAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        dispatcher.onPlayTransition(TestPlayStates.playing(), true);
        dispatcher.onStopTransition(TestPlayStates.idle(), false);
        dispatcher.onPlayTransition(TestPlayStates.playing(), false);

        final List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        final AdPlaybackSessionEvent first = (AdPlaybackSessionEvent) events.get(0);
        assertThat(first.eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PLAY);
        final AdRichMediaSessionEvent second = (AdRichMediaSessionEvent) events.get(events.size() - 1);
        assertThat(second.action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
    }

    @Test
    public void stateChangeEventForFinishPublishesStopEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

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
        assertThat(event.stopReason().get()).isEqualTo(STOP_REASON_TRACK_FINISHED);
        assertThat(event.adUrn()).isEqualTo(audioAd.getAdUrn());
        assertThat(event.monetizableTrackUrn()).isEqualTo(audioAd.getMonetizableTrackUrn());
    }

    @Test
    public void publishesStopEventWhenUserSkipsBetweenTracksManually() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        playTransition();
        skipTransition();

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).isInstanceOf(AdPlaybackSessionEvent.class);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PLAY);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).adUrn()).isEqualTo(audioAd.getAdUrn());
        assertThat(((AdRichMediaSessionEvent) events.get(2)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PAUSE);
        assertThat(((AdRichMediaSessionEvent) events.get(2)).adUrn()).isEqualTo(audioAd.getAdUrn());
    }

    @Test
    public void shouldPublishCheckpointEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        final PlaybackStateTransition transition = playTransition();
        dispatcher.onProgressCheckpoint(wrap(transition),
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000L, 30000L, TRACK_URN),
                                                                     transition.getUrn()));

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PLAY);
        assertThat(((AdRichMediaSessionEvent) events.get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
        assertThat(((AdRichMediaSessionEvent) events.get(2)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_CHECKPOINT);
    }

    @Test
    public void shouldNotPublishCheckpointEventIfLastTransitionWasntPlayTransition() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        final PlaybackStateTransition transition = playTransition();
        stopTransition(PlaybackState.IDLE, PlayStateReason.NONE);
        dispatcher.onProgressCheckpoint(wrap(transition),
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000, 30000, TRACK_URN),
                                                                     transition.getUrn()));

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(((AdPlaybackSessionEvent) events.get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PLAY);
        assertThat(((AdRichMediaSessionEvent) events.get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
        assertThat(((AdRichMediaSessionEvent) events.get(2)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PAUSE);
    }

    @Test
    public void shouldNotPublishCheckpointEventIfPlaybackProgressUrnDoesntMatchPlayTransition() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        final PlaybackStateTransition transition = playTransition();
        dispatcher.onProgressCheckpoint(wrap(transition),
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000, 30000, TRACK_URN),
                                                                     Urn.forTrack(101L)));

        assertThat(((AdPlaybackSessionEvent) eventBus.eventsOn(EventQueue.TRACKING).get(0)).eventKind()).isEqualTo(AdPlaybackSessionEvent.EventKind.PLAY);
        assertThat(((AdRichMediaSessionEvent) eventBus.eventsOn(EventQueue.TRACKING).get(1)).action()).isEqualTo(AdRichMediaSessionEvent.Action.AUDIO_ACTION_PLAY);
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
