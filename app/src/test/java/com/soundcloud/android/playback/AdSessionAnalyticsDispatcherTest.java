package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableTrackingKeys;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import java.util.List;

public class AdSessionAnalyticsDispatcherTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn LOGGED_IN_USER_URN = Urn.forUser(2L);
    private static final Urn CREATOR_URN = Urn.forUser(3L);
    private static final long PROGRESS = PlaybackSessionEvent.FIRST_PLAY_MAX_PROGRESS + 1;
    private static final long DURATION = 2001L;
    private static final String UUID = "blah-123";

    private AdSessionAnalyticsDispatcher dispatcher;

    @Mock private TrackRepository trackRepository;
    @Mock private AdsOperations adsOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private AppboyPlaySessionState appboyPlaySessionState;
    @Mock private StopReasonProvider stopReasonProvider;
    @Mock private UuidProvider uuidProvider;

    private TestEventBus eventBus = new TestEventBus();
    private DateProvider dateProvider = new TestDateProvider();

    @Before
    public void setUp() throws Exception {
        PropertySet track = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, CREATOR_URN, "allow", DURATION);
        when(trackRepository.track(TRACK_URN)).thenReturn(Observable.just(track));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(new PlaySessionSource("stream"));
        when(playQueueManager.isTrackFromCurrentPromotedItem(TRACK_URN)).thenReturn(false);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(LOGGED_IN_USER_URN);
        when(uuidProvider.getRandomUuid()).thenReturn(UUID);

        dispatcher = new AdSessionAnalyticsDispatcher(
                eventBus, trackRepository, accountOperations, playQueueManager, adsOperations, stopReasonProvider, uuidProvider, dateProvider);
    }

    @Test
    public void quartileProgressEventsForPlayerAdsPublishesQuartileEvent() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100), Urn.forAd("dfp", "809")));

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(1);
        AdPlaybackSessionEvent adEvent = (AdPlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(adEvent.get(PlayableTrackingKeys.KEY_QUARTILE_TYPE)).isEqualTo("ad::first_quartile");
    }

    @Test
    public void duplicateQuartileProgressEventsAreNotPublished() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(Optional.<AdData>of(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100), Urn.forAd("dfp", "809")));
        dispatcher.onProgressEvent(PlaybackProgressEvent.create(new PlaybackProgress(25, 100), Urn.forAd("dfp", "809")));

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(1);
    }

    @Test
    public void stateChangeEventForPlayingAudioAdPublishesAdSpecificPlayEvent() throws Exception {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN, audioAd));

        PlaybackStateTransition playEvent = playTransition();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        // track properties
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        assertThat(playbackSessionEvent.isStopEvent()).isFalse();
        // ad specific properties
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.getMonetizableTrackUrn().toString());
    }

    @Test
    public void stateChangeEventForFinishPlayingAudioAdPublishesAdSpecificStopEvent() throws Exception {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN, audioAd));
        when(playQueueManager.hasNextItem()).thenReturn(true);

        playTransition();
        stopTransition(PlaybackState.BUFFERING, PlayStateReason.NONE); // make sure intermediate events don't matter
        playTransition();
        final PlaybackStateTransition stateTransition = stopTransition(PlaybackState.IDLE, PlayStateReason.PLAYBACK_COMPLETE, PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);

        when(stopReasonProvider.fromTransition(stateTransition)).thenReturn(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
        assertThat(playbackSessionEvent.hasTrackFinished()).isTrue();
        // ad specific properties
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
        assertThat(playbackSessionEvent.get(PlayableTrackingKeys.KEY_MONETIZABLE_TRACK_URN)).isEqualTo(audioAd.getMonetizableTrackUrn().toString());
    }

    @Test
    public void shouldPublishStopEventWithAdDataWhenUserSkipsBetweenTracksManually() {
        final AudioAd audioAd = AdFixtures.getAudioAd(TRACK_URN);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN, audioAd));

        playTransition();
        skipTransition();

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events).hasSize(2);
        assertThat(events.get(1)).isInstanceOf(PlaybackSessionEvent.class);
        assertThat(((PlaybackSessionEvent) events.get(1)).isStopEvent()).isTrue();
        assertThat(((PlaybackSessionEvent) events.get(1)).getTrackUrn()).isEqualTo(TRACK_URN);
        assertThat(events.get(1).get(PlayableTrackingKeys.KEY_AD_URN)).isEqualTo(audioAd.getAdUrn().toString());
    }

    private void expectCommonAudioEventData(PlaybackStateTransition stateTransition, PlaybackSessionEvent playbackSessionEvent) {
        assertThat(playbackSessionEvent.getTrackUrn()).isEqualTo(TRACK_URN);
        assertThat(playbackSessionEvent.getCreatorUrn()).isEqualTo(CREATOR_URN);
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_LOGGED_IN_USER_URN)).isEqualTo(LOGGED_IN_USER_URN.toString());
        assertThat(playbackSessionEvent.get(PlaybackSessionEvent.KEY_PROTOCOL)).isEqualTo(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL));
        assertThat(playbackSessionEvent.getTrackSourceInfo()).isSameAs(trackSourceInfo);
        assertThat(playbackSessionEvent.getUUID()).isEqualTo(UUID);
        assertThat(playbackSessionEvent.getProgress()).isEqualTo(PROGRESS);
        assertThat(playbackSessionEvent.getTimestamp()).isGreaterThan(0L);
    }

    private PlaybackStateTransition playTransition() {
        return playTransitionForTrack(TRACK_URN);
    }

    private PlaybackStateTransition playTransitionForTrack(Urn trackUrn) {
        final PlaybackStateTransition startEvent = new PlaybackStateTransition(
                PlaybackState.PLAYING, PlayStateReason.NONE, trackUrn, PROGRESS, DURATION);

        dispatcher.onPlayTransition(withExtras(startEvent), true);

        return startEvent;
    }

    private PlaybackStateTransition skipTransition() {
        final PlaybackStateTransition skipEvent = new PlaybackStateTransition(
                PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN, PROGRESS, DURATION);

        dispatcher.onSkipTransition(withExtras(skipEvent));

        return skipEvent;
    }

    private PlaybackStateTransition stopTransition(PlaybackState newState, PlayStateReason reason) {
        final PlaybackStateTransition stopEvent = new PlaybackStateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");

        dispatcher.onStopTransition(withExtras(stopEvent));

        return stopEvent;
    }

    private PlaybackStateTransition stopTransition(PlaybackState newState, PlayStateReason reason, int stopReason) {
        final PlaybackStateTransition stopEvent = new PlaybackStateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        when(stopReasonProvider.fromTransition(stopEvent)).thenReturn(stopReason);

        dispatcher.onStopTransition(withExtras(stopEvent));

        return stopEvent;
    }

    private static PlaybackStateTransition withExtras(PlaybackStateTransition transition) {
        return transition
                .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, "play er")
                .addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls")
                .addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE, "6g");
    }

    private void verifyStopEvent(int stopReason) {
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
