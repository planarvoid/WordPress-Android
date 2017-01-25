package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.*;
import static com.soundcloud.android.testsupport.fixtures.TestPlayStates.wrap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableTrackingKeys;
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
    private static final Urn CREATOR_URN = Urn.forUser(3L);
    private static final long PROGRESS = 1001L;
    private static final long DURATION = 2001L;

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

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(2);
        assertThat(eventBus.firstEventOn(EventQueue.TRACKING).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_PLAY);
        AdPlaybackSessionEvent adEvent = (AdPlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(adEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::first_quartile");
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

        assertThat(eventBus.firstEventOn(EventQueue.TRACKING).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_PLAY);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(AdPlaybackSessionEvent.class);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(2);
    }

    @Test
    public void stateChangeEventForStartPublishesPlayEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        PlaybackStateTransition playEvent = playTransition();

        AdPlaybackSessionEvent playbackSessionEvent = (AdPlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertCommonEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.getKind()).isNotEqualTo(AdPlaybackSessionEvent.EVENT_KIND_STOP);
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.getMonetizableTrackUrn().toString());
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
        assertThat(first.getKind()).isEqualToIgnoringCase(AdPlaybackSessionEvent.EVENT_KIND_PLAY);
        assertThat(first.shouldReportStart()).isTrue();
        final AdPlaybackSessionEvent second = (AdPlaybackSessionEvent) events.get(2);
        assertThat(second.getKind()).isEqualToIgnoringCase(AdPlaybackSessionEvent.EVENT_KIND_PLAY);
        assertThat(second.shouldReportStart()).isFalse();
    }

    @Test
    public void stateChangeEventForFinishPublishesStopEvent() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        playTransition();
        stopTransition(PlaybackState.BUFFERING, PlayStateReason.NONE); // make sure intermediate events don't matter
        playTransition();
        final PlaybackStateTransition stateTransition = stopTransition(PlaybackState.IDLE,
                                                                       PlayStateReason.PLAYBACK_COMPLETE,
                                                                       STOP_REASON_TRACK_FINISHED);

        when(stopReasonProvider.fromTransition(stateTransition)).thenReturn(STOP_REASON_TRACK_FINISHED);

        AdPlaybackSessionEvent event = (AdPlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);

        assertCommonEventData(stateTransition, event);
        assertThat(event.getStopReason()).isEqualTo(STOP_REASON_TRACK_FINISHED);
        assertThat(event.hasAdFinished()).isTrue();
        assertThat(event.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(event.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.getMonetizableTrackUrn().toString());
    }

    @Test
    public void publishesStopEventWhenUserSkipsBetweenTracksManually() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        playTransition();
        skipTransition();

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(2);
        assertThat(events.get(1)).isInstanceOf(AdPlaybackSessionEvent.class);
        assertThat(events.get(1).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_STOP);
        assertThat(events.get(1).get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
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
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_PLAY);
        assertThat(events.get(1).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_CHECKPOINT);
        assertThat(((AdPlaybackSessionEvent) events.get(1)).getEventArgs().getProgress()).isEqualTo(3000L);
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
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_PLAY);
        assertThat(events.get(1).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_STOP);
    }

    @Test
    public void shouldNotPublishCheckpointEventIfPlaybackProgressUrnDoesntMatchPlayTransition() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.of(audioAd));

        final PlaybackStateTransition transition = playTransition();
        dispatcher.onProgressCheckpoint(wrap(transition),
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000, 30000, TRACK_URN),
                                                                     Urn.forTrack(101L)));

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(AdPlaybackSessionEvent.EVENT_KIND_PLAY);
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

    private void assertCommonEventData(PlaybackStateTransition stateTransition, AdPlaybackSessionEvent playbackSessionEvent) {
        assertThat(playbackSessionEvent.getEventArgs().getProtocol()).isEqualTo(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL));
        assertThat(playbackSessionEvent.trackSourceInfo).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.getEventArgs().getUuid()).isNotEmpty();
        assertThat(playbackSessionEvent.getEventArgs().getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getEventArgs().getDuration()).isEqualTo(DURATION);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
    }
}
