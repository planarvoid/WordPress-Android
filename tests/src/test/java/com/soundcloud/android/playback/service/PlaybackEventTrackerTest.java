package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.tracking.eventlogger.EventLoggerParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackEventTrackerTest {

    private static final long USER_ID = 123L;
    private static final long WAIT_TIME = 50L;

    private PlaybackEventTracker playbackEventTracker;
    private Track track;

    @Mock
    private TrackSourceInfo trackSourceInfo;
    @Mock
    private Observer<PlaybackEventData> observer;

    @Before
    public void setUp() throws Exception {
        track = TestHelper.getModelFactory().createModel(Track.class);
        playbackEventTracker = new PlaybackEventTracker();
    }

    @Test
    public void trackPlayEventPublishesPlaybackEventWithPlaybackEventData() throws Exception {
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackPlayEvent(track, trackSourceInfo, USER_ID);

        ArgumentCaptor<PlaybackEventData> captor = ArgumentCaptor.forClass(PlaybackEventData.class);
        verify(observer).onNext(captor.capture());

        PlaybackEventData playbackEventData = captor.getValue();
        expect(playbackEventData.getTrack()).toBe(track);
        expect(playbackEventData.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackEventData.getAction()).toBe(EventLoggerParams.Action.PLAY);
        expect(playbackEventData.getUserId()).toBe(USER_ID);
        expect(playbackEventData.getTimeStamp()).toBeGreaterThan(0L);
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterNoPlayEvent() throws Exception {
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent(track, trackSourceInfo, USER_ID);
        verifyZeroInteractions(observer);
    }

    @Test
    public void trackStopEventPublishesPlaybackEventWithPlaybackEventDataAfterInitialPlayEvent() throws Exception {
        playbackEventTracker.trackPlayEvent(track, trackSourceInfo, USER_ID);
        Thread.sleep(WAIT_TIME);
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent(track, trackSourceInfo, USER_ID);

        ArgumentCaptor<PlaybackEventData> captor = ArgumentCaptor.forClass(PlaybackEventData.class);
        verify(observer).onNext(captor.capture());

        PlaybackEventData playbackEventData = captor.getValue();
        expect(playbackEventData.getTrack()).toBe(track);
        expect(playbackEventData.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackEventData.getAction()).toBe(EventLoggerParams.Action.STOP);
        expect(playbackEventData.getUserId()).toBe(USER_ID);
        expect(playbackEventData.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackEventData.getListenTime()).toBeGreaterThan(WAIT_TIME);
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterPlayEventConsumed() throws Exception {
        playbackEventTracker.trackPlayEvent(track, trackSourceInfo, USER_ID);
        playbackEventTracker.trackStopEvent(track, trackSourceInfo, USER_ID);
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent(track, trackSourceInfo, USER_ID);
        verifyZeroInteractions(observer);
    }

    @Test
    public void trackPlayEventShouldSkipNullTracks() {
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackPlayEvent(null, trackSourceInfo, USER_ID);
        verifyZeroInteractions(observer);
    }

    @Test
    public void trackPlayEventShouldSkipNullSourceInfo() {
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackPlayEvent(track, null, USER_ID);
        verifyZeroInteractions(observer);
    }

    @Test
    public void trackStopEventShouldSkipNullTracks() {
        playbackEventTracker.trackPlayEvent(track, trackSourceInfo, USER_ID);
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent(null, trackSourceInfo, USER_ID);
        verifyZeroInteractions(observer);
    }

    @Test
    public void trackStopEventShouldSkipNullSourceInfo() {
        playbackEventTracker.trackPlayEvent(track, trackSourceInfo, USER_ID);
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent(track, null, USER_ID);
        verifyZeroInteractions(observer);
    }
}
