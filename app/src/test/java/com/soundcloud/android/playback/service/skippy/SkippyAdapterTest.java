package com.soundcloud.android.playback.service.skippy;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.Protocol;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.skippy.Skippy.Error;
import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason;
import static com.soundcloud.android.skippy.Skippy.State;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.api.Token;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class SkippyAdapterTest {

    public static final String CDN_URI = "http://ec-rtmp-media.soundcloud.com/123456?param=1";
    private SkippyAdapter skippyAdapter;

    @Mock
    private Skippy skippy;
    @Mock
    private SkippyAdapter.SkippyFactory skippyFactory;
    @Mock
    private Context context;
    @Mock
    private Playa.PlayaListener listener;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private EventBus eventBus;

    private Token token = new Token("access", "refresh");

    @Before
    public void setUp() throws Exception {
        when(skippyFactory.create(any(Skippy.PlayListener.class))).thenReturn(skippy);
        when(accountOperations.getSoundCloudToken()).thenReturn(token);
        skippyAdapter = new SkippyAdapter(context, skippyFactory, accountOperations, eventBus);
        skippyAdapter.setListener(listener);
    }

    @Test
    public void getStateDefaultsToIdle() throws Exception {
        expect(skippyAdapter.getState()).toBe(PlayaState.IDLE);
    }

    @Test
    public void playCallsPlayUrlOnSkippy(){
        Track track = new Track(1L);
        track.stream_url = "http://www.some-url.com";
        skippyAdapter.play(track);
        verify(skippy).play("http://www.some-url.com?track_id=1&oauth_token=access", 0);
    }

    @Test
    public void pauseCallsPauseOnSkippy(){
        skippyAdapter.pause();
        verify(skippy).pause();
    }

    @Test
    public void stopCallsPauseOnSkippy(){
        skippyAdapter.stop();
        verify(skippy).pause();
    }

    @Test
    public void destroyCallsDestroySkippy(){
        skippyAdapter.destroy();
        verify(skippy).destroy();
    }

    @Test
    public void seekCallsPauseOnSkippy(){
        skippyAdapter.seek(123L, true);
        verify(skippy).seek(123L);
    }

    @Test
    public void setVolumeCallsSetVolumeOnSkippy(){
        skippyAdapter.setVolume(123F);
        verify(skippy).setVolume(123F);
    }

    @Test
    public void resumeCallsResumeOnSkippy(){
        skippyAdapter.resume();
        verify(skippy).resume();
    }

    @Test
    public void getProgressReturnsGetPositionFromSkippy(){
        skippyAdapter.getProgress();
        verify(skippy).getPosition();
    }

    @Test
    public void getLastStateTransitionDefaultsToIdleNone() throws Exception {
        expect(skippyAdapter.getLastStateTransition()).toEqual(new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE));
    }

    @Test
    public void skippyIdleEventTranslatesToListenerIdleEvent() throws Exception {
        skippyAdapter.onStateChanged(State.IDLE, Reason.NOTHING, Error.OK);
        expect(skippyAdapter.getState()).toBe(PlayaState.IDLE);

        final Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE);
        verify(listener).onPlaystateChanged(expected);
        expect(skippyAdapter.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void skippyIdlePausedEventTranslatesToListenerIdleEvent() throws Exception {
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK);
        expect(skippyAdapter.getState()).toBe(PlayaState.IDLE);

        final Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE);
        verify(listener).onPlaystateChanged(expected);
        expect(skippyAdapter.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void skippyPlayingTranslatesToListenerPlayingEvent() throws Exception {
        skippyAdapter.onStateChanged(State.PLAYING, Reason.NOTHING, Error.OK);
        expect(skippyAdapter.getState()).toBe(PlayaState.PLAYING);

        final Playa.StateTransition expected = new Playa.StateTransition(PlayaState.PLAYING, Playa.Reason.NONE);
        verify(listener).onPlaystateChanged(expected);
        expect(skippyAdapter.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void skippyPlayBufferingTranslatesToListenerBufferingEvent() throws Exception {
        skippyAdapter.onStateChanged(State.PLAYING, Reason.BUFFERING, Error.OK);
        expect(skippyAdapter.getState()).toBe(PlayaState.BUFFERING);

        final Playa.StateTransition expected = new Playa.StateTransition(PlayaState.BUFFERING, Playa.Reason.NONE);
        verify(listener).onPlaystateChanged(expected);
        expect(skippyAdapter.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void skippyIdleErrorFailedTranslatesToListenerErrorFailedEvent() throws Exception {
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FAILED);
        expect(skippyAdapter.getState()).toBe(PlayaState.IDLE);

        final Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED);
        verify(listener).onPlaystateChanged(expected);
        expect(skippyAdapter.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void skippyIdleErrorForbiddenTranslatesToListenerErrorForbiddenEvent() throws Exception {
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FORBIDDEN);
        expect(skippyAdapter.getState()).toBe(PlayaState.IDLE);

        final Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN);
        verify(listener).onPlaystateChanged(expected);
        expect(skippyAdapter.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void skippyIdleErrorNotFoundTranslatesToListenerErrorNotFoundEvent() throws Exception {
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.MEDIA_NOT_FOUND);
        expect(skippyAdapter.getState()).toBe(PlayaState.IDLE);

        final Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND);
        verify(listener).onPlaystateChanged(expected);
        expect(skippyAdapter.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void performanceMetricPublishesTimeToPlayEventEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_PLAY, 1000L, CDN_URI);

        final PlaybackPerformanceEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(CDN_URI);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(Protocol.HLS);
    }

    @Test
    public void performanceMetricPublishesTimeToBufferEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_BUFFER, 1000L, CDN_URI);

        final PlaybackPerformanceEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(CDN_URI);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(Protocol.HLS);
    }

    @Test
    public void performanceMetricPublishesTimeToPlaylistEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_GET_PLAYLIST, 1000L, CDN_URI);

        final PlaybackPerformanceEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(CDN_URI);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(Protocol.HLS);
    }

    @Test
    public void performanceMetricPublishesTimeToSeekEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_SEEK, 1000L, CDN_URI);

        final PlaybackPerformanceEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(CDN_URI);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(Protocol.HLS);
    }

    @Test
    public void performanceMetricPublishesFragmentDownloadRateEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        skippyAdapter.onPerformanceMeasured(PlaybackMetric.FRAGMENT_DOWNLOAD_RATE, 1000L, CDN_URI);

        final PlaybackPerformanceEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getUri()).toEqual(CDN_URI);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(Protocol.HLS);
    }

}
