package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

public class TrackSessionAnalyticsDispatcherTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn CREATOR_URN = Urn.forUser(3L);
    private static final long PROGRESS = 1001L;
    private static final long DURATION = 2001L;
    private static final String UUID = "blah-123";

    private PlaybackAnalyticsDispatcher dispatcher;

    @Mock private TrackRepository trackRepository;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AppboyPlaySessionState appboyPlaySessionState;
    @Mock private StopReasonProvider stopReasonProvider;
    @Mock private UuidProvider uuidProvider;

    private TrackSourceInfo trackSourceInfo = new TrackSourceInfo(Screen.STREAM.get(), true);

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        PropertySet track = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, CREATOR_URN, "allow", DURATION);
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(track));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource("stream"));
        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(false);
        when(uuidProvider.getRandomUuid()).thenReturn(UUID);

        dispatcher = new TrackSessionAnalyticsDispatcher(
                eventBus, trackRepository, playQueueManager, appboyPlaySessionState,
                stopReasonProvider, uuidProvider);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEvent() {
        PlaybackStateTransition playEvent = playTransition();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
    }

    @Test
    public void stateChangeEventPublishesSkipEvent() {
        PlaybackStateTransition playEvent = playTransition();
        skipTransition();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isTrue();
        assertThat(playbackSessionEvent.getStopReason()).isEqualTo(PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEventForLocalStoragePlayback() {
        final PlaybackStateTransition startEvent = addStateExtras(new PlaybackStateTransition(
                PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN, PROGRESS, DURATION));
        startEvent.addExtraAttribute(PlaybackStateTransition.EXTRA_URI, "file://some/local/uri");

        dispatcher.onPlayTransition(startEvent, true);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(startEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isOfflineTrack()).isTrue();
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStateDoesNotPublishTwoConsecutivePlayEvents() {
        PlaybackStateTransition playEvent = playTransition();

        dispatcher.onPlayTransition(playEvent, false);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
    }

    private void expectCommonAudioEventData(PlaybackStateTransition stateTransition,
                                            PlaybackSessionEvent playbackSessionEvent) {
        assertThat(playbackSessionEvent.getTrackUrn()).isEqualTo(TRACK_URN);
        assertThat(playbackSessionEvent.getCreatorUrn()).isEqualTo(CREATOR_URN);
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_PROTOCOL)).isEqualTo(stateTransition.getExtraAttribute(
                PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL));
        assertThat(playbackSessionEvent.getTrackSourceInfo()).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.getUUID()).isEqualTo(UUID);
        assertThat(playbackSessionEvent.getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
    }

    @Test
    public void stateChangeToPlayEventForPlayingPromotedTrackIncludesPromotedInfo() {
        Urn promoter = Urn.forUser(83L);
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123",
                                                            TRACK_URN,
                                                            Optional.of(promoter),
                                                            Arrays.asList("url")));

        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        PlaybackStateTransition playEvent = playTransition();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo("ad:urn:123");
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_PROMOTER_URN)).isEqualTo(promoter.toString());
    }

    @Test
    public void stateChangeToPlayEventForPlayingNonPromotedTrackAlongWithPromotedContentDoesntIncludePromotedInfo() {
        Urn promoter = Urn.forUser(83L);
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123",
                                                            Urn.forPlaylist(421L),
                                                            Optional.of(promoter),
                                                            Arrays.asList("url")));

        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        PlaybackStateTransition playEvent = playTransition();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isNull();
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_PROMOTER_URN)).isNull();
    }

    @Test
    public void promotedInfoSetsHasPlayedOnPlayEvent() {
        PlaySessionSource source = new PlaySessionSource("stream");
        source.setPromotedSourceInfo(new PromotedSourceInfo("ad:urn:123",
                                                            TRACK_URN,
                                                            Optional.<Urn>absent(),
                                                            Arrays.asList("url")));

        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(true);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(source);

        playTransition();

        assertThat(source.isFromPromotedItem()).isTrue();
        assertThat(source.getPromotedSourceInfo().isPlaybackStarted()).isTrue();
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopReasonFromProvider() {
        playTransition();
        stopTransition(PlaybackState.IDLE, PlayStateReason.NONE, PlaybackSessionEvent.STOP_REASON_PAUSE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_PAUSE);
    }

    // the player does not send stop events when skipping, so we have to materialize these
    @Test
    public void shouldPublishStopEventWhenUserSkipsBetweenTracksManually() {
        playTransitionForTrack(TRACK_URN);
        skipTransition();

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(2);
        assertThat(events.get(1)).isInstanceOf(PlaybackSessionEvent.class);
        assertThat(((PlaybackSessionEvent) events.get(1)).isStopEvent()).isTrue();
        assertThat(((PlaybackSessionEvent) events.get(1)).getTrackUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void shouldPublishCheckpointEvent() {
        final PlaybackStateTransition transition = playTransition();

        dispatcher.onProgressCheckpoint(transition,
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000L, 30000L),
                                                                     transition.getUrn()));

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(2);
        assertThat(((PlaybackSessionEvent) events.get(0)).isPlayEvent()).isTrue();
        assertThat(((PlaybackSessionEvent) events.get(1)).isCheckpointEvent()).isTrue();
        assertThat(((PlaybackSessionEvent) events.get(1)).getProgress()).isEqualTo(3000L);
    }

    @Test
    public void shouldNotPublishCheckpointEventIfNotPlaying() {
        final PlaybackStateTransition transition = playTransition();
        stopTransition(PlaybackState.IDLE, PlayStateReason.NONE, PlaybackSessionEvent.STOP_REASON_PAUSE);

        dispatcher.onProgressCheckpoint(transition,
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000L, 30000L),
                                                                     transition.getUrn()));

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(2);
        assertThat(((PlaybackSessionEvent) events.get(0)).isPlayEvent()).isTrue();
        assertThat(((PlaybackSessionEvent) events.get(1)).isStopEvent()).isTrue();
    }

    @Test
    public void shouldNotPublishCheckpointEventIfProgressEventIsNotForPlayingItem() {
        final PlaybackStateTransition transition = playTransition();

        dispatcher.onProgressCheckpoint(transition,
                                        PlaybackProgressEvent.create(new PlaybackProgress(3000L, 30000L),
                                                                     Urn.forTrack(101L)));

        assertThat(((PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING)).isPlayEvent()).isTrue();
    }

    protected PlaybackStateTransition playTransition() {
        return playTransitionForTrack(TRACK_URN);
    }

    protected PlaybackStateTransition playTransitionForTrack(Urn trackUrn) {
        final PlaybackStateTransition startEvent = new PlaybackStateTransition(
                PlaybackState.PLAYING, PlayStateReason.NONE, trackUrn, PROGRESS, DURATION);
        dispatcher.onPlayTransition(addStateExtras(startEvent), true);

        return startEvent;
    }

    private PlaybackStateTransition stopTransition(PlaybackState newState, PlayStateReason reason, int stopReason) {
        final PlaybackStateTransition stopEvent = new PlaybackStateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        when(stopReasonProvider.fromTransition(addStateExtras(stopEvent))).thenReturn(stopReason);
        dispatcher.onStopTransition(stopEvent, false);

        return stopEvent;
    }

    private PlaybackStateTransition skipTransition() {
        final PlaybackStateTransition skipEvent = new PlaybackStateTransition(
                PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN, PROGRESS, DURATION);

        dispatcher.onSkipTransition(addStateExtras(skipEvent));

        return skipEvent;
    }

    protected void verifyStopEvent(int stopReason) {
        final PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(playbackSessionEvent.getTrackUrn()).isEqualTo(TRACK_URN);
        assertThat(playbackSessionEvent.getTrackSourceInfo()).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.isStopEvent()).isTrue();
        assertThat(playbackSessionEvent.getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
        assertThat(playbackSessionEvent.getStopReason()).isEqualTo(stopReason);
        assertThat(playbackSessionEvent.getDuration()).isEqualTo(DURATION);
    }

    private PlaybackStateTransition addStateExtras(PlaybackStateTransition startEvent) {
        return startEvent.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls")
                         .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, "skippy")
                         .addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE, "3g");
    }

}
