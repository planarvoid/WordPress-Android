package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionAnalyticsControllerTest {

    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final Urn USER_URN = Urn.forUser(2L);
    private static final long PROGRESS = 123L;
    private static final int DURATION = 456;

    private PlaybackSessionAnalyticsController analyticsController;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private TrackOperations trackOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private AdsOperations adsOperations;

    @Before
    public void setUp() throws Exception {
        PropertySet track = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, "allow", DURATION);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.just(track));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);

        analyticsController = new PlaybackSessionAnalyticsController(
                eventBus, trackOperations, accountOperations, playQueueManager, adsOperations);
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
        expect(playbackSessionEvent.isStopEvent()).toBeFalse();
    }

    private void expectCommonAudioEventData(Playa.StateTransition stateTransition, PlaybackSessionEvent playbackSessionEvent) {
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_TRACK_URN)).toEqual(TRACK_URN.toString());
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_USER_URN)).toEqual(USER_URN.toString());
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_PROTOCOL)).toEqual(stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL));
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.getProgress()).toEqual(PROGRESS);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
    }

    @Test
    public void stateChangeEventForPlayingAudioAdPublishesAdSpecificPlayEvent() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);

        Playa.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = (PlaybackSessionEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        // track properties
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        expect(playbackSessionEvent.isStopEvent()).toBeFalse();
        // ad specific properties
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_AD_URN)).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_MONETIZED_URN)).toEqual(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
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
        expect(playbackSessionEvent.hasTrackFinished()).toBeTrue();
        // ad specific properties
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_AD_URN)).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_MONETIZED_URN)).toEqual(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
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
        when(trackOperations.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack)));

        publishPlayingEventForTrack(TRACK_URN);
        publishPlayingEventForTrack(nextTrack);

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        expect(events).toNumber(3);
        expect(events.get(1)).toBeInstanceOf(PlaybackSessionEvent.class);
        expect(((PlaybackSessionEvent) events.get(1)).isStopEvent()).toBeTrue();
        expect(events.get(1).get(PlaybackSessionEvent.KEY_TRACK_URN)).toEqual(TRACK_URN.toString());
    }

    @Test
    public void shouldPublishStopEventWithAdDataWhenUserSkipsBetweenTracksManually() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        final Urn nextTrack = Urn.forTrack(456L);
        when(trackOperations.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack)));

        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(audioAd);

        publishPlayingEventForTrack(TRACK_URN);

        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        publishPlayingEventForTrack(nextTrack);

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        expect(events).toNumber(3);
        expect(events.get(1)).toBeInstanceOf(PlaybackSessionEvent.class);
        expect(((PlaybackSessionEvent) events.get(1)).isStopEvent()).toBeTrue();
        expect(events.get(1).get(PlaybackSessionEvent.KEY_TRACK_URN)).toEqual(TRACK_URN.toString());
        expect(events.get(1).get(PlaybackSessionEvent.KEY_AD_URN)).toEqual(audioAd.get(AdProperty.AD_URN));
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
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_TRACK_URN)).toEqual(TRACK_URN.toString());
        expect(playbackSessionEvent.get(PlaybackSessionEvent.KEY_USER_URN)).toEqual(USER_URN.toString());
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.isStopEvent()).toBeTrue();
        expect(playbackSessionEvent.getProgress()).toEqual(PROGRESS);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackSessionEvent.getStopReason()).toEqual(stopReason);
        expect(playbackSessionEvent.getDuration()).toEqual(DURATION);
    }
}
