package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

public class PlaybackSessionAnalyticsControllerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn USER_URN = Urn.forUser(2L);
    private static final long PROGRESS = PlaybackSessionEvent.FIRST_PLAY_MAX_PROGRESS + 1;
    private static final long DURATION = 2001L;

    private PlaybackSessionAnalyticsController analyticsController;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private TrackRepository trackRepository;
    @Mock private AccountOperations accountOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private AdsOperations adsOperations;

    @Before
    public void setUp() throws Exception {
        PropertySet track = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, "allow", DURATION);
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(track));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource("stream"));
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);

        analyticsController = new PlaybackSessionAnalyticsController(
                eventBus, trackRepository, accountOperations, playQueueManager, adsOperations);
    }

    @Test
    public void stateChangeEventDoesNotPublishEventWithInvalidTrackUrn() throws Exception {
        analyticsController.onStateTransition(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, Urn.NOT_SET));
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEvent() throws Exception {
        Playa.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStateDoesNotPublishTwoConsecutivePlayEvents() throws Exception {
        Playa.StateTransition playEvent = publishPlayingEvent();
        Playa.StateTransition nextEvent = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN, PROGRESS + 1, DURATION);
        analyticsController.onStateTransition(nextEvent);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
    }

    private void expectCommonAudioEventData(Playa.StateTransition stateTransition, PlaybackSessionEvent playbackSessionEvent) {
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_TRACK_URN)).isEqualTo(TRACK_URN.toString());
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_USER_URN)).isEqualTo(USER_URN.toString());
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_PROTOCOL)).isEqualTo(stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL));
        assertThat(playbackSessionEvent.getTrackSourceInfo()).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
    }

    @Test
    public void stateChangeEventForPlayingAudioAdPublishesAdSpecificPlayEvent() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);

        Playa.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        // track properties
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
        // ad specific properties
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.get(AdProperty.AUDIO_AD_URN));
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
    }

    @Test
    public void stateChangeEventForFinishPlayingAudioAdPublishesAdSpecificStopEvent() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);
        when(playQueueManager.hasNextTrack()).thenReturn(true);

        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.BUFFERING, Playa.Reason.NONE); // make sure intermediate events don't matter
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
        assertThat(playbackSessionEvent.hasTrackFinished()).isTrue();
        // ad specific properties
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.get(AdProperty.AUDIO_AD_URN));
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
    }

    @Test
    public void stateChangeToPlayEventForPlayingPromotedTrackIncludesPromotedInfo() {
        Urn promoter = Urn.forUser(83L);
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.of(promoter), Arrays.asList("url")));
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        Playa.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo("ad:urn:123");
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_PROMOTER_URN)).isEqualTo(promoter.toString());
    }

    @Test
    public void promotedInfoIsClearedOnPlayEvent() {
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123", Urn.forTrack(123L), Optional.<Urn>absent(), Arrays.asList("url")));
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        publishPlayingEvent();

        assertThat(source.isFromPromotedItem()).isFalse();
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForBuffering() throws Exception {
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForPause() throws Exception {
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.NONE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_PAUSE);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForTrackFinished() throws Exception {
        publishPlayingEvent();
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForQueueFinished() throws Exception {
        publishPlayingEvent();
        when(playQueueManager.hasNextTrack()).thenReturn(false);
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForError() throws Exception {
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_ERROR);
    }

    // the player does not send stop events when skipping, so we have to materialize these
    @Test
    public void shouldPublishStopEventWhenUserSkipsBetweenTracksManually() {
        final Urn nextTrack = Urn.forTrack(456L);
        when(trackRepository.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack)));

        publishPlayingEventForTrack(TRACK_URN);
        publishPlayingEventForTrack(nextTrack);

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(events.get(1)).isInstanceOf(PlaybackSessionEvent.class);
        assertThat(((PlaybackSessionEvent) events.get(1)).isStopEvent()).isTrue();
        assertThat(events.get(1).get(PlaybackSessionEvent.KEY_TRACK_URN)).isEqualTo(TRACK_URN.toString());
    }

    @Test
    public void shouldPublishStopEventWithAdDataWhenUserSkipsBetweenTracksManually() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        final Urn nextTrack = Urn.forTrack(456L);
        when(trackRepository.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack)));

        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);

        publishPlayingEventForTrack(TRACK_URN);

        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        publishPlayingEventForTrack(nextTrack);

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(events.get(1)).isInstanceOf(PlaybackSessionEvent.class);
        assertThat(((PlaybackSessionEvent) events.get(1)).isStopEvent()).isTrue();
        assertThat(events.get(1).get(PlaybackSessionEvent.KEY_TRACK_URN)).isEqualTo(TRACK_URN.toString());
        assertThat(events.get(1).get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.get(AdProperty.AUDIO_AD_URN));
    }

    protected Playa.StateTransition publishPlayingEvent() {
        return publishPlayingEventForTrack(TRACK_URN);
    }

    protected Playa.StateTransition publishPlayingEventForTrack(Urn trackUrn) {
        final Playa.StateTransition startEvent = new Playa.StateTransition(
                Playa.PlayaState.PLAYING, Playa.Reason.NONE, trackUrn, PROGRESS, DURATION);
        startEvent.addExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");

        analyticsController.onStateTransition(startEvent);

        return startEvent;
    }

    protected Playa.StateTransition publishStopEvent(Playa.PlayaState newState, Playa.Reason reason) {
        final Playa.StateTransition stopEvent = new Playa.StateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        analyticsController.onStateTransition(stopEvent);
        return stopEvent;
    }

    protected void verifyStopEvent(int stopReason) {
        final PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_TRACK_URN)).isEqualTo(TRACK_URN.toString());
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_USER_URN)).isEqualTo(USER_URN.toString());
        assertThat(playbackSessionEvent.getTrackSourceInfo()).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.isStopEvent()).isTrue();
        assertThat(playbackSessionEvent.getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
        assertThat(playbackSessionEvent.getStopReason()).isEqualTo(stopReason);
        assertThat(playbackSessionEvent.getDuration()).isEqualTo(DURATION);
    }
}
