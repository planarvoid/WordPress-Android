package com.soundcloud.android.playback.service.skippy;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.skippy.Skippy.Error;
import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason;
import static com.soundcloud.android.skippy.Skippy.State;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Message;

@RunWith(SoundCloudTestRunner.class)
public class SkippyAdapterTest {

    public static final String CDN_URI = "http://ec-rtmp-media.soundcloud.com/123456?param=1";
    private SkippyAdapter skippyAdapter;

    private static final String STREAM_URL = "https://api.soundcloud.com/app/mobileapps/tracks/soundcloud:tracks:1/streams/hls?oauth_token=access";

    @Mock
    private Skippy skippy;
    @Mock
    private SkippyAdapter.SkippyFactory skippyFactory;
    @Mock
    private Playa.PlayaListener listener;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private EventBus eventBus;
    @Mock
    private SkippyAdapter.StateChangeHandler stateChangeHandler;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private Track track;
    @Mock
    private Message message;
    @Mock
    private NetworkConnectionHelper connectionHelper;

    @Before
    public void setUp() throws Exception {
        when(skippyFactory.create(any(Skippy.PlayListener.class))).thenReturn(skippy);
        skippyAdapter = new SkippyAdapter(skippyFactory, accountOperations, playbackOperations,
                stateChangeHandler, eventBus, connectionHelper);
        skippyAdapter.setListener(listener);
    }

    @Test
    public void getStateDefaultsToIdle() throws Exception {
        expect(skippyAdapter.getState()).toBe(PlayaState.IDLE);
    }

    @Test
    public void playCallsPlayUrlOnSkippy(){
        when(playbackOperations.buildHLSUrlForTrack(track)).thenReturn(STREAM_URL);
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);

        skippyAdapter.play(track);
        verify(skippy).play(STREAM_URL, 0);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfSoundCloudDoesNotExistWhenTryingToPlay(){
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        skippyAdapter.play(new Track(1L));

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
    public void resumeCallsResumeOnSkippyIfInPausedState(){
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK);
        skippyAdapter.resume();
        verify(skippy).resume();
    }

    @Test
    public void resumeReturnsFalseAndDoesNotResumeIfInCompleteState(){
        skippyAdapter.onStateChanged(State.IDLE, Reason.NOTHING, Error.OK);
        expect(skippyAdapter.resume()).toBeFalse();
        verifyZeroInteractions(skippy);
    }

    @Test
    public void getProgressReturnsGetPositionFromSkippy(){
        skippyAdapter.getProgress();
        verify(skippy).getPosition();
    }

    @Test
    public void getLastStateTransitionDefaultsToIdleComplete() throws Exception {
        expect(skippyAdapter.getLastStateTransition()).toEqual(new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.COMPLETE));
    }

    @Test
    public void skippyIdleNothingEventTranslatesToListenerIdleCompleteEvent() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.COMPLETE);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.NOTHING, Error.OK);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdlePausedEventTranslatesToListenerIdleEvent() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyPlayingTranslatesToListenerPlayingEvent() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.PLAYING, Playa.Reason.NONE);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.NOTHING, Error.OK);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void shouldStoreLastStateInformation(){
        skippyAdapter.onStateChanged(State.PLAYING, Reason.BUFFERING, Error.OK);
        expect(skippyAdapter.getLastStateTransition()).toEqual(new Playa.StateTransition(PlayaState.BUFFERING, Playa.Reason.NONE));
    }

    @Test
    public void skippyPlayBufferingTranslatesToListenerBufferingEvent() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.BUFFERING, Playa.Reason.NONE);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.BUFFERING, Error.OK);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorTimeoutTranslatesToListenerTimeoutError() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.TIMEOUT);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyFailedErrorTranslatesToListenerErrorFailed() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FAILED);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorForbiddenTranslatesToListenerErrorForbiddenEvent() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FORBIDDEN);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorNotFoundTranslatesToListenerErrorNotFoundEvent() throws Exception {
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.MEDIA_NOT_FOUND);
        verify(stateChangeHandler).sendMessage(message);
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
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
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
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
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
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
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
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
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
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
    }

    @Test
    public void onErrorPublishesPlaybackErrorEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        skippyAdapter.onErrorMessage("category", "message", CDN_URI);

        final PlaybackErrorEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_ERROR);
        expect(event.getBitrate()).toEqual("128");
        expect(event.getFormat()).toEqual("mp3");
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getCategory()).toEqual("category");
        expect(event.getCdnUrl()).toEqual(CDN_URI);
    }

}
