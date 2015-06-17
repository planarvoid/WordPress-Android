package com.soundcloud.android.playback.service.skippy;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.skippy.Skippy.Error;
import static com.soundcloud.android.skippy.Skippy.PlayListener;
import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason;
import static com.soundcloud.android.skippy.Skippy.State;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.SkippyInitilizationFailedEvent;
import com.soundcloud.android.events.SkippyInitilizationSucceededEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.BufferUnderrunListener;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Message;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class SkippyAdapterTest {

    private static final String CDN_HOST = "ec-rtmp-media.soundcloud.com";
    private SkippyAdapter skippyAdapter;

    private static final String STREAM_URL = "https://api-mobile.soundcloud.com/tracks/soundcloud:tracks:123/streams/hls?oauth_token=access";
    private static final long PROGRESS = 500L;
    private static final long DURATION = 1000L;

    @Mock private Skippy skippy;
    @Mock private SkippyFactory skippyFactory;
    @Mock private Playa.PlayaListener listener;
    @Mock private AccountOperations accountOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private SkippyAdapter.StateChangeHandler stateChangeHandler;
    @Mock private ApiUrlBuilder apiUrlBuilder;
    @Mock private Message message;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private Skippy.Configuration configuration;
    @Mock private LockUtil lockUtil;
    @Mock private DeviceHelper deviceHelper;
    @Mock private BufferUnderrunListener bufferUnderrunListener;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private SecureFileStorage secureFileStorage;
    @Mock private CryptoOperations cryptoOperations;
    @Captor private ArgumentCaptor<Playa.StateTransition> stateCaptor;

    private Urn userUrn;
    private TestEventBus eventBus = new TestEventBus();
    private PropertySet track;
    private Urn trackUrn;

    @Before
    public void setUp() {
        userUrn = ModelFixtures.create(Urn.class);
        when(skippyFactory.create(any(PlayListener.class))).thenReturn(skippy);
        skippyAdapter = new SkippyAdapter(skippyFactory, accountOperations, apiUrlBuilder,
                stateChangeHandler, eventBus, connectionHelper, lockUtil, bufferUnderrunListener, sharedPreferences, secureFileStorage, cryptoOperations);
        skippyAdapter.setListener(listener);

        track = TestPropertySets.expectedTrackForPlayer();

        trackUrn = track.get(TrackProperty.URN);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", "refresh"));
        when(listener.requestAudioFocus()).thenReturn(true);
        when(applicationProperties.isReleaseBuild()).thenReturn(true);
        when(connectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.FOUR_G);

        when(stateChangeHandler.obtainMessage(eq(0), any(Playa.StateTransition.class))).thenReturn(new Message());
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);

        when(apiUrlBuilder.from(ApiEndpoints.HLS_STREAM, trackUrn)).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, "access")).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.build()).thenReturn(STREAM_URL);
    }

    @Test
     public void initInitializesWithContextAndFactoryConfiguration() {
        when(skippyFactory.createConfiguration()).thenReturn(configuration);
        skippyAdapter.init(Robolectric.application);
        verify(skippy).init(Robolectric.application, configuration);
    }


    @Test
    public void playDoesNotInteractWithSkippyIfNoListenerPresent() {
        skippyAdapter.setListener(null);
        skippyAdapter.play(track);
        verifyZeroInteractions(skippy);
    }

    @Test
    public void playDoesNotInteractWithSkippyIfAudioFocusFailsToBeGranted() {
        when(listener.requestAudioFocus()).thenReturn(false);
        skippyAdapter.play(track);
        verifyZeroInteractions(skippy);
    }

    @Test
    public void playBroadcastsErrorStateIfAudioFocusFailsToBeGranted() {
        when(listener.requestAudioFocus()).thenReturn(false);
        skippyAdapter.play(track);
        verify(listener).onPlaystateChanged(new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED, trackUrn, 0, track.get(PlayableProperty.DURATION)));
    }

    @Test
    public void playCallsPlayUrlOnSkippy() {
        skippyAdapter.play(track);
        verify(skippy).play(STREAM_URL, 0);
    }

    @Test
    public void playUninterruptedUsesPlayAd() {
        skippyAdapter.playUninterrupted(track);
        verify(skippy).playAd(STREAM_URL, 0);
    }

    @Test
    public void playRemovesStateChangeMessagesFromHandler() {
        skippyAdapter.play(track);
        verify(stateChangeHandler).removeMessages(0);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfSoundCloudDoesNotExistWhenTryingToPlay() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        skippyAdapter.play(track);
    }

    @Test
    public void playUrlWithTheCurrentUrlAndPositionCallsSeekAndResumeOnSkippy() {
        skippyAdapter.play(track);
        skippyAdapter.play(track, 123L);
        InOrder inOrder = Mockito.inOrder(skippy);
        inOrder.verify(skippy).seek(123L);
        inOrder.verify(skippy).resume();
    }

    @Test
    public void playUrlWithTheSameUrlAfterErrorCallsPlayUrlOnSkippy() {
        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FAILED, PROGRESS, DURATION, STREAM_URL);
        skippyAdapter.play(track);

        verify(skippy, times(2)).play(STREAM_URL, 0);
    }

    @Test
    public void playUrlWithTheSameUrlAfterCompleteCallsPlayUrlOnSkippy() {
        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.COMPLETE, Error.OK, PROGRESS, DURATION, STREAM_URL);
        skippyAdapter.play(track);

        verify(skippy, times(2)).play(STREAM_URL, 0);
    }

    @Test
    public void playOfflinePlaysOfflineOnSkippy() {
        final Uri uri = Uri.parse("asdf/123");
        final byte[] key = new byte[0];
        final byte[] iVector = new byte[0];
        when(cryptoOperations.checkAndGetDeviceKey()).thenReturn(new DeviceSecret("asdf", key, iVector));
        when(secureFileStorage.getFileUriForOfflineTrack(trackUrn)).thenReturn(uri);

        skippyAdapter.playOffline(track, 123L);

        verify(skippy).playOffline(uri.toString(), 123L, key, iVector);
    }

    @Test
    public void pauseCallsPauseOnSkippy() {
        skippyAdapter.pause();
        verify(skippy).pause();
    }

    @Test
    public void stopCallsPauseOnSkippy() {
        skippyAdapter.stop();
        verify(skippy).pause();
    }

    @Test
    public void stopForTrackTransitionCallsPauseOnSkippy() {
        skippyAdapter.stopForTrackTransition();
        verify(skippy).pause();
    }

    @Test
    public void destroyCallsDestroySkippy() {
        skippyAdapter.destroy();
        verify(skippy).destroy();
    }

    @Test
    public void seekCallsPauseOnSkippyIfPerformSeekTrue() {
        skippyAdapter.seek(123L, true);
        verify(skippy).seek(123L);
    }

    @Test
    public void seekDoesNotCallPauseOnSkippyIfPerformSeekFalse() {
        skippyAdapter.seek(123L, false);
        verify(skippy, never()).seek(any(Long.class));
    }

    @Test
    public void seekCallsOnProgressWithSeekPosition() {
        when(skippy.getDuration()).thenReturn(456L);
        skippyAdapter.seek(123L, true);
        verify(listener).onProgressEvent(123L, 456L);
    }

    @Test
    public void seekDoesNotCallOnProgressEventWhenDurationIsZero() {
        when(skippy.getDuration()).thenReturn(0L);
        skippyAdapter.seek(123L, true);
        verify(listener, never()).onProgressEvent(anyLong(), anyLong());
    }

    @Test
    public void setVolumeCallsSetVolumeOnSkippy() {
        skippyAdapter.setVolume(123F);
        verify(skippy).setVolume(123F);
    }

    @Test
    public void returnAlwaysReturnsTrue() { // just on Skippy, not on MediaPlayer
        expect(skippyAdapter.resume()).toBeTrue();
    }

    @Test
    public void resumeCallsResumeOnSkippyIfInPausedState() {
        skippyAdapter.resume();
        verify(skippy).resume();
    }

    @Test
    public void getProgressReturnsGetPositionFromSkippy() {
        skippyAdapter.play(track);
        skippyAdapter.getProgress();
        verify(skippy).getPosition();
    }

    @Test
    public void doesNotPropogateProgressChangesForIncorrectUri() {
        skippyAdapter.play(track);
        skippyAdapter.onProgressChange(123L, 456L, "WrongStreamUrl");
        verify(listener, never()).onProgressEvent(anyLong(), anyLong());
    }

    @Test
    public void propogatesProgressChangesForPlayingUri() {
        skippyAdapter.play(track);
        skippyAdapter.onProgressChange(123L, 456L, STREAM_URL);
        verify(listener).onProgressEvent(123L, 456L);
    }

    @Test
    public void adjustsProgressChangesToDurationBoundsForPlayingUri() {
        skippyAdapter.play(track);
        skippyAdapter.onProgressChange(567, 456L, STREAM_URL);
        verify(listener).onProgressEvent(456, 456L);
    }


    @Test
    public void doesNotPropogateStateChangesForIncorrectUrl() {
        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK, PROGRESS, DURATION, "WrongStreamUrl");
        verify(stateChangeHandler, never()).sendMessage(any(Message.class));
    }

    @Test
    public void skippyIdlePausedEventTranslatesToListenerIdleEvent() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyPlayingTranslatesToListenerPlayingEvent() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.PLAYING, Playa.Reason.NONE, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.NOTHING, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyPlayBufferingTranslatesToListenerBufferingEvent() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.BUFFERING, Playa.Reason.NONE, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.BUFFERING, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorTimeoutTranslatesToListenerTimeoutError() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.TIMEOUT, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyFailedErrorTranslatesToListenerErrorFailed() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FAILED, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FAILED, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorForbiddenTranslatesToListenerErrorForbiddenEvent() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FORBIDDEN, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdleErrorNotFoundTranslatesToListenerErrorNotFoundEvent() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.MEDIA_NOT_FOUND, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void skippyIdlePausedEventAdjustsProgressToDurationBoundsWhenSendingEvent() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.NONE, trackUrn, DURATION, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK, DURATION + 1, DURATION, STREAM_URL);
        verify(stateChangeHandler).sendMessage(message);
    }

    @Test
    public void returnsLastStateChangeProgressAfterError() {
        skippyAdapter.play(track);
        Playa.StateTransition expected = new Playa.StateTransition(PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, trackUrn, PROGRESS, DURATION);
        when(stateChangeHandler.obtainMessage(0, expected)).thenReturn(message);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.MEDIA_NOT_FOUND, PROGRESS, DURATION, STREAM_URL);
        expect(skippyAdapter.getProgress()).toEqual(PROGRESS);
    }

    @Test
    public void locksLockUtilWhenPlaying() {
        when(deviceHelper.inSplitTestGroup()).thenReturn(true);

        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.NOTHING, Error.OK, PROGRESS, DURATION, STREAM_URL);

        verify(lockUtil).lock();
    }

    @Test
    public void locksLockUtilWhenBuffering() {
        when(deviceHelper.inSplitTestGroup()).thenReturn(true);

        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.BUFFERING, Error.OK, PROGRESS, DURATION, STREAM_URL);

        verify(lockUtil).lock();
    }

    @Test
    public void unlocksLockUtilWhenIdle() {
        when(deviceHelper.inSplitTestGroup()).thenReturn(true);

        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.NOTHING, Error.OK, PROGRESS, DURATION, STREAM_URL);

        verify(lockUtil).unlock();
    }

    @Test
    public void unlocksLockUtilOnError() {
        when(deviceHelper.inSplitTestGroup()).thenReturn(true);

        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.ERROR, Error.FAILED, PROGRESS, DURATION, STREAM_URL);

        verify(lockUtil).unlock();
    }

    @Test
    public void unlocksLockUtilWhenComplete() {
        when(deviceHelper.inSplitTestGroup()).thenReturn(true);

        skippyAdapter.play(track);
        skippyAdapter.onStateChanged(State.IDLE, Reason.COMPLETE, Error.OK, PROGRESS, DURATION, STREAM_URL);

        verify(lockUtil).unlock();
    }

    @Test
    public void shouldAddStreamingProtocolToPlayStateEvent() {
        skippyAdapter.play(track);

        skippyAdapter.onStateChanged(State.PLAYING, Reason.NOTHING, Error.OK, PROGRESS, DURATION, STREAM_URL);
        verify(stateChangeHandler).obtainMessage(anyInt(), stateCaptor.capture());

        expect(stateCaptor.getValue().getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL)).toEqual(PlaybackProtocol.HLS.getValue());
    }

    @Test
    public void performanceMetricPublishesTimeToPlayEventEvent() {
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
    public void performanceMetricPublishesTimeToPlaylistEvent() {
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
    public void performanceMetricPublishesTimeToSeekEvent() {
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
    public void performanceMetricPublishesFragmentDownloadRateEvent() {
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
    public void performanceMetricPublishesTimeToLoadEvent() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_LOAD_LIBRARY, 1000L, STREAM_URL, CDN_HOST);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD);
        expect(event.getMetricValue()).toEqual(1000L);
        expect(event.getCdnHost()).toEqual(CDN_HOST);
        expect(event.getPlayerType()).toEqual(PlayerType.SKIPPY);
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getUserUrn()).toEqual(userUrn);
    }

    @Test
    public void onErrorPublishesPlaybackErrorEvent() {
        skippyAdapter.onErrorMessage("CODEC_DECODER", "sourceFile", 1, "message", "uri", CDN_HOST);

        final PlaybackErrorEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_ERROR);
        expect(event.getBitrate()).toEqual("128");
        expect(event.getFormat()).toEqual("mp3");
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HLS);
        expect(event.getCategory()).toEqual("CODEC_DECODER");
        expect(event.getCdnHost()).toEqual(CDN_HOST);
    }

    @Test
    public void initilizationSuccessPublishesSkippyInitSuccessEvent() {
        when(skippyFactory.createConfiguration()).thenReturn(configuration);
        when(skippy.init(Robolectric.application, configuration)).thenReturn(true);
        skippyAdapter.init(Robolectric.application);

        final SkippyInitilizationSucceededEvent event = (SkippyInitilizationSucceededEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("failure_count")).toEqual("0");
        expect(event.getAttributes().get("success_count")).toEqual("1");
        expect(event.getAttributes().get("has_failed")).toEqual("false");
    }

    @Test
    public void initilizationSuccessIncrementsSuccessCount() {
        when(skippyFactory.createConfiguration()).thenReturn(configuration);
        when(skippy.init(Robolectric.application, configuration)).thenReturn(true);
        skippyAdapter.init(Robolectric.application);
        verify(sharedPreferencesEditor).putInt(SkippyAdapter.SKIPPY_INIT_SUCCESS_COUNT_KEY, 1);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void initilizationErrorPublishesSkippyInitErrorEvent() {
        skippyAdapter.onInitializationError(new IOException("because"), "some error message");

        final SkippyInitilizationFailedEvent event = (SkippyInitilizationFailedEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("throwable")).toEqual("java.io.IOException: because");
        expect(event.getAttributes().get("message")).toEqual("some error message");
        expect(event.getAttributes().get("failure_count")).toEqual("1");
        expect(event.getAttributes().get("success_count")).toEqual("0");
        expect(event.getAttributes().get("has_succeeded")).toEqual("false");
    }

    @Test
    public void initilizationErrorIncrementsFailureCount() {
        skippyAdapter.onInitializationError(new IOException(), "some error message");
        verify(sharedPreferencesEditor).putInt(SkippyAdapter.SKIPPY_INIT_ERROR_COUNT_KEY, 1);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void shouldNotPerformAnyActionIfUserIsNotLoggedInWhenGettingPerformanceCallback() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.FRAGMENT_DOWNLOAD_RATE, 1000L, STREAM_URL, CDN_HOST);
        verify(accountOperations, never()).getLoggedInUserUrn();
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

}
