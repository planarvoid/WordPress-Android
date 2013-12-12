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

    private static final String PARAMS = "params";
    private static final long USER_ID = 123L;
    private static final long WAIT_TIME = 50L;

    private PlaybackEventTracker playbackEventTracker;
    private Track track;

    @Mock
    private Observer<PlaybackEventData> observer;

    @Before
    public void setUp() throws Exception {
        track = TestHelper.getModelFactory().createModel(Track.class);
        playbackEventTracker = new PlaybackEventTracker(track, PARAMS, USER_ID);
    }

    @Test
    public void trackPlayEventPublishesPlaybackEventWithPlaybackEventData() throws Exception {
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackPlayEvent();

        ArgumentCaptor<PlaybackEventData> captor = ArgumentCaptor.forClass(PlaybackEventData.class);
        verify(observer).onNext(captor.capture());

        PlaybackEventData playbackEventData = captor.getValue();
        expect(playbackEventData.getTrack()).toBe(track);
        expect(playbackEventData.getEventLoggerParams()).toBe(PARAMS);
        expect(playbackEventData.getAction()).toBe(EventLoggerParams.Action.PLAY);
        expect(playbackEventData.getUserId()).toBe(USER_ID);
        expect(playbackEventData.getTimeStamp()).toBeGreaterThan(0L);
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterNoPlayEvent() throws Exception {
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent();
        verifyZeroInteractions(observer);
    }

    @Test
    public void trackStopEventPublishesPlaybackEventWithPlaybackEventDataAfterInitialPlayEvent() throws Exception {
        playbackEventTracker.trackPlayEvent();
        Thread.sleep(WAIT_TIME);
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent();

        ArgumentCaptor<PlaybackEventData> captor = ArgumentCaptor.forClass(PlaybackEventData.class);
        verify(observer).onNext(captor.capture());

        PlaybackEventData playbackEventData = captor.getValue();
        expect(playbackEventData.getTrack()).toBe(track);
        expect(playbackEventData.getEventLoggerParams()).toBe(PARAMS);
        expect(playbackEventData.getAction()).toBe(EventLoggerParams.Action.STOP);
        expect(playbackEventData.getUserId()).toBe(USER_ID);
        expect(playbackEventData.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackEventData.getListenTime()).toBeGreaterThan(WAIT_TIME);
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterPlayEventConsumed() throws Exception {
        playbackEventTracker.trackPlayEvent();
        playbackEventTracker.trackStopEvent();
        Event.PLAYBACK.subscribe(observer);
        playbackEventTracker.trackStopEvent();
        verifyZeroInteractions(observer);
    }

}
