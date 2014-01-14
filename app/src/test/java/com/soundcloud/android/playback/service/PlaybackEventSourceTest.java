package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackEventSourceTest {

    private static final long USER_ID = 123L;

    private PlaybackEventSource playbackEventSource;
    private Track track;

    @Mock
    private TrackSourceInfo trackSourceInfo;
    @Mock
    private Observer<PlaybackEvent> observer;

    @Before
    public void setUp() throws Exception {
        track = TestHelper.getModelFactory().createModel(Track.class);
        playbackEventSource = new PlaybackEventSource();
    }

    @Test
    public void trackPlayEventPublishesPlaybackEventWithPlaybackEventData() throws Exception {
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);

        ArgumentCaptor<PlaybackEvent> captor = ArgumentCaptor.forClass(PlaybackEvent.class);
        verify(observer).onNext(captor.capture());

        PlaybackEvent playbackEvent = captor.getValue();
        expect(playbackEvent.getTrack()).toBe(track);
        expect(playbackEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackEvent.isPlayEvent()).toBeTrue();
        expect(playbackEvent.getUserId()).toBe(USER_ID);
        expect(playbackEvent.getTimeStamp()).toBeGreaterThan(0L);

        subscription.unsubscribe();
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterNoPlayEvent() throws Exception {
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, 0);
        verifyZeroInteractions(observer);
        subscription.unsubscribe();
    }

    @Test
    public void trackStopEventPublishesPlaybackEventWithPlaybackEventDataAfterInitialPlayEvent() throws Exception {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, PlaybackEvent.STOP_REASON_BUFFERING);

        ArgumentCaptor<PlaybackEvent> captor = ArgumentCaptor.forClass(PlaybackEvent.class);
        verify(observer).onNext(captor.capture());

        PlaybackEvent playbackEvent = captor.getValue();
        expect(playbackEvent.getTrack()).toBe(track);
        expect(playbackEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackEvent.isStopEvent()).toBeTrue();
        expect(playbackEvent.getUserId()).toBe(USER_ID);
        expect(playbackEvent.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackEvent.getStopReason()).toEqual(PlaybackEvent.STOP_REASON_BUFFERING);

        subscription.unsubscribe();
    }

    @Test
    public void trackStopEventDoesNothingWhenCallingStopAfterPlayEventConsumed() throws Exception {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, 0);
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishStopEvent(track, trackSourceInfo, USER_ID, 0);
        verifyZeroInteractions(observer);
        subscription.unsubscribe();
    }

    @Test
    public void trackPlayEventShouldSkipNullTracks() {
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishPlayEvent(null, trackSourceInfo, USER_ID);
        verifyZeroInteractions(observer);
        subscription.unsubscribe();
    }

    @Test
    public void trackPlayEventShouldSkipNullSourceInfo() {
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishPlayEvent(track, null, USER_ID);
        verifyZeroInteractions(observer);
        subscription.unsubscribe();
    }

    @Test
    public void trackStopEventShouldSkipNullTracks() {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishStopEvent(null, trackSourceInfo, USER_ID, 0);
        verifyZeroInteractions(observer);
        subscription.unsubscribe();
    }

    @Test
    public void trackStopEventShouldSkipNullSourceInfo() {
        playbackEventSource.publishPlayEvent(track, trackSourceInfo, USER_ID);
        Subscription subscription = EventBus.PLAYBACK.subscribe(observer);
        playbackEventSource.publishStopEvent(track, null, USER_ID, 0);
        verifyZeroInteractions(observer);
        subscription.unsubscribe();
    }

}
