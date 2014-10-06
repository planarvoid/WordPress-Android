package com.soundcloud.android.analytics.localytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderPlaybackSessionEventTest {

    private static final String LISTEN = "Listen";
    private static final int DURATION = 100000;
    private static final Urn USER_URN = Urn.forUser(123L);
    private static final Urn TRACK_URN = Urn.forTrack(1L);
    private static final PropertySet TRACK_DATA = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, "allow", DURATION);

    private LocalyticsAnalyticsProvider localyticsProvider;

    @Mock
    private LocalyticsSession localyticsSession;

    @Mock
    private PlaybackStateProvider playbackServiceStateWrapper;

    @Captor
    private ArgumentCaptor<Map<String, String>> stopEventAttributes;

    private TrackSourceInfo trackSourceInfo;
    private PlaybackSessionEvent startEvent, stopEvent;

    @Before
    public void setUp() throws CreateModelException {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, playbackServiceStateWrapper);
        trackSourceInfo = new TrackSourceInfo(Screen.YOUR_LIKES.get(), true);


        long startTime = System.currentTimeMillis();
        long stopTime = startTime + 1000L;

        startEvent = PlaybackSessionEvent.forPlay(TRACK_DATA, USER_URN, "hls", trackSourceInfo, 0, startTime);
        stopEvent = createStopEventWithStopTimeAndDuration(stopTime, DURATION);
    }

    @Test
    public void shouldNotTrackPlaybackStartEvents() throws CreateModelException {
        localyticsProvider.handlePlaybackSessionEvent(startEvent);
        verifyZeroInteractions(localyticsSession);
    }

    @Test
    public void playbackEventDataForStopEventShouldContainBasicAttributes() throws CreateModelException {
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq("Listen"), stopEventAttributes.capture());

        expect(stopEventAttributes.getValue().get("context")).toEqual(Screen.YOUR_LIKES.get());
        expect(stopEventAttributes.getValue().get("play_duration_ms")).toEqual("1000");
        expect(stopEventAttributes.getValue().get("track_length_ms")).toEqual(String.valueOf(DURATION));
        expect(stopEventAttributes.getValue().get("track_id")).toEqual(String.valueOf(TRACK_URN.getNumericId()));
    }

    @Test
    public void playbackEventDataForStopEventShouldContainAttributesForTrackBelongingToLoggedInUsersPlaylist() {
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 0, Urn.forUser(123L));
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("set_id")).toEqual("123");
        expect(stopEventAttributes.getValue().get("set_owner")).toEqual("you");
    }

    @Test
    public void playbackEventDataForStopEventShouldContainAttributesForTrackBelongingToOtherUsersPlaylist() {
        trackSourceInfo.setOriginPlaylist(Urn.forPlaylist(123L), 0, Urn.forUser(456L));
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("set_id")).toEqual("123");
        expect(stopEventAttributes.getValue().get("set_owner")).toEqual("other");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListenedLessThan5() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithPercentListened(.04));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("<5%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened5to25With5percent() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithPercentListened(.05));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("5% to 25%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened5to25With25percent() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithPercentListened(.25));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("5% to 25%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened25to75With26percent() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithPercentListened(.26));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("25% to 75%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened25to75With75percent() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithPercentListened(.75));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("25% to 75%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListenedGreaterThan75() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithPercentListened(.76));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual(">75%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackIntoLessThan1MinuteBucket() {
        stopEvent = createStopEventWithDuration(59999);
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("<1min");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackInto1to10MinutesBucket() {
        stopEvent = createStopEventWithDuration(60000);
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("1min to 10min");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackInto10to30MinutesBucket() {
        stopEvent = createStopEventWithDuration(600001);
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("10min to 30min");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackInto30to60MinutesBucket() {
        stopEvent = createStopEventWithDuration(1800001);
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("30min to 1hr");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackIntoMoreThan1HourBucket() {
        stopEvent = createStopEventWithDuration(3600001);
        localyticsProvider.handlePlaybackSessionEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual(">1hr");
    }

    @Test
    public void playbackEventDataForStopEventShouldAddStopReasonPause() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithWithReason(PlaybackSessionEvent.STOP_REASON_PAUSE));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("stop_reason")).toEqual("pause");
    }

    @Test
    public void playbackEventDataForStopEventShouldAddStopReasonTrackFinished() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithWithReason(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("stop_reason")).toEqual("track_finished");
    }

    @Test
    public void playbackEventDataForStopEventShouldAddStopReasonEndOfContent() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithWithReason(PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("stop_reason")).toEqual("end_of_content");
    }

    @Test
    public void playbackEventDataForStopEventShouldAddStopReasonContextChange() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithWithReason(PlaybackSessionEvent.STOP_REASON_NEW_QUEUE));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("stop_reason")).toEqual("context_change");
    }

    @Test
    public void playbackEventDataForStopEventShouldAddStopReasonBuffering() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithWithReason(PlaybackSessionEvent.STOP_REASON_BUFFERING));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("stop_reason")).toEqual("buffering");
    }

    @Test
    public void playbackEventDataForStopEventShouldAddStopReasonSkip() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithWithReason(PlaybackSessionEvent.STOP_REASON_SKIP));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("stop_reason")).toEqual("skip");
    }

    @Test
    public void playbackEventDataForStopEventShouldAddStopReasonError() {
        localyticsProvider.handlePlaybackSessionEvent(createStopEventWithWithReason(PlaybackSessionEvent.STOP_REASON_ERROR));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("stop_reason")).toEqual("playback_error");
    }

    private PlaybackSessionEvent createStopEventWithPercentListened(double percent) {
        return PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, "hls", trackSourceInfo, startEvent,
                PlaybackSessionEvent.STOP_REASON_BUFFERING, 0L,
                (long) (startEvent.getTimeStamp() + DURATION * percent));
    }

    private PlaybackSessionEvent createStopEventWithDuration(int duration) {
        return createStopEventWithStopTimeAndDuration(System.currentTimeMillis(), duration);
    }

    private PlaybackSessionEvent createStopEventWithStopTimeAndDuration(long stopTime, int duration) {
        return PlaybackSessionEvent.forStop(
                TestPropertySets.expectedTrackForAnalytics(TRACK_URN, "allow", duration),
                USER_URN, "hls", trackSourceInfo, startEvent,
                PlaybackSessionEvent.STOP_REASON_BUFFERING, 0L, stopTime);
    }

    private PlaybackSessionEvent createStopEventWithWithReason(int reason) {
        return PlaybackSessionEvent.forStop(TRACK_DATA, USER_URN, "hls", trackSourceInfo, startEvent, reason, 0);
    }
}
