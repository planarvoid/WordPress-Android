package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionEventSourceTest {

    private static final long USER_ID = 123L;

    private PlaybackEventSource playbackEventSource;
    private Track track;

    @Mock
    private TrackSourceInfo trackSourceInfo;
    @Mock
    private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        track = TestHelper.getModelFactory().createModel(Track.class);
        playbackEventSource = new PlaybackEventSource(eventBus);
    }

    @Test
    public void trackPlayEventPublishesPlaybackEventWithPlaybackEventData() throws Exception {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);

        ArgumentCaptor<PlaybackSessionEvent> captor = ArgumentCaptor.forClass(PlaybackSessionEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAYBACK_SESSION), captor.capture());

        PlaybackSessionEvent playbackSessionEvent = captor.getValue();
        expect(playbackSessionEvent.getTrack()).toBe(track);
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.isPlayEvent()).toBeTrue();
        expect(playbackSessionEvent.getUserId()).toBe(USER_ID);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterNoPlayEvent() throws Exception {
        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, 0);
        verifyZeroInteractions(eventBus);
    }

    @Test
    public void trackStopEventPublishesPlaybackEventWithPlaybackEventDataAfterInitialPlayEvent() throws Exception {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, PlaybackSessionEvent.STOP_REASON_BUFFERING);

        ArgumentCaptor<PlaybackSessionEvent> captor = ArgumentCaptor.forClass(PlaybackSessionEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.PLAYBACK_SESSION), captor.capture());

        PlaybackSessionEvent stopEvent = captor.getAllValues().get(1);
        expect(stopEvent.getTrack()).toBe(track);
        expect(stopEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(stopEvent.isStopEvent()).toBeTrue();
        expect(stopEvent.getUserId()).toBe(USER_ID);
        expect(stopEvent.getTimeStamp()).toBeGreaterThan(0L);
        expect(stopEvent.getStopReason()).toEqual(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterPlayEventConsumed() throws Exception {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, 0);
        ArgumentCaptor<PlaybackSessionEvent> captor = ArgumentCaptor.forClass(PlaybackSessionEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.PLAYBACK_SESSION), captor.capture());

        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, 0);
        verifyNoMoreInteractions(eventBus);
    }

    @Test
    public void trackPlayEventShouldSkipNullTracks() {
        playbackEventSource.publishPlayEvent(null, trackSourceInfo, USER_ID);
        verifyZeroInteractions(eventBus);
    }

    @Test
    public void trackPlayEventShouldSkipNullSourceInfo() {
        playbackEventSource.publishPlayEvent(track, null, USER_ID);
        verifyZeroInteractions(eventBus);
    }

    @Test
    public void trackStopEventShouldSkipNullTracks() {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        playbackEventSource.publishStopEvent(null, trackSourceInfo, USER_ID, 0);

        ArgumentCaptor<PlaybackSessionEvent> captor = ArgumentCaptor.forClass(PlaybackSessionEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAYBACK_SESSION), captor.capture());
        expect(captor.getValue().isPlayEvent()).toBeTrue();
        verifyNoMoreInteractions(eventBus);
    }

    @Test
    public void trackStopEventShouldSkipNullSourceInfo() {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        playbackEventSource.publishStopEvent(track, null, USER_ID, 0);
        ArgumentCaptor<PlaybackSessionEvent> captor = ArgumentCaptor.forClass(PlaybackSessionEvent.class);
        verify(eventBus).publish(eq(EventQueue.PLAYBACK_SESSION), captor.capture());
        expect(captor.getValue().isPlayEvent()).toBeTrue();
        verifyNoMoreInteractions(eventBus);
    }

}
