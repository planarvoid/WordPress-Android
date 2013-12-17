package com.soundcloud.android.analytics.localytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderTest {
    public static final String LISTEN = "Listen";
    public static final int DURATION = 100000;
    private LocalyticsAnalyticsProvider localyticsProvider;
    @Mock
    private LocalyticsSession localyticsSession;
    @Captor
    private ArgumentCaptor<Map<String, String>> stopEventAttributes;

    private Track track;
    private TrackSourceInfo trackSourceInfo;
    private PlaybackEventData startEvent, stopEvent;

    @Before
    public void setUp() throws CreateModelException {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession);
        track = TestHelper.getModelFactory().createModel(Track.class);
        trackSourceInfo = new TrackSourceInfo(Screen.YOUR_LIKES.get(), true);

        track.genre = "clown step";
        track.duration = DURATION;

        long startTime = System.currentTimeMillis();
        long stopTime = startTime + 1000L;

        startEvent = PlaybackEventData.forPlay(track, 123L, trackSourceInfo, startTime);
        stopEvent = PlaybackEventData.forStop(track, 123L, trackSourceInfo, startEvent, stopTime);
    }

    @Test
    public void shouldOpenSession(){
        localyticsProvider.openSession();
        verify(localyticsSession).open();
    }

    @Test
    public void shouldUploadDataWhenClosingSession(){
        localyticsProvider.closeSession();
        verify(localyticsSession).upload();
    }

    @Test
    public void shouldCloseSession(){
        localyticsProvider.closeSession();
        verify(localyticsSession).close();
    }

    @Test
    public void shouldTrackScreenWithGivenName() {
        localyticsProvider.trackScreen("main:explore");
        verify(localyticsSession).tagScreen(eq("main:explore"));
    }

    @Test
    public void shouldNotTrackPlaybackStartEvents() throws CreateModelException {
        localyticsProvider.trackPlaybackEvent(startEvent);
        verifyZeroInteractions(localyticsSession);
    }

    @Test
    public void playbackEventDataForStopEventShouldContainBasicAttributes() throws CreateModelException {
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq("Listen"), stopEventAttributes.capture());

        expect(stopEventAttributes.getValue().get("context")).toEqual(Screen.YOUR_LIKES.get());
        expect(stopEventAttributes.getValue().get("duration_ms")).toEqual("1000");
        expect(stopEventAttributes.getValue().get("track_length_ms")).toEqual(String.valueOf(track.duration));
        expect(stopEventAttributes.getValue().get("track_id")).toEqual(String.valueOf(track.getId()));
        expect(stopEventAttributes.getValue().get("tag")).toEqual(String.valueOf(track.getGenreOrTag()));
    }

    @Test
    public void playbackEventDataForStopEventShouldNotContainNullTag() throws CreateModelException {
        track.genre = null;
        track.tag_list = null;
        stopEvent = PlaybackEventData.forStop(track, 123L, trackSourceInfo, startEvent);
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq("Listen"), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().containsKey("tag")).toBeFalse();
    }

    @Test
    public void playbackEventDataForStopEventShouldContainAttributesForTrackBelongingToLoggedInUsersPlaylist() {
        trackSourceInfo.setOriginPlaylist(123L, 0, 123L);
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("set_id")).toEqual("123");
        expect(stopEventAttributes.getValue().get("set_owner")).toEqual("you");
    }

    @Test
    public void playbackEventDataForStopEventShouldContainAttributesForTrackBelongingToOtherUsersPlaylist() {
        trackSourceInfo.setOriginPlaylist(123L, 0, 456L);
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("set_id")).toEqual("123");
        expect(stopEventAttributes.getValue().get("set_owner")).toEqual("other");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListenedLessThan5() {
        localyticsProvider.trackPlaybackEvent(createStopEventWithPercentListened(.04));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("<5%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened5to25With5percent() {
        localyticsProvider.trackPlaybackEvent(createStopEventWithPercentListened(.05));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("5% to 25%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened5to25With25percent() {
        localyticsProvider.trackPlaybackEvent(createStopEventWithPercentListened(.25));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("5% to 25%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened25to75With26percent() {
        localyticsProvider.trackPlaybackEvent(createStopEventWithPercentListened(.26));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("25% to 75%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListened25to75With75percent() {
        localyticsProvider.trackPlaybackEvent(createStopEventWithPercentListened(.75));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual("25% to 75%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackPercentListenedGreaterThan75() {
        localyticsProvider.trackPlaybackEvent(createStopEventWithPercentListened(.76));
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("percent_listened")).toEqual(">75%");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackIntoLessThan1MinuteBucket() {
        track.duration = 59999;
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("<1min");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackInto1to10MinutesBucket() {
        track.duration = 60000;
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("1min to 10min");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackInto10to30MinutesBucket() {
        track.duration = 600001;
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("10min to 30min");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackInto30to60MinutesBucket() {
        track.duration = 1800001;
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual("30min to 1hr");
    }

    @Test
    public void playbackEventDataForStopEventShouldTrackLengthOfTrackIntoMoreThan1HourBucket() {
        track.duration = 3600001;
        localyticsProvider.trackPlaybackEvent(stopEvent);
        verify(localyticsSession).tagEvent(eq(LISTEN), stopEventAttributes.capture());
        expect(stopEventAttributes.getValue().get("track_length_bucket")).toEqual(">1hr");
    }

    private PlaybackEventData createStopEventWithPercentListened(double percent) {
        return PlaybackEventData.forStop(track, 123L, trackSourceInfo, startEvent,
                (long) (startEvent.getTimeStamp() + DURATION * percent));
    }

}
