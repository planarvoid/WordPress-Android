package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
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
    private static final Urn LOGGED_IN_USER_URN = Urn.forUser(2L);
    private static final Urn CREATOR_URN = Urn.forUser(3L);
    private static final long PROGRESS = PlaybackSessionEvent.FIRST_PLAY_MAX_PROGRESS + 1;
    private static final long DURATION = 2001L;

    private PlaybackSessionAnalyticsController analyticsController;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private TrackRepository trackRepository;
    @Mock private AccountOperations accountOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private AdsOperations adsOperations;
    @Mock private AppboyPlaySessionState appboyPlaySessionState;
    @Mock private StopReasonProvider stopReasonProvider;

    @Before
    public void setUp() throws Exception {
        PropertySet track = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, CREATOR_URN, "allow", DURATION);
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(track));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource("stream"));
        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(false);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER_URN);

        analyticsController = new PlaybackSessionAnalyticsController(
                eventBus, trackRepository, accountOperations, playQueueManager, adsOperations, appboyPlaySessionState, stopReasonProvider);
    }

    @Test
    public void stateChangeEventDoesNotPublishEventWithInvalidTrackUrn() throws Exception {
        analyticsController.onStateTransition(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, Urn.NOT_SET));
        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEvent() throws Exception {
        Player.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
    }

    @Test
    public void stateChangeEventPublishesSkipEvent() {
        publishPlayingEvent();
        final Urn trackUrn2 = Urn.forTrack(2L);
        when(trackRepository.track(trackUrn2)).thenReturn(Observable.<PropertySet>never());
        Player.StateTransition playEvent2 = publishPlayingEventForTrack(trackUrn2);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent2, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isTrue();
        assertThat(playbackSessionEvent.getStopReason()).isEqualTo(PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEventForLocalStoragePlayback() throws Exception {
        final Player.StateTransition startEvent = new Player.StateTransition(
                Player.PlayerState.PLAYING, Player.Reason.NONE, TRACK_URN, PROGRESS, DURATION);
        startEvent.addExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        startEvent.addExtraAttribute(Player.StateTransition.EXTRA_URI, "file://some/local/uri");

        analyticsController.onStateTransition(startEvent);

        Player.StateTransition playEvent = startEvent;

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isOfflineTrack()).isTrue();
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStateDoesNotPublishTwoConsecutivePlayEvents() throws Exception {
        Player.StateTransition playEvent = publishPlayingEvent();
        Player.StateTransition nextEvent = new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, TRACK_URN, PROGRESS + 1, DURATION);
        analyticsController.onStateTransition(nextEvent);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
    }

    private void expectCommonAudioEventData(Player.StateTransition stateTransition, PlaybackSessionEvent playbackSessionEvent) {
        assertThat(playbackSessionEvent.getTrackUrn()).isEqualTo(TRACK_URN);
        assertThat(playbackSessionEvent.getCreatorUrn()).isEqualTo(CREATOR_URN);
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_LOGGED_IN_USER_URN)).isEqualTo(LOGGED_IN_USER_URN.toString());
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_PROTOCOL)).isEqualTo(stateTransition.getExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL));
        assertThat(playbackSessionEvent.getTrackSourceInfo()).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
    }

    @Test
    public void stateChangeEventForPlayingAudioAdPublishesAdSpecificPlayEvent() throws Exception {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN, audioAd));

        Player.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        // track properties
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
        // ad specific properties
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn());
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.getMonetizableTrackUrn().toString());
    }

    @Test
    public void stateChangeEventForFinishPlayingAudioAdPublishesAdSpecificStopEvent() throws Exception {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN, audioAd));
        when(playQueueManager.hasNextItem()).thenReturn(true);


        publishPlayingEvent();
        publishStopEvent(Player.PlayerState.BUFFERING, Player.Reason.NONE); // make sure intermediate events don't matter
        publishPlayingEvent();
        final Player.StateTransition stateTransition = publishStopEvent(Player.PlayerState.IDLE, Player.Reason.TRACK_COMPLETE, PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);

        when(stopReasonProvider.fromTransition(stateTransition)).thenReturn(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
        assertThat(playbackSessionEvent.hasTrackFinished()).isTrue();
        // ad specific properties
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn());
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.getMonetizableTrackUrn().toString());
    }

    @Test
    public void stateChangeEventForPlayingAudioAdOnPromotedContentDoesntHavePromotedInfo() throws Exception {
        final Urn promoter = Urn.forUser(83L);
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        final PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123", TRACK_URN, Optional.of(promoter), Arrays.asList("url")));

        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);
        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(true);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN, audioAd));

        publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        // ad specific properties
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn());
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.getMonetizableTrackUrn().toString());
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_MONETIZATION_TYPE)).isEqualTo("audio_ad");
    }

    @Test
    public void stateChangeToPlayEventForPlayingPromotedTrackIncludesPromotedInfo() {
        Urn promoter = Urn.forUser(83L);
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123", TRACK_URN, Optional.of(promoter), Arrays.asList("url")));

        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        Player.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isEqualTo("ad:urn:123");
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_PROMOTER_URN)).isEqualTo(promoter.toString());
    }

    @Test
    public void stateChangeToPlayEventForPlayingNonPromotedTrackAlongWithPromotedContentDoesntIncludePromotedInfo() {
        Urn promoter = Urn.forUser(83L);
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123", Urn.forPlaylist(421L), Optional.of(promoter), Arrays.asList("url")));

        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        Player.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_AD_URN)).isNull();
        assertThat(playbackSessionEvent.get(AdTrackingKeys.KEY_PROMOTER_URN)).isNull();
    }

    @Test
    public void promotedInfoSetsHasPlayedOnPlayEvent() {
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123", TRACK_URN, Optional.<Urn>absent(), Arrays.asList("url")));

        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        publishPlayingEvent();

        assertThat(source.isFromPromotedItem()).isTrue();
        assertThat(source.getPromotedSourceInfo().isFirstPlay()).isFalse();
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopReasonFromProvider() throws Exception {
        publishPlayingEvent();
        publishStopEvent(Player.PlayerState.IDLE, Player.Reason.NONE, PlaybackSessionEvent.STOP_REASON_PAUSE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_PAUSE);
    }

    // the player does not send stop events when skipping, so we have to materialize these
    @Test
    public void shouldPublishStopEventWhenUserSkipsBetweenTracksManually() {
        final Urn nextTrack = Urn.forTrack(456L);
        when(trackRepository.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack, CREATOR_URN)));

        publishPlayingEventForTrack(TRACK_URN);
        publishPlayingEventForTrack(nextTrack);

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(events.get(1)).isInstanceOf(PlaybackSessionEvent.class);
        assertThat(((PlaybackSessionEvent) events.get(1)).isStopEvent()).isTrue();
        assertThat(((PlaybackSessionEvent) events.get(1)).getTrackUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void shouldPublishStopEventWithAdDataWhenUserSkipsBetweenTracksManually() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        final Urn nextTrack = Urn.forTrack(456L);
        when(trackRepository.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack, CREATOR_URN)));

        when(adsOperations.isCurrentItemAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN, audioAd));

        publishPlayingEventForTrack(TRACK_URN);

        when(adsOperations.isCurrentItemAudioAd()).thenReturn(false);
        publishPlayingEventForTrack(nextTrack);

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(3);
        assertThat(events.get(1)).isInstanceOf(PlaybackSessionEvent.class);
        assertThat(((PlaybackSessionEvent) events.get(1)).isStopEvent()).isTrue();
        assertThat(((PlaybackSessionEvent) events.get(1)).getTrackUrn()).isEqualTo(TRACK_URN);
        assertThat(events.get(1).get(AdTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn());
    }

    protected Player.StateTransition publishPlayingEvent() {
        return publishPlayingEventForTrack(TRACK_URN);
    }

    protected Player.StateTransition publishPlayingEventForTrack(Urn trackUrn) {
        final Player.StateTransition startEvent = new Player.StateTransition(
                Player.PlayerState.PLAYING, Player.Reason.NONE, trackUrn, PROGRESS, DURATION);
        startEvent.addExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");

        analyticsController.onStateTransition(startEvent);

        return startEvent;
    }

    protected Player.StateTransition publishStopEvent(Player.PlayerState newState, Player.Reason reason) {
        final Player.StateTransition stopEvent = new Player.StateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        analyticsController.onStateTransition(stopEvent);
        return stopEvent;
    }

    private Player.StateTransition publishStopEvent(Player.PlayerState newState, Player.Reason reason, int stopReason) {
        final Player.StateTransition stopEvent = new Player.StateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        when(stopReasonProvider.fromTransition(stopEvent)).thenReturn(stopReason);
        analyticsController.onStateTransition(stopEvent);
        return stopEvent;
    }

    protected void verifyStopEvent(int stopReason) {
        final PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(playbackSessionEvent.getTrackUrn()).isEqualTo(TRACK_URN);
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_LOGGED_IN_USER_URN)).isEqualTo(LOGGED_IN_USER_URN.toString());
        assertThat(playbackSessionEvent.getTrackSourceInfo()).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.isStopEvent()).isTrue();
        assertThat(playbackSessionEvent.getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
        assertThat(playbackSessionEvent.getStopReason()).isEqualTo(stopReason);
        assertThat(playbackSessionEvent.getDuration()).isEqualTo(DURATION);
    }
}
