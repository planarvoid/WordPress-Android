package com.soundcloud.android.playback.mediaplayer;

import static com.soundcloud.android.playback.Player.PlayerState;
import static com.soundcloud.android.playback.Player.Reason;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.VideoSource;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.BufferUnderrunListener;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.StreamUrlBuilder;
import com.soundcloud.android.playback.VideoPlaybackItem;
import com.soundcloud.android.playback.VideoSourceProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Date;

public class MediaPlayerAdapterTest extends AndroidUnitTest {

    private static final String ACCESS_TOKEN = "access";
    private static final Token TOKEN = new Token(ACCESS_TOKEN,"refresh");
    private static final String STREAM_URL = "https://api-mobile.soundcloud.com/tracks/soundcloud:tracks:123/streams/http?oauth_token=access";

    private MediaPlayerAdapter mediaPlayerAdapter;

    @Mock private Context context;
    @Mock private MediaPlayer mediaPlayer;
    @Mock private MediaPlayerManager mediaPlayerManager;
    @Mock private MediaPlayerAdapter.PlayerHandler playerHandler;
    @Mock private Player.PlayerListener listener;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private BufferUnderrunListener bufferUnderrunListener;
    @Mock private StreamUrlBuilder urlBuilder;
    @Mock private CurrentDateProvider dateProvider;
    @Mock private VideoSourceProvider videoSourceProvider;
    @Mock private SurfaceHolder surfaceHolder;
    @Captor private ArgumentCaptor<Player.StateTransition> stateCaptor;

    private Urn trackUrn = Urn.forTrack(123L);
    private PlaybackItem trackItem = AudioPlaybackItem.create(trackUrn, 0L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT);
    private VideoPlaybackItem videoItem = VideoPlaybackItem.create(AdFixtures.getVideoAd(Urn.forTrack(321L)));
    private int duration = 20000;

    private Urn userUrn;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        userUrn = ModelFixtures.create(Urn.class);
        when(context.getApplicationContext()).thenReturn(context);
        when(mediaPlayerManager.create()).thenReturn(mediaPlayer);
        when(accountOperations.getSoundCloudToken()).thenReturn(TOKEN);
        when(listener.requestAudioFocus()).thenReturn(true);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.FOUR_G);

        when(urlBuilder.buildHttpsStreamUrl(trackUrn)).thenReturn(STREAM_URL);
        when(videoSourceProvider.selectOptimalSource(videoItem)).thenReturn(VideoSource.create(AdFixtures.getApiVideoSource(100, 200)));
        when(dateProvider.getCurrentDate()).thenReturn(new Date());

        mediaPlayerAdapter = new MediaPlayerAdapter(context, mediaPlayerManager, playerHandler, eventBus, networkConnectionHelper, accountOperations, bufferUnderrunListener, videoSourceProvider, urlBuilder, dateProvider);
        mediaPlayerAdapter.setListener(listener);
    }

    @Test
    public void constructorSetsPlayerListener() {
        verify(playerHandler).setMediaPlayerAdapter(mediaPlayerAdapter);
    }

    @Test
    public void playUrlShouldCreateConfiguredMediaPlayer() {
        mediaPlayerAdapter.play(trackItem);
        verify(mediaPlayerManager).create();
        verify(mediaPlayer).setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        verify(mediaPlayer).setAudioStreamType(AudioManager.STREAM_MUSIC);
        verify(mediaPlayer).setOnPreparedListener(any(MediaPlayer.OnPreparedListener.class));
        verify(mediaPlayer).setOnSeekCompleteListener(any(MediaPlayer.OnSeekCompleteListener.class));
        verify(mediaPlayer).setOnErrorListener(any(MediaPlayer.OnErrorListener.class));
        verify(mediaPlayer).setOnCompletionListener(any(PlaybackCompletionListener.class));
        verify(mediaPlayer).setOnInfoListener(any(MediaPlayer.OnInfoListener.class));
        verify(mediaPlayer, never()).reset();
    }

    @Test
    public void playUrlShouldCallBufferingState() {
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);
        when(mediaPlayer.getDuration()).thenReturn(duration);
        mediaPlayerAdapter.play(trackItem);
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void preparedListenerShouldStartPlayback() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer).start();
    }

    @Test
    public void preparedListenerShouldNotStartPlaybackIfFocusNotGranted() {
        when(listener.requestAudioFocus()).thenReturn(false);
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer, never()).start();
    }

    @Test
    public void preparedListenerShouldCallStatesBufferingToPlaying() {
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);

        mediaPlayerAdapter.play(trackItem);
        when(mediaPlayer.getDuration()).thenReturn(20000);
        mediaPlayerAdapter.onPrepared(mediaPlayer);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, 0, 20000, dateProvider)));
    }

    @Test
    public void shouldAddStreamingProtocolToPlayStateEvent() {
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);

        mediaPlayerAdapter.play(trackItem);

        verify(listener).onPlaystateChanged(stateCaptor.capture());

        assertThat(stateCaptor.getValue().getExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL)).isEqualTo(PlaybackProtocol.HTTPS.getValue());
    }

    @Test
    public void preparedListenerShouldReportTimeToPlay() {
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.TWO_G);
        when(dateProvider.getCurrentDate()).thenReturn(new Date(0));
        mediaPlayerAdapter.play(AudioPlaybackItem.create(trackUrn, 123L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT));
        when(dateProvider.getCurrentDate()).thenReturn(new Date(1000));
        mediaPlayerAdapter.onPrepared(mediaPlayer);

        final PlaybackPerformanceEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        assertThat(event.getMetric()).isEqualTo(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        assertThat(event.getMetricValue()).isEqualTo(1000L);
        assertThat(event.getCdnHost()).isEqualTo(STREAM_URL);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.MEDIA_PLAYER);
        assertThat(event.getProtocol()).isEqualTo(PlaybackProtocol.HTTPS);
        assertThat(event.getConnectionType()).isEqualTo(ConnectionType.TWO_G);
        assertThat(event.getUserUrn()).isEqualTo(userUrn);
    }

    @Test
    public void preparedListenerShouldntReportTimeToPlayOnVideoPlayback() {
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.TWO_G);
        when(dateProvider.getCurrentDate()).thenReturn(new Date(0));
        mediaPlayerAdapter.play(videoItem);
        when(dateProvider.getCurrentDate()).thenReturn(new Date(1000));
        mediaPlayerAdapter.onPrepared(mediaPlayer);

        assertThat(eventBus.eventsOn(EventQueue.PLAYBACK_PERFORMANCE)).isEmpty();
    }

    @Test
    public void playUrlShouldResetAndReuseOldMediaPlayer() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.play(trackItem);
        verify(mediaPlayer).reset();
    }

    @Test
    public void pauseShouldStopReleaseMediaPlayerIfPausedWhilePreparing() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.pause();
        verify(mediaPlayerManager).stopAndReleaseAsync(mediaPlayer);
    }

    @Test
    public void pauseNotifyIdleStateIfPausedWhilePreparing() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.pause();

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.NONE, trackUrn, 0, -1, dateProvider)));
    }

    @Test
    public void seekShouldReturnInvalidSeekPositionWithNoMediaPlayer() {
        assertThat(mediaPlayerAdapter.seek(123l)).isEqualTo(-1L);
    }

    @Test
    public void seekShouldSeekOnMediaPlayerWhilePreparing() {
        mediaPlayerAdapter.play(trackItem);
        assertThat(mediaPlayerAdapter.seek(123l)).isEqualTo(123l);
        verify(mediaPlayer).seekTo(123);
    }

    @Test
    public void seekShouldCallSeekOnMediaPlayer() {
        playUrlAndSetPrepared(trackItem);
        assertThat(mediaPlayerAdapter.seek(123l)).isEqualTo(123l);
        verify(mediaPlayer).seekTo(123);
    }

    @Test
    public void seekShouldCallSeekOnMediaPlayerWithTimeOfZeroWhenPositionNotZero() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(1);
        assertThat(mediaPlayerAdapter.seek(0L)).isEqualTo(0L);
        verify(mediaPlayer).seekTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void seekShouldThrowAnIllegalArgumentExceptionWithNegativeSeekTime() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.seek(-1L);
    }

    @Test
    public void seekShouldReturnSeekPositionWhenGettingProgressWhileSeeking() {
        playUrlAndSetPrepared(trackItem);
        assertThat(mediaPlayerAdapter.seek(123l)).isEqualTo(123l);
        assertThat(mediaPlayerAdapter.getProgress()).isEqualTo(123l);
    }

    @Test
    public void playUrlShouldCreateNewMediaPlayerIfWaitingForSeekToReturn() {
        playUrlAndSetPrepared(trackItem);
        assertThat(mediaPlayerAdapter.seek(123l)).isEqualTo(123l);
        mediaPlayerAdapter.play(trackItem);
        verify(mediaPlayerManager).create();
    }

    @Test
    public void playUrlShouldReleaseOldMediaPlayerIfWaitingForSeekToReturn() {
        playUrlAndSetPrepared(trackItem);
        assertThat(mediaPlayerAdapter.seek(123l)).isEqualTo(123l);
        mediaPlayerAdapter.play(trackItem);
        verify(mediaPlayerManager).stopAndReleaseAsync(mediaPlayer);
    }

    @Test
    public void resumeShouldCreateNewMediaPlayerWithNoMediaPlayer() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.destroy();
        mediaPlayerAdapter.resume();
        verify(mediaPlayerManager).create();
    }

    @Test
    public void resumeShouldCreateNewMediaPlayerIfInPreparingState() {
        mediaPlayerAdapter.play(trackItem);
        reset(mediaPlayer);
        mediaPlayerAdapter.resume();
        verify(mediaPlayerManager, times(2)).create();
    }

    @Test
    public void pauseShouldDoNothingWithNoMediaPlayer() {
        mediaPlayerAdapter.pause();
        verifyNoMoreInteractions(mediaPlayer);
    }

    @Test
    public void pauseShouldCallPauseWhenMediaPlayerPlaying() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void pauseShouldCallPauseOnWhenInPauseState() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.pause();
        reset(mediaPlayer);
        mediaPlayerAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void pauseShouldCallPauseOnWhenInBufferingState() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mediaPlayerAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void seekShouldReturn0WithNoMediaPlayer() {
        assertThat(mediaPlayerAdapter.getProgress()).isEqualTo(0l);
    }

    @Test
    public void seekShouldReturnMediaPlayerPosition() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(999);
        assertThat(mediaPlayerAdapter.getProgress()).isEqualTo(999l);
    }

    @Test
    public void seekShouldReturnSeekPositionIfWaitingForSeek() {
        playUrlAndSetPrepared(trackItem);
        assertThat(mediaPlayerAdapter.seek(123l)).isEqualTo(123l);
        assertThat(mediaPlayerAdapter.getProgress()).isEqualTo(123l);
    }

    @Test
    public void seekRemovesSeekPosClearingThroughHandlerThroughHandler() {
        playUrlAndSetPrepared(trackItem);

        mediaPlayerAdapter.seek(456l);
        // seek position cleared via handler
        verify(playerHandler).removeMessages(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK);
    }

    @Test
    public void playUrlShouldRetryMaxTimesIfMediaPlayerFailsToPrepare() throws IOException {
        Mockito.doThrow(new IOException()).when(mediaPlayer).setDataSource(any(String.class));
        mediaPlayerAdapter.play(trackItem);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(3)).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.ERROR_FAILED, trackUrn, 0, -1, dateProvider)));
    }

    @Test
    public void playUrlSetsDataSourceOnMediaPlayer() throws IOException {
        mediaPlayerAdapter.play(trackItem);
        verify(mediaPlayer).setDataSource(STREAM_URL);
    }

    @Test
    public void playVideoSetsDataSourceOnMediaPlayer() throws IOException {
        final VideoSource videoSource = VideoSource.create(AdFixtures.getApiVideoSource(1, 2));
        when(videoSourceProvider.selectOptimalSource(videoItem)).thenReturn(videoSource);

        mediaPlayerAdapter.play(videoItem);

        verify(mediaPlayer).setDataSource(videoSource.getUrl());
    }

    @Test
    public void setsDisplayOnVideoPlayIfSurfaceHolderSet() throws IOException {
        mediaPlayerAdapter.surfaceCreated(surfaceHolder);
        mediaPlayerAdapter.play(videoItem);

        verify(mediaPlayer).setDisplay(surfaceHolder);
    }

    @Test
    public void setsDisplayAfterPlaybackStartsWhenNewSurfaceHolderSet() throws IOException {
        playUrlAndSetPrepared(videoItem);
        mediaPlayerAdapter.surfaceCreated(surfaceHolder);

        verify(mediaPlayer).setDisplay(surfaceHolder);
    }

    @Test
    public void setsDisplayToNullIfSurfaceHolderDestroyed() throws IOException {
        playUrlAndSetPrepared(videoItem);
        mediaPlayerAdapter.surfaceDestroyed(surfaceHolder);

        verify(mediaPlayer).setDisplay(null);
    }

    @Test
    public void onSurfaceChangeIsNoopAndDoesntSetDisplay() throws IOException {
        playUrlAndSetPrepared(videoItem);
        mediaPlayerAdapter.surfaceChanged(surfaceHolder, 1, 1, 1);

        verify(mediaPlayer, never()).setDisplay(any(SurfaceHolder.class));
    }

    @Test
    public void playUrlCallsPrepareAsyncOnMediaPlayer() {
        mediaPlayerAdapter.play(trackItem);
        verify(mediaPlayer).prepareAsync();
    }

    @Test
    public void onPlaybackEndedResetsRetryCount() throws IOException {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAdapter.MAX_CONNECT_RETRIES);
        mediaPlayerAdapter.onPlaybackEnded();
        causeMediaPlayerErrors(MediaPlayerAdapter.MAX_CONNECT_RETRIES);
        verify(mediaPlayer, times(4)).reset();
        verify(mediaPlayer, never()).release();
    }

    @Test
    public void onSeekResetsRetryCount() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAdapter.MAX_CONNECT_RETRIES);
        mediaPlayerAdapter.seek(10, true);
        causeMediaPlayerErrors(MediaPlayerAdapter.MAX_CONNECT_RETRIES);
        verify(mediaPlayer, never()).release();
    }

    @Test
    public void shouldReleaseMediaPlayerOnlyAfterRetryingPlaybackThreeTimes() throws IOException {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAdapter.MAX_CONNECT_RETRIES);
        verify(mediaPlayer, times(2)).reset();
        verify(mediaPlayer, times(3)).setDataSource(STREAM_URL);
        verify(mediaPlayer, never()).release();
        mediaPlayerAdapter.onError(mediaPlayer, 0, 0);
        verify(mediaPlayer).release();
    }

    @Test
    public void onErrorShouldRetryStreamPlaybacksMaxRetryTimesThenReportError() throws IOException {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAdapter.MAX_CONNECT_RETRIES + 1);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(3)).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.ERROR_FAILED, trackUrn, 0, -1, dateProvider)));
    }

    @Test
    public void onErrorShouldReturnTrueWhenRetrying() throws IOException {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        assertThat(mediaPlayerAdapter.onError(mediaPlayer, 0, 0)).isTrue();
    }

    @Test
    public void onErrorShouldReturnTrueWhenRetriesExhausted() throws IOException {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAdapter.MAX_CONNECT_RETRIES);
        assertThat(mediaPlayerAdapter.onError(mediaPlayer, 0, 0)).isTrue();
    }

    @Test
    public void shouldReturnMediaPlayerProgressAfterOnSeekCompleteCalled() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        when(mediaPlayer.getDuration()).thenReturn(duration);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        assertThat(mediaPlayerAdapter.seek(456l)).isEqualTo(456l);
        assertThat(mediaPlayerAdapter.getProgress()).isEqualTo(456l);
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);
        assertThat(mediaPlayerAdapter.getProgress()).isEqualTo(123l);
    }

    @Test
    public void onSeekCompleteShouldPauseIfInPauseState() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.pause();
        Mockito.reset(mediaPlayer);

        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer).pause();
    }

    @Test
    public void onSeekCompleteShouldNotPauseIfInPlaybackComplete() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.onPlaybackEnded();
        Mockito.reset(mediaPlayer);

        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer, never()).pause();
    }

    @Test
    public void onSeekCompleteShouldNotStartMediaPlayerIfPlaybackComplete() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.onPlaybackEnded();
        Mockito.reset(mediaPlayer);

        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer, never()).start();
    }

    @Test
    public void onSeekCompleteShouldPauseAndPlayMediaPlayerIfInPlayStateOnKitKat() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer).pause();
        verify(mediaPlayer).start();
    }

    @Test
    public void onSeekCompletePublishesPlayingEventIfNotPaused() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);

        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }


    @Test
    public void onSeekCompletePublishesPlayingEventWithAdjustedPosition() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(duration + 1);

        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, duration, duration, dateProvider)));
    }

    @Test
    public void onSeekCompleteShouldClearSeekPosThroughHandler() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(playerHandler).removeMessages(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK);
        verify(playerHandler).sendEmptyMessageDelayed(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK, MediaPlayerAdapter.SEEK_COMPLETE_PROGRESS_DELAY);
    }

    @Test
    public void stopShouldDoNothingWithIncorrectMediaPlayer() {
        mediaPlayerAdapter.stop(mediaPlayer);
        verifyZeroInteractions(listener);
    }

    @Test
    public void onBufferingListenerClearsSeekMessageThroughHandlerWhenBuffering() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);

        verify(playerHandler).removeMessages(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK);
    }

    @Test
    public void onBufferingListenerWhilePreparingDoesNotChangeState() {
        mediaPlayerAdapter.play(trackItem);
        reset(listener);

        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);

        verify(listener, never()).onPlaystateChanged(any(Player.StateTransition.class));
    }


    @Test
    public void onBufferingListenerSetsBufferingStateWhenBuffering() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void onBufferingListenerClearsSeekPosThroughHandlerWhenBufferingComplete() {
        playUrlAndSetPrepared(trackItem);

        mediaPlayerAdapter.seek(456l);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        // reflects the seek position
        assertThat(mediaPlayerAdapter.getSeekPosition()).isEqualTo(456l);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);

        // seek position cleared via handler
        verify(playerHandler, times(3)).removeMessages(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK);
        verify(playerHandler).sendEmptyMessageDelayed(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK, 3000);
    }

    @Test
    public void onBufferingListenerPausesWhenNotPlayingInOnBufferingComplete() {
        playUrlAndSetPrepared(trackItem);

        mediaPlayerAdapter.pause();
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);

        verify(mediaPlayer, times(2)).pause();
    }

    @Test
    public void shouldSetStateToPlayingAfterBufferingCompletes() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void stopDoesNothingWithNoMediaPlayer() {
        mediaPlayerAdapter.stop();
        verifyZeroInteractions(listener);
    }

    @Test
    public void stopDoesNothingIfNotStoppable() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.stop();
        verify(mediaPlayer, never()).stop();
    }

    @Test
    public void stopCallsStopAndSetsIdleStateIfStoppable() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAdapter.stop();
        verify(mediaPlayer).stop();
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void stopForTransitionCallsStopAndSetsIdleStateIfStoppable() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAdapter.stopForTrackTransition();
        verify(mediaPlayer).stop();
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void shouldNotSeekIfPlayFromPositionIsZero() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer, never()).seekTo(anyInt());
    }

    @Test
    public void shouldResumePlaybackAtSpecifiedTime() {
        when(mediaPlayer.getDuration()).thenReturn(duration);

        mediaPlayerAdapter.play(AudioPlaybackItem.create(trackUrn, 123L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT));
        mediaPlayerAdapter.onPrepared(mediaPlayer);

        InOrder inOrder = inOrder(mediaPlayer);
        inOrder.verify(mediaPlayer).start();
        inOrder.verify(mediaPlayer).seekTo(123);
        assertThat(mediaPlayerAdapter.getProgress()).isEqualTo(123L);
    }

    @Test
    public void setVolumeDoesNothingWithNoPlayer() {
        mediaPlayerAdapter.setVolume(1.0f);
        verifyZeroInteractions(mediaPlayer);
    }

    @Test
    public void setVolumeSetsVolumeOnPlayer() {
        mediaPlayerAdapter.play(trackItem);
        mediaPlayerAdapter.setVolume(1.0f);
        verify(mediaPlayer).setVolume(1.0f, 1.0f);
    }

    @Test
    public void destroyShouldCallStop() {
        playUrlAndSetPrepared(trackItem);
        mediaPlayerAdapter.destroy();
        verify(mediaPlayer).stop();
    }

    @Test
    public void destroyShouldClearHandler() {
        mediaPlayerAdapter.destroy();
        verify(playerHandler).removeCallbacksAndMessages(null);
    }

    @Test
    public void shouldNotFirePerformanceEventWhenPreparedIfUserIsNotLoggedIn(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(accountOperations, never()).getLoggedInUserUrn();
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    @Test
    public void shouldNotSendBufferUnderrunEventWhenBufferingInitially() {
        playUrlAndSetPrepared(trackItem);
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void shouldNotInteractWithMediaPlayerWhenPreparedIfUserIsNotLoggedIn(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        mediaPlayerAdapter.onPrepared(mediaPlayer);

        verifyZeroInteractions(mediaPlayer);
    }

    private void playUrlAndSetPrepared(PlaybackItem item) {
        mediaPlayerAdapter.play(item);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        reset(mediaPlayer);
        reset(mediaPlayerManager);
        reset(listener);
        when(mediaPlayer.getDuration()).thenReturn(duration);
        when(mediaPlayerManager.create()).thenReturn(mediaPlayer);
        when(listener.requestAudioFocus()).thenReturn(true);
    }

    private void causeMediaPlayerErrors(int numberOfErrors) {
        for (int i = 0; i < numberOfErrors; i++) {
            mediaPlayerAdapter.onError(mediaPlayer, 0, 0);
        }
    }

}
