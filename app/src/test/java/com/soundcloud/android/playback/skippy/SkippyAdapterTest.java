package com.soundcloud.android.playback.skippy;

import static com.soundcloud.android.skippy.Skippy.Error;
import static com.soundcloud.android.skippy.Skippy.PlayListener;
import static com.soundcloud.android.skippy.Skippy.PlaybackMetric;
import static com.soundcloud.android.skippy.Skippy.Reason;
import static com.soundcloud.android.skippy.Skippy.State;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FileAccessEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.playback.AudioAdPlaybackItem;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PreloadItem;
import com.soundcloud.android.playback.skippy.SkippyAdapter.StateChangeHandler;
import com.soundcloud.android.playback.skippy.SkippyAdapter.StateChangeHandler.StateChangeMessage;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.skippy.SkippyPreloader;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Message;
import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.List;

public class SkippyAdapterTest extends AndroidUnitTest {

    private static final String CDN_HOST = "ec-rtmp-media.soundcloud.com";
    private static final Skippy.SkippyMediaType MP3 = Skippy.SkippyMediaType.MP3;
    public static final int BITRATE = 128000;
    public static final String CACHE_PATH = "absolute/path/to/cache";
    private SkippyAdapter skippyAdapter;

    private static final String STREAM_URL = "https://api-mobile.soundcloud.com/tracks/soundcloud:tracks:123/streams/hls?oauth_token=access";
    private static final String SNIPPET_STREAM_URL = "https://api-mobile.soundcloud.com/tracks/soundcloud:tracks:123/streams/hls/snippet?oauth_token=access";
    private static final long PROGRESS = 500L;
    private static final long DURATION = 1000L;

    @Mock private Skippy skippy;
    @Mock private SkippyPreloader skippyPreloader;
    @Mock private SkippyFactory skippyFactory;
    @Mock private Player.PlayerListener listener;
    @Mock private AccountOperations accountOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private StateChangeHandler stateChangeHandler;
    @Mock private ApiUrlBuilder apiUrlBuilder;
    @Mock private ApiUrlBuilder snippetApiUrlBuilder;
    @Mock private Message message;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private Skippy.Configuration configuration;
    @Mock private Skippy.Configuration preloadConfiguration;
    @Mock private LockUtil lockUtil;
    @Mock private BufferUnderrunListener bufferUnderrunListener;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private SecureFileStorage secureFileStorage;
    @Mock private CryptoOperations cryptoOperations;
    @Captor private ArgumentCaptor<StateChangeMessage> stateChangeMessageCaptor;
    @Captor private ArgumentCaptor<Message> messageCaptor;

    private Urn userUrn;
    private TestEventBus eventBus = new TestEventBus();
    private Urn trackUrn = Urn.forTrack(123L);
    private TrackItem track;
    private PlaybackItem playbackItem = AudioPlaybackItem.create(trackUrn,
                                                                 0L,
                                                                 Consts.NOT_SET,
                                                                 PlaybackType.AUDIO_DEFAULT);
    private TestDateProvider dateProvider;

    @Before
    public void setUp() {
        track = PlayableFixtures.baseTrackBuilder().getUrn(trackUrn).snippetDuration(345L).fullDuration(456L).isSnipped(false).build();
        userUrn = ModelFixtures.create(Urn.class);
        when(skippyFactory.create(any(PlayListener.class))).thenReturn(skippy);
        when(skippyFactory.createPreloader()).thenReturn(skippyPreloader);
        dateProvider = new TestDateProvider();
        skippyAdapter = new SkippyAdapter(skippyFactory,
                                          accountOperations,
                                          apiUrlBuilder,
                                          stateChangeHandler,
                                          eventBus,
                                          connectionHelper,
                                          lockUtil,
                                          bufferUnderrunListener,
                                          sharedPreferences,
                                          secureFileStorage,
                                          cryptoOperations,
                                          dateProvider);
        skippyAdapter.setListener(listener);

        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getSoundCloudToken()).thenReturn(new Token("access", "refresh"));
        when(applicationProperties.isReleaseBuild()).thenReturn(true);
        when(connectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.FOUR_G);

        when(stateChangeHandler.obtainMessage(eq(0), any(StateChangeMessage.class))).thenReturn(message);
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);

        when(apiUrlBuilder.from(ApiEndpoints.HLS_STREAM, trackUrn)).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, "access")).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.withQueryParam("can_snip", false)).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.build()).thenReturn(STREAM_URL);

        when(apiUrlBuilder.from(ApiEndpoints.HLS_SNIPPET_STREAM, trackUrn)).thenReturn(snippetApiUrlBuilder);
        when(snippetApiUrlBuilder.withQueryParam(ApiRequest.Param.OAUTH_TOKEN, "access")).thenReturn(
                snippetApiUrlBuilder);
        when(snippetApiUrlBuilder.build()).thenReturn(SNIPPET_STREAM_URL);

        when(configuration.getCachePath()).thenReturn(CACHE_PATH);
        when(skippyFactory.createConfiguration()).thenReturn(configuration);
        when(skippyFactory.createPreloaderConfiguration()).thenReturn(preloadConfiguration);
    }

    @Test
    public void initInitializesWithContextAndFactoryConfiguration() {
        skippyAdapter.init();
        verify(skippy).init(configuration);
    }

    @Test
    public void initInitializesPreloaderWithContextAndFactoryConfiguration() {
        when(skippy.init(configuration)).thenReturn(true);
        skippyAdapter.init();
        verify(skippyPreloader).init();
    }

    @Test
    public void preloadCallsCueOnSkippyPreloader() {
        skippyAdapter.preload(getPreloadItem());
        verify(skippyPreloader).fetch(SNIPPET_STREAM_URL,
                                      SkippyAdapter.PRELOAD_START_POSITION,
                                      SkippyAdapter.PRELOAD_DURATION);
    }

    @Test
    public void playCallsPlayUrlOnSkippy() {
        skippyAdapter.play(playbackItem);
        verify(skippy).play(STREAM_URL, 0);
    }

    @Test
    public void playSnippetPlaysSnippetOnSkippy() {
        skippyAdapter.play(AudioPlaybackItem.forSnippet(track, 123L));

        verify(skippy).play(SNIPPET_STREAM_URL, 123);
    }

    @Test
    public void playAudioAdPlaysUrlOnSkippy() {
        final AudioAd audioAd = AdFixtures.getAudioAd(trackUrn);

        skippyAdapter.play(AudioAdPlaybackItem.create(audioAd));

        verify(skippy).play("http://audiourl.com/audio.m3u?oauth_token=access", 0, false);
    }

    @Test
    public void playRemovesStateChangeMessagesFromHandler() {
        skippyAdapter.play(playbackItem);
        verify(stateChangeHandler).removeMessages(0);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfSoundCloudDoesNotExistWhenTryingToPlay() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        skippyAdapter.play(playbackItem);
    }

    @Test
    public void playUrlWithTheCurrentUrlAndPositionCallsSeekAndResumeOnSkippy() {
        skippyAdapter.play(AudioPlaybackItem.create(trackUrn, 0L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT));
        skippyAdapter.play(AudioPlaybackItem.create(trackUrn, 123L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT));
        InOrder inOrder = Mockito.inOrder(skippy);
        inOrder.verify(skippy).seek(123L);
        inOrder.verify(skippy).resume();
    }

    @Test
    public void playUrlWithTheSameUrlAfterErrorCallsPlayUrlOnSkippy() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.ERROR,
                                     Error.FAILED,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);
        skippyAdapter.play(playbackItem);

        verify(skippy, times(2)).play(STREAM_URL, 0);
    }

    @Test
    public void playUrlWithTheSameUrlAfterCompleteCallsPlayUrlOnSkippy() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.COMPLETE,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);
        skippyAdapter.play(playbackItem);

        verify(skippy, times(2)).play(STREAM_URL, 0);
    }

    @Test
    public void playOfflinePlaysOfflineOnSkippy() {
        final Uri uri = Uri.parse("asdf/123");
        final byte[] key = new byte[0];
        final byte[] iVector = new byte[0];
        when(cryptoOperations.checkAndGetDeviceKey()).thenReturn(new DeviceSecret("asdf", key, iVector));
        when(secureFileStorage.getFileUriForOfflineTrack(trackUrn)).thenReturn(uri);

        skippyAdapter.play(AudioPlaybackItem.forOffline(track, 123L));

        verify(skippy).playOffline(uri.toString(), 123L, key, iVector);
    }

    @Test
    public void pauseCallsPauseOnSkippy() {
        skippyAdapter.pause();
        verify(skippy).pause();
    }

    @Test
    public void stopCallsStopOnSkippy() {
        skippyAdapter.stop();
        verify(skippy).stop();
    }

    @Test
    public void stopForTrackTransitionCallsStopOnSkippy() {
        skippyAdapter.stopForTrackTransition();
        verify(skippy).stop();
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
    public void getVolumeCallsGetVolumeOnSkippy() {
        when(skippy.getVolume()).thenReturn(0.42f);
        float volume = skippyAdapter.getVolume();

        assertThat(volume).isEqualTo(0.42f);
    }

    @Test
    public void resumeCallsResumeOnSkippyIfInPausedState() {
        skippyAdapter.play(playbackItem);

        skippyAdapter.pause();
        skippyAdapter.resume(playbackItem);

        verify(skippy).resume();
    }

    @Test
    public void resumeRestartsPlaybackWhenStoppedAtItsPreviousPosition() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.PLAYING, Reason.NOTHING, Error.OK, 1500L, 3000L, STREAM_URL, MP3, BITRATE);

        skippyAdapter.stop();
        skippyAdapter.resume(playbackItem);

        verify(skippy).play(STREAM_URL, 1500L);
    }

    @Test
    public void resumeStartsPlaybackWhenNeverPlayed() {
        skippyAdapter.resume(playbackItem);

        verify(skippy).play(STREAM_URL, playbackItem.getStartPosition());
    }

    @Test
    public void getProgressReturnsGetPositionFromSkippy() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.getProgress();
        verify(skippy).getPosition();
    }

    @Test
    public void doesNotPropogateProgressChangesForIncorrectUri() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onProgressChange(123L, 456L, "WrongStreamUrl", MP3, BITRATE);
        verify(listener, never()).onProgressEvent(anyLong(), anyLong());
    }

    @Test
    public void propogatesProgressChangesForPlayingUri() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onProgressChange(123L, 456L, STREAM_URL, MP3, BITRATE);
        verify(listener).onProgressEvent(123L, 456L);
    }

    @Test
    public void adjustsProgressChangesToDurationBoundsForPlayingUri() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onProgressChange(567, 456L, STREAM_URL, MP3, BITRATE);
        verify(listener).onProgressEvent(456, 456L);
    }

    @Test
    public void doesNotPropogateStateChangesForIncorrectUrl() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.PAUSED,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     "WrongStreamUrl",
                                     MP3,
                                     BITRATE);
        verify(stateChangeHandler, never()).sendMessage(any(Message.class));
    }

    @Test
    public void skippyIdlePausedEventTranslatesToListenerIdleEvent() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.NONE,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);

        skippyAdapter.onStateChanged(State.IDLE, Reason.PAUSED, Error.OK, PROGRESS, DURATION, STREAM_URL, MP3, BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyIdleStoppedEventTranslatesToListenerIdleNoneEvent() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.NONE,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);

        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.STOPPED,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyPlayingTranslatesToListenerPlayingEvent() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                         PlayStateReason.NONE,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);

        skippyAdapter.onStateChanged(State.PLAYING,
                                     Reason.NOTHING,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyPlayBufferingTranslatesToListenerBufferingEvent() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.BUFFERING,
                                                                         PlayStateReason.NONE,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);
        skippyAdapter.onStateChanged(State.PLAYING,
                                     Reason.BUFFERING,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyIdleErrorTimeoutTranslatesToListenerTimeoutError() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.ERROR_FAILED,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);
        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.ERROR,
                                     Error.TIMEOUT,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyFailedErrorTranslatesToListenerErrorFailed() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.ERROR_FAILED,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);

        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.ERROR,
                                     Error.FAILED,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyIdleErrorForbiddenTranslatesToListenerErrorForbiddenEvent() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.ERROR_FORBIDDEN,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);

        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.ERROR,
                                     Error.FORBIDDEN,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyIdleErrorNotFoundTranslatesToListenerErrorNotFoundEvent() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.ERROR_NOT_FOUND,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION,
                                                                         dateProvider);

        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.ERROR,
                                     Error.MEDIA_NOT_FOUND,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void skippyIdlePausedEventAdjustsProgressToDurationBoundsWhenSendingEvent() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.NONE,
                                                                         trackUrn,
                                                                         DURATION,
                                                                         DURATION,
                                                                         dateProvider);

        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.PAUSED,
                                     Error.OK,
                                     DURATION + 1,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
    }

    @Test
    public void returnsLastStateChangeProgressAfterError() {
        skippyAdapter.play(playbackItem);
        PlaybackStateTransition transition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                         PlayStateReason.ERROR_NOT_FOUND,
                                                                         trackUrn,
                                                                         PROGRESS,
                                                                         DURATION);

        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.ERROR,
                                     Error.MEDIA_NOT_FOUND,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verifyStateChangeMessage(playbackItem, transition);
        assertThat(skippyAdapter.getProgress()).isEqualTo(PROGRESS);
    }

    @Test
    public void locksLockUtilWhenPlaying() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.PLAYING,
                                     Reason.NOTHING,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verify(lockUtil).lock();
    }

    @Test
    public void locksLockUtilWhenBuffering() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.PLAYING,
                                     Reason.BUFFERING,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verify(lockUtil).lock();
    }

    @Test
    public void unlocksLockUtilWhenIdle() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.NOTHING,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verify(lockUtil).unlock();
    }

    @Test
    public void unlocksLockUtilOnError() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.ERROR,
                                     Error.FAILED,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verify(lockUtil).unlock();
    }

    @Test
    public void unlocksLockUtilWhenComplete() {
        skippyAdapter.play(playbackItem);
        skippyAdapter.onStateChanged(State.IDLE,
                                     Reason.COMPLETE,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);

        verify(lockUtil).unlock();
    }

    @Test
    public void shouldAddStreamingProtocolToPlayStateEvent() {
        skippyAdapter.play(playbackItem);

        skippyAdapter.onStateChanged(State.PLAYING,
                                     Reason.NOTHING,
                                     Error.OK,
                                     PROGRESS,
                                     DURATION,
                                     STREAM_URL,
                                     MP3,
                                     BITRATE);
        verify(stateChangeHandler).obtainMessage(anyInt(), stateChangeMessageCaptor.capture());

        assertThat(stateChangeMessageCaptor.getValue().stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL)).isEqualTo(
                PlaybackProtocol.HLS.getValue());
    }

    @Test
    public void performanceMetricPublishesTimeToPlayEventEvent() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        skippyAdapter.play(playbackItem);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_PLAY, 1000L, STREAM_URL, CDN_HOST, MP3, BITRATE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.SKIPPY);
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HLS);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void performanceMetricPublishesTimeToPlayEventEventForAudioAds() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        skippyAdapter.play(AudioAdPlaybackItem.create(AdFixtures.getAudioAd(trackUrn)));
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_PLAY, 1000L, STREAM_URL, CDN_HOST, MP3, BITRATE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.SKIPPY);
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HLS);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void performanceMetricPublishesTimeToPlaylistEvent() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        skippyAdapter.play(playbackItem);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_GET_PLAYLIST,
                                            1000L,
                                            STREAM_URL,
                                            CDN_HOST,
                                            MP3,
                                            BITRATE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.SKIPPY);
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HLS);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void performanceMetricPublishesTimeToSeekEvent() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        skippyAdapter.play(playbackItem);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_SEEK, 1000L, STREAM_URL, CDN_HOST, MP3, BITRATE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.SKIPPY);
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HLS);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void performanceMetricPublishesFragmentDownloadRateEvent() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        skippyAdapter.play(playbackItem);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.FRAGMENT_DOWNLOAD_RATE,
                                            1000L,
                                            STREAM_URL,
                                            CDN_HOST,
                                            MP3,
                                            BITRATE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.SKIPPY);
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HLS);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void performanceMetricPublishesTimeToLoadEvent() {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);

        skippyAdapter.onPerformanceMeasured(PlaybackMetric.TIME_TO_LOAD_LIBRARY,
                                            1000L,
                                            STREAM_URL,
                                            CDN_HOST,
                                            MP3,
                                            BITRATE);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_LOAD);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.SKIPPY);
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HLS);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void onErrorPublishesPlaybackErrorEvent() {
        skippyAdapter.onErrorMessage("CODEC_DECODER", "sourceFile", 1, "message", "uri", CDN_HOST, MP3, BITRATE);

        final PlaybackErrorEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_ERROR);
        assertThat(event.getBitrate()).isEqualTo(128000);
        assertThat(event.getFormat()).isEqualTo(MP3.name());
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HLS);
        assertThat(event.getCategory()).isEqualTo("CODEC_DECODER");
        assertThat(event.getCdnHost()).isEqualTo(CDN_HOST);
    }

    @Test
    public void initilizationSuccessWhenSkippyAndPreloadInitialize() {
        when(skippy.init(configuration)).thenReturn(true);

        assertThat(skippyAdapter.init()).isTrue();
    }

    @Test
    public void initilizationErrorWhenSkippyFailsToinit() {
        when(skippy.init(configuration)).thenReturn(false);

        assertThat(skippyAdapter.init()).isFalse();
    }

    @Test
    public void shouldSendFileAccessEvent() {
        skippyAdapter.init();

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events.size()).isEqualTo(1);
        assertThat(events.get(0)).isInstanceOf(FileAccessEvent.class);
    }

    @Test
    public void shouldSendFileAccessEventWithoutCachePath() {
        when(configuration.getCachePath()).thenReturn(null);
        skippyAdapter.init();

        List<TrackingEvent> events = eventBus.eventsOn(EventQueue.TRACKING);
        assertThat(events.size()).isEqualTo(1);
        assertThat(events.get(0)).isInstanceOf(FileAccessEvent.class);
    }

    @Test
    public void shouldNotPerformAnyActionIfUserIsNotLoggedInWhenGettingPerformanceCallback() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        skippyAdapter.onPerformanceMeasured(PlaybackMetric.FRAGMENT_DOWNLOAD_RATE,
                                            1000L,
                                            STREAM_URL,
                                            CDN_HOST,
                                            MP3,
                                            BITRATE);
        verify(accountOperations, never()).getLoggedInUserUrn();
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    private void verifyStateChangeMessage(PlaybackItem item, PlaybackStateTransition transition) {
        verify(stateChangeHandler).obtainMessage(anyInt(), stateChangeMessageCaptor.capture());
        final StateChangeMessage stateChangeMessage = stateChangeMessageCaptor.getValue();

        assertThat(stateChangeMessage.playbackItem).isEqualTo(item);
        assertThat(stateChangeMessage.stateTransition.getUrn()).isEqualTo(transition.getUrn());
        assertThat(stateChangeMessage.stateTransition.getNewState()).isEqualTo(transition.getNewState());
        assertThat(stateChangeMessage.stateTransition.getReason()).isEqualTo(transition.getReason());
        assertThat(stateChangeMessage.stateTransition.getProgress().getPosition()).isEqualTo(transition.getProgress().getPosition());
        assertThat(stateChangeMessage.stateTransition.getProgress().getDuration()).isEqualTo(transition.getProgress().getDuration());

        verify(stateChangeHandler).sendMessage(message);
    }

    @NonNull
    private PreloadItem getPreloadItem() {
        return new PreloadItem() {
            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {

            }

            @Override
            public Urn getUrn() {
                return trackUrn;
            }

            @Override
            public PlaybackType getPlaybackType() {
                return PlaybackType.AUDIO_SNIPPET;
            }
        };
    }
}
