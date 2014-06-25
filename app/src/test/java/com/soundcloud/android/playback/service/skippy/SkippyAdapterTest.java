package com.soundcloud.android.playback.service.skippy;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.skippy.Skippy.Error;
import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason;
import static com.soundcloud.android.skippy.Skippy.State;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.PlaybackServiceOperations;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.os.Message;

@RunWith(SoundCloudTestRunner.class)
public class SkippyAdapterTest {

    private static final String CDN_HOST = "ec-rtmp-media.soundcloud.com";
    private SkippyAdapter skippyAdapter;

    private static final String STREAM_URL = "https://api.soundcloud.com/app/mobileapps/tracks/soundcloud:tracks:1/streams/hls?oauth_token=access";
    private static final long PROGRESS = 500L;
    private static final long DURATION = 1000L;

    @Mock
    private Skippy skippy;
    @Mock
    private SkippyAdapter.SkippyFactory skippyFactory;
    @Mock
    private Playa.PlayaListener listener;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private SkippyAdapter.StateChangeHandler stateChangeHandler;
    @Mock
    private PlaybackServiceOperations playbackOperations;
    @Mock
    private Track track;
    @Mock
    private Message message;
    @Mock
    private NetworkConnectionHelper connectionHelper;

    private UserUrn userUrn;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        userUrn = TestHelper.getModelFactory().createModel(UserUrn.class);
        when(skippyFactory.create(any(Skippy.PlayListener.class))).thenReturn(skippy);
        skippyAdapter = new SkippyAdapter(skippyFactory, accountOperations, playbackOperations,
                stateChangeHandler, eventBus, connectionHelper, applicationProperties);
        skippyAdapter.setListener(listener);

        final TrackUrn trackUrn = Urn.forTrack(1L);
        when(track.getUrn()).thenReturn(trackUrn);
        when(playbackOperations.logPlay(trackUrn)).thenReturn(Observable.just(trackUrn));
        when(playbackOperations.buildHLSUrlForTrack(track)).thenReturn(STREAM_URL);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(listener.requestAudioFocus()).thenReturn(true);
        when(applicationProperties.isReleaseBuild()).thenReturn(true);
    }

    @Test
    public void playDoesNotInteractWithSkippyIfNoListenerPresent(){
        skippyAdapter.setListener(null);
        skippyAdapter.play(track);
        verifyZeroInteractions(skippy);
    }

    @Test
    public void playDoesNotInteractWithSkippyIfAudioFocusFailsToBeGranted(){
        when(listener.requestAudioFocus()).thenReturn(false);
        skippyAdapter.play(track);
        verifyZeroInteractions(skippy);
    }

    @Test
    public void playBroadcastsErrorStateIfAudioFocusFailsToBeGranted(){
        when(listener.requestAudioFocus()).thenReturn(false);
        skippyAdapter.play(track);
        verify(listener).onPlaystateChanged(new Playa.StateTransition(PlayaState.IDLE , Playa.Reason.ERROR_FAILED, 0, 0));
    }

    @Test
    public void playCallsPlayUrlOnSkippy(){
        skippyAdapter.play(track);
        verify(skippy).play(STREAM_URL, 0);
    }

    @Test
    public void playRemovesStateChangeMessagesFromHandler(){
        skippyAdapter.play(track);
        verify(stateChangeHandler).removeMessages(0);
    }

    @Test
    public void playLogsPlayThroughPlaybackOperations(){
        TestObservables.MockObservable<TrackUrn> mockObservable = TestObservables.emptyObservable();
        when(playbackOperations.logPlay(track.getUrn())).thenReturn(mockObservable);
        skippyAdapter.play(track);
        expect(mockObservable.subscribedTo()).toBeTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfSoundCloudDoesNotExistWhenTryingToPlay(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        skippyAdapter.play(new Track(1L));

    }

    @Test
    public void playUrlWithTheCurrentUrlAndPositionCallsSeekAndResumeOnSkippy(){
        skippyAdapter.play(track);
        skippyAdapter.play(track, 123L);
        InOrder inOrder = Mockito.inOrder(skippy);
        inOrder.verify(skippy).seek(123L);
        inOrder.verify(skippy).resume();
    }

    @Test
    public void playUrlWithTheSameUrlAfterErrorCallsPlayUrlOnSkippy(){
        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FAILED, PROGRESS, DURATION, STREAM_URL);
        skippyAdapter.play(track);

        verify(skippy, times(2)).play(STREAM_URL, 0);
    }

    @Test
    public void playUrlWithTheSameUrlAfterCompleteCallsPlayUrlOnSkippy(){
        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.COMPLETE, Error.OK, PROGRESS, DURATION, STREAM_URL);
        skippyAdapter.play(track);

        verify(skippy, times(2)).play(STREAM_URL, 0);
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
    public void seekCallsPauseOnSkippyIfPerformSeekTrue(){
        skippyAdapter.seek(123L, true);
        verify(skippy).seek(123L);
    }

    @Test
    public void seekDoesNotCallPauseOnSkippyIfPerformSeekFalse(){
        skippyAdapter.seek(123L, false);
        verify(skippy, never()).seek(any(Long.class));
    }

    @Test
    public void seekCallsOnProgressWithSeekPosition(){
        when(skippy.getDuration()).thenReturn(456L);
        skippyAdapter.seek(123L, true);
        verify(listener).onProgressEvent(123L, 456L);
    }

    @Test
    public void setVolumeCallsSetVolumeOnSkippy(){
        skippyAdapter.setVolume(123F);
        verify(skippy).setVolume(123F);
    }

    @Test
    public void returnAlwaysReturnsTrue(){ // just on Skippy, not on MediaPlayer
        expect(skippyAdapter.resume()).toBeTrue();
    }

    @Test
    public void resumeCallsResumeOnSkippyIfInPausedState(){
        skippyAdapter.resume();
        verify(skippy).resume();
    }

    @Test
    public void getProgressReturnsGetPositionFromSkippy(){
        skippyAdapter.play(track);
        skippyAdapter.getProgress();
        verify(skippy).getPosition();
    }

    @Test
    public void doesNotPropogateProgressChangesForIncorrectUri(){
        skippyAdapter.play(track);
        skippyAdapter.onProgressChange(123L, 456L, "WrongStreamUrl");
        verify(listener, never()).onProgressEvent(anyLong(), anyLong());
    }

    @Test
    public void propogatesProgressChangesForPlayingUri(){
        skippyAdapter.play(track);
        skippyAdapter.onProgressChange(123L, 456L, STREAM_URL);
        verify(listener).onProgressEvent(123L, 456L);
    }


    @Test
    public void doesNotPropogateStateChangesForIncorrectUrl(){
        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK, PROGRESS, DURATION, "WrongStreamUrl");
        verify(stateChangeHandler, never()).sendMessage(any(Message.class));
    }

    @Test
    public void skippyAddsDebugToStateChangeEventWhenNotReleaseBuild() throws Exception {
        skippyAdapter.play(track);

        when(applicationProperties.isReleaseBuild()).thenReturn(false);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE, PROGRESS, DURATION);
        expected.setDebugExtra("Experimental Player");
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);

        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdlePausedEventTranslatesToListenerIdleEvent() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyPlayingTranslatesToListenerPlayingEvent() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.PLAYING, Playa.Reason.NONE, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.NOTHING, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyPlayBufferingTranslatesToListenerBufferingEvent() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.BUFFERING, Playa.Reason.NONE, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.BUFFERING, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorTimeoutTranslatesToListenerTimeoutError() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.TIMEOUT, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyFailedErrorTranslatesToListenerErrorFailed() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FAILED, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorForbiddenTranslatesToListenerErrorForbiddenEvent() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FORBIDDEN, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorNotFoundTranslatesToListenerErrorNotFoundEvent() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.MEDIA_NOT_FOUND, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void returnsLastStateChangeProgressAfterError() throws Exception {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.MEDIA_NOT_FOUND, PROGRESS, DURATION, STREAM_URL);
        expect(skippyAdapter.getProgress()).toEqual(PROGRESS);
    }

    @Test
    public void performanceMetricPublishesTimeToPlayEventEvent() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_PLAY, 1000L, STREAM_URL, CDN_HOST);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void performanceMetricPublishesTimeToBufferEvent() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_BUFFER, 1000L, STREAM_URL, CDN_HOST);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void performanceMetricPublishesTimeToPlaylistEvent() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_GET_PLAYLIST, 1000L, STREAM_URL, CDN_HOST);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void performanceMetricPublishesTimeToSeekEvent() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_SEEK, 1000L, STREAM_URL, CDN_HOST);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void performanceMetricPublishesFragmentDownloadRateEvent() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.FRAGMENT_DOWNLOAD_RATE, 1000L, STREAM_URL, CDN_HOST);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void onErrorPublishesPlaybackErrorEvent() throws Exception {
        skippyAdapter.onErrorMessage("category", "message", STREAM_URL, CDN_HOST);

        final PlaybackErrorEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_ERROR);
        expect(event.getBitrate()).toEqual("128");
        expect(event.getFormat()).toEqual("mp3");
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getCategory()).toEqual("category");
        expect(event.getCdnHost()).toEqual(CDN_HOST);
    }

    @Test
    public void shouldNotPerformAnyActionIfUserIsNotLoggedInWhenGettingPerformanceCallback(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.FRAGMENT_DOWNLOAD_RATE, 1000L, STREAM_URL, CDN_HOST);
        verify(accountOperations, never()).getLoggedInUserUrn();
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

}
