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

import java.io.IOException;
import java.util.Date;

public class MediaPlayerAudioAdapterTest extends AndroidUnitTest {

    private static final String ACCESS_TOKEN = "access";
    private static final Token TOKEN = new Token(ACCESS_TOKEN,"refresh");
    private static final String STREAM_URL = "https://api-mobile.soundcloud.com/tracks/soundcloud:tracks:123/streams/http?oauth_token=access";

    private MediaPlayerAudioAdapter mediaPlayerAudioAdapter;

    @Mock private Context context;
    @Mock private MediaPlayer mediaPlayer;
    @Mock private MediaPlayerManager mediaPlayerManager;
    @Mock private MediaPlayerAudioAdapter.PlayerHandler playerHandler;
    @Mock private Player.PlayerListener listener;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private AccountOperations accountOperations;
    @Mock private BufferUnderrunListener bufferUnderrunListener;
    @Mock private StreamUrlBuilder urlBuilder;
    @Mock private CurrentDateProvider dateProvider;
    @Captor private ArgumentCaptor<Player.StateTransition> stateCaptor;

    private Urn trackUrn = Urn.forTrack(123L);
    private PlaybackItem playbackItem = AudioPlaybackItem.create(trackUrn, 0L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT);
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
        when(dateProvider.getCurrentDate()).thenReturn(new Date());

        mediaPlayerAudioAdapter = new MediaPlayerAudioAdapter(context, mediaPlayerManager, playerHandler, eventBus, networkConnectionHelper, accountOperations, bufferUnderrunListener, urlBuilder, dateProvider);
        mediaPlayerAudioAdapter.setListener(listener);
    }

    @Test
    public void constructorSetsPlayerListener() {
        verify(playerHandler).setMediaPlayerAdapter(mediaPlayerAudioAdapter);
    }

    @Test
    public void playUrlShouldCreateConfiguredMediaPlayer() {
        mediaPlayerAudioAdapter.play(playbackItem);
        verify(mediaPlayerManager).create();
        verify(mediaPlayer).setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        verify(mediaPlayer).setAudioStreamType(AudioManager.STREAM_MUSIC);
        verify(mediaPlayer).setOnPreparedListener(any(MediaPlayer.OnPreparedListener.class));
        verify(mediaPlayer).setOnSeekCompleteListener(any(MediaPlayer.OnSeekCompleteListener.class));
        verify(mediaPlayer).setOnErrorListener(any(MediaPlayer.OnErrorListener.class));
        verify(mediaPlayer).setOnCompletionListener(any(TrackCompletionListener.class));
        verify(mediaPlayer).setOnInfoListener(any(MediaPlayer.OnInfoListener.class));
        verify(mediaPlayer, never()).reset();
    }

    @Test
    public void playUrlShouldCallBufferingState() {
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);
        when(mediaPlayer.getDuration()).thenReturn(duration);
        mediaPlayerAudioAdapter.play(playbackItem);
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void preparedListenerShouldStartPlayback() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer).start();
    }

    @Test
    public void preparedListenerShouldNotStartPlaybackIfFocusNotGranted() {
        when(listener.requestAudioFocus()).thenReturn(false);
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer, never()).start();
    }

    @Test
    public void preparedListenerShouldCallStatesBufferingToPlaying() {
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);

        mediaPlayerAudioAdapter.play(playbackItem);
        when(mediaPlayer.getDuration()).thenReturn(20000);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, 0, 20000, dateProvider)));
    }

    @Test
    public void shouldAddStreamingProtocolToPlayStateEvent() {
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);

        mediaPlayerAudioAdapter.play(playbackItem);

        verify(listener).onPlaystateChanged(stateCaptor.capture());

        assertThat(stateCaptor.getValue().getExtraAttribute(Player.StateTransition.EXTRA_PLAYBACK_PROTOCOL)).isEqualTo(PlaybackProtocol.HTTPS.getValue());
    }

    @Test
    public void preparedListenerShouldReportTimeToPlay() {
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(ConnectionType.TWO_G);
        when(dateProvider.getCurrentDate()).thenReturn(new Date(0));
        mediaPlayerAudioAdapter.play(AudioPlaybackItem.create(trackUrn, 123L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT));
        when(dateProvider.getCurrentDate()).thenReturn(new Date(1000));
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);

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
    public void playUrlShouldResetAndReuseOldMediaPlayer() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.play(playbackItem);
        verify(mediaPlayer).reset();
    }

    @Test
    public void pauseShouldStopReleaseMediaPlayerIfPausedWhilePreparing() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.pause();
        verify(mediaPlayerManager).stopAndReleaseAsync(mediaPlayer);
    }

    @Test
    public void pauseNotifyIdleStateIfPausedWhilePreparing() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.pause();

        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.NONE, trackUrn, 0, -1, dateProvider)));
    }

    @Test
    public void seekShouldReturnInvalidSeekPositionWithNoMediaPlayer() {
        assertThat(mediaPlayerAudioAdapter.seek(123l)).isEqualTo(-1L);
    }

    @Test
    public void seekShouldSeekOnMediaPlayerWhilePreparing() {
        mediaPlayerAudioAdapter.play(playbackItem);
        assertThat(mediaPlayerAudioAdapter.seek(123l)).isEqualTo(123l);
        verify(mediaPlayer).seekTo(123);
    }

    @Test
    public void seekShouldCallSeekOnMediaPlayer() {
        playUrlAndSetPrepared();
        assertThat(mediaPlayerAudioAdapter.seek(123l)).isEqualTo(123l);
        verify(mediaPlayer).seekTo(123);
    }

    @Test
    public void seekShouldCallSeekOnMediaPlayerWithTimeOfZeroWhenPositionNotZero() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(1);
        assertThat(mediaPlayerAudioAdapter.seek(0L)).isEqualTo(0L);
        verify(mediaPlayer).seekTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void seekShouldThrowAnIllegalArgumentExceptionWithNegativeSeekTime() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.seek(-1L);
    }

    @Test
    public void seekShouldReturnSeekPositionWhenGettingProgressWhileSeeking() {
        playUrlAndSetPrepared();
        assertThat(mediaPlayerAudioAdapter.seek(123l)).isEqualTo(123l);
        assertThat(mediaPlayerAudioAdapter.getProgress()).isEqualTo(123l);
    }

    @Test
    public void playUrlShouldCreateNewMediaPlayerIfWaitingForSeekToReturn() {
        playUrlAndSetPrepared();
        assertThat(mediaPlayerAudioAdapter.seek(123l)).isEqualTo(123l);
        mediaPlayerAudioAdapter.play(playbackItem);
        verify(mediaPlayerManager).create();
    }

    @Test
    public void playUrlShouldReleaseOldMediaPlayerIfWaitingForSeekToReturn() {
        playUrlAndSetPrepared();
        assertThat(mediaPlayerAudioAdapter.seek(123l)).isEqualTo(123l);
        mediaPlayerAudioAdapter.play(playbackItem);
        verify(mediaPlayerManager).stopAndReleaseAsync(mediaPlayer);
    }

    @Test
    public void resumeShouldCreateNewMediaPlayerWithNoMediaPlayer() {
        mediaPlayerAudioAdapter.resume();
        verify(mediaPlayerManager).create();
    }

    @Test
    public void resumeShouldCreateNewMediaPlayerIfInPreparingState() {
        mediaPlayerAudioAdapter.play(playbackItem);
        reset(mediaPlayer);
        mediaPlayerAudioAdapter.resume();
        verify(mediaPlayerManager, times(2)).create();
    }

    @Test
    public void pauseShouldDoNothingWithNoMediaPlayer() {
        mediaPlayerAudioAdapter.pause();
        verifyNoMoreInteractions(mediaPlayer);
    }

    @Test
    public void pauseShouldCallPauseWhenMediaPlayerPlaying() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void pauseShouldCallPauseOnWhenInPauseState() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.pause();
        reset(mediaPlayer);
        mediaPlayerAudioAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void pauseShouldCallPauseOnWhenInBufferingState() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mediaPlayerAudioAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void seekShouldReturn0WithNoMediaPlayer() {
        assertThat(mediaPlayerAudioAdapter.getProgress()).isEqualTo(0l);
    }

    @Test
    public void seekShouldReturnMediaPlayerPosition() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(999);
        assertThat(mediaPlayerAudioAdapter.getProgress()).isEqualTo(999l);
    }

    @Test
    public void seekShouldReturnSeekPositionIfWaitingForSeek() {
        playUrlAndSetPrepared();
        assertThat(mediaPlayerAudioAdapter.seek(123l)).isEqualTo(123l);
        assertThat(mediaPlayerAudioAdapter.getProgress()).isEqualTo(123l);
    }

    @Test
    public void seekRemovesSeekPosClearingThroughHandlerThroughHandler() {
        playUrlAndSetPrepared();

        mediaPlayerAudioAdapter.seek(456l);
        // seek position cleared via handler
        verify(playerHandler).removeMessages(MediaPlayerAudioAdapter.PlayerHandler.CLEAR_LAST_SEEK);
    }

    @Test
    public void playUrlShouldRetryMaxTimesIfMediaPlayerFailsToPrepare() throws IOException {
        Mockito.doThrow(new IOException()).when(mediaPlayer).setDataSource(any(String.class));
        mediaPlayerAudioAdapter.play(playbackItem);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(3)).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.ERROR_FAILED, trackUrn, 0, -1, dateProvider)));
    }

    @Test
    public void playUrlSetsDataSourceOnMediaPlayer() throws IOException {
        mediaPlayerAudioAdapter.play(playbackItem);
        verify(mediaPlayer).setDataSource(STREAM_URL);
    }

    @Test
    public void playUrlCallsPrepareAsyncOnMediaPlayer() {
        mediaPlayerAudioAdapter.play(playbackItem);
        verify(mediaPlayer).prepareAsync();
    }

    @Test
    public void onTrackEndedResetsRetryCount() throws IOException {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAudioAdapter.MAX_CONNECT_RETRIES);
        mediaPlayerAudioAdapter.onTrackEnded();
        causeMediaPlayerErrors(MediaPlayerAudioAdapter.MAX_CONNECT_RETRIES);
        verify(mediaPlayer, times(4)).reset();
        verify(mediaPlayer, never()).release();
    }

    @Test
    public void onSeekResetsRetryCount() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAudioAdapter.MAX_CONNECT_RETRIES);
        mediaPlayerAudioAdapter.seek(10, true);
        causeMediaPlayerErrors(MediaPlayerAudioAdapter.MAX_CONNECT_RETRIES);
        verify(mediaPlayer, never()).release();
    }

    @Test
    public void shouldReleaseMediaPlayerOnlyAfterRetryingPlaybackThreeTimes() throws IOException {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAudioAdapter.MAX_CONNECT_RETRIES);
        verify(mediaPlayer, times(2)).reset();
        verify(mediaPlayer, times(3)).setDataSource(STREAM_URL);
        verify(mediaPlayer, never()).release();
        mediaPlayerAudioAdapter.onError(mediaPlayer, 0, 0);
        verify(mediaPlayer).release();
    }

    @Test
    public void onErrorShouldRetryStreamPlaybacksMaxRetryTimesThenReportError() throws IOException {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAudioAdapter.MAX_CONNECT_RETRIES + 1);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(3)).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 0, -1, dateProvider)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.ERROR_FAILED, trackUrn, 0, -1, dateProvider)));
    }

    @Test
    public void onErrorShouldReturnTrueWhenRetrying() throws IOException {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        assertThat(mediaPlayerAudioAdapter.onError(mediaPlayer, 0, 0)).isTrue();
    }

    @Test
    public void onErrorShouldReturnTrueWhenRetriesExhausted() throws IOException {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        causeMediaPlayerErrors(MediaPlayerAudioAdapter.MAX_CONNECT_RETRIES);
        assertThat(mediaPlayerAudioAdapter.onError(mediaPlayer, 0, 0)).isTrue();
    }

    @Test
    public void shouldReturnMediaPlayerProgressAfterOnSeekCompleteCalled() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        when(mediaPlayer.getDuration()).thenReturn(duration);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        assertThat(mediaPlayerAudioAdapter.seek(456l)).isEqualTo(456l);
        assertThat(mediaPlayerAudioAdapter.getProgress()).isEqualTo(456l);
        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);
        assertThat(mediaPlayerAudioAdapter.getProgress()).isEqualTo(123l);
    }

    @Test
    public void onSeekCompleteShouldPauseIfInPauseState() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.pause();
        Mockito.reset(mediaPlayer);

        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer).pause();
    }

    @Test
    public void onSeekCompleteShouldNotPauseIfInTrackComplete() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.onTrackEnded();
        Mockito.reset(mediaPlayer);

        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer, never()).pause();
    }

    @Test
    public void onSeekCompleteShouldNotStartMediaPlayerIfTrackComplete() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.onTrackEnded();
        Mockito.reset(mediaPlayer);

        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer, never()).start();
    }

    @Test
    public void onSeekCompleteShouldPauseAndPlayMediaPlayerIfInPlayStateOnKitKat() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer).pause();
        verify(mediaPlayer).start();
    }

    @Test
    public void onSeekCompletePublishesPlayingEventIfNotPaused() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);

        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }


    @Test
    public void onSeekCompletePublishesPlayingEventWithAdjustedPosition() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(duration + 1);

        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, duration, duration, dateProvider)));
    }

    @Test
    public void onSeekCompleteShouldClearSeekPosThroughHandler() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        verify(playerHandler).removeMessages(MediaPlayerAudioAdapter.PlayerHandler.CLEAR_LAST_SEEK);
        verify(playerHandler).sendEmptyMessageDelayed(MediaPlayerAudioAdapter.PlayerHandler.CLEAR_LAST_SEEK, MediaPlayerAudioAdapter.SEEK_COMPLETE_PROGRESS_DELAY);
    }

    @Test
    public void stopShouldDoNothingWithIncorrectMediaPlayer() {
        mediaPlayerAudioAdapter.stop(mediaPlayer);
        verifyZeroInteractions(listener);
    }

    @Test
    public void onBufferingListenerClearsSeekMessageThroughHandlerWhenBuffering() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);

        verify(playerHandler).removeMessages(MediaPlayerAudioAdapter.PlayerHandler.CLEAR_LAST_SEEK);
    }

    @Test
    public void onBufferingListenerWhilePreparingDoesNotChangeState() {
        mediaPlayerAudioAdapter.play(playbackItem);
        reset(listener);

        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);

        verify(listener, never()).onPlaystateChanged(any(Player.StateTransition.class));
    }


    @Test
    public void onBufferingListenerSetsBufferingStateWhenBuffering() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.BUFFERING, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void onBufferingListenerClearsSeekPosThroughHandlerWhenBufferingComplete() {
        playUrlAndSetPrepared();

        mediaPlayerAudioAdapter.seek(456l);
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mediaPlayerAudioAdapter.onSeekComplete(mediaPlayer);

        // reflects the seek position
        assertThat(mediaPlayerAudioAdapter.getSeekPosition()).isEqualTo(456l);
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);

        // seek position cleared via handler
        verify(playerHandler, times(3)).removeMessages(MediaPlayerAudioAdapter.PlayerHandler.CLEAR_LAST_SEEK);
        verify(playerHandler).sendEmptyMessageDelayed(MediaPlayerAudioAdapter.PlayerHandler.CLEAR_LAST_SEEK, 3000);
    }

    @Test
    public void onBufferingListenerPausesWhenNotPlayingInOnBufferingComplete() {
        playUrlAndSetPrepared();

        mediaPlayerAudioAdapter.pause();
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);

        verify(mediaPlayer, times(2)).pause();
    }

    @Test
    public void shouldSetStateToPlayingAfterBufferingCompletes() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.PLAYING, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void stopDoesNothingWithNoMediaPlayer() {
        mediaPlayerAudioAdapter.stop();
        verifyZeroInteractions(listener);
    }

    @Test
    public void stopDoesNothingIfNotStoppable() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.stop();
        verify(mediaPlayer, never()).stop();
    }

    @Test
    public void stopCallsStopAndSetsIdleStateIfStoppable() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAudioAdapter.stop();
        verify(mediaPlayer).stop();
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void stopForTransitionCallsStopAndSetsIdleStateIfStoppable() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        mediaPlayerAudioAdapter.stopForTrackTransition();
        verify(mediaPlayer).stop();
        verify(listener).onPlaystateChanged(eq(new Player.StateTransition(PlayerState.IDLE, Reason.NONE, trackUrn, 123, duration, dateProvider)));
    }

    @Test
    public void shouldNotSeekIfPlayFromPositionIsZero() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer, never()).seekTo(anyInt());
    }

    @Test
    public void shouldResumePlaybackAtSpecifiedTime() {
        when(mediaPlayer.getDuration()).thenReturn(duration);

        mediaPlayerAudioAdapter.play(AudioPlaybackItem.create(trackUrn, 123L, Consts.NOT_SET, PlaybackType.AUDIO_DEFAULT));
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);

        InOrder inOrder = inOrder(mediaPlayer);
        inOrder.verify(mediaPlayer).start();
        inOrder.verify(mediaPlayer).seekTo(123);
        assertThat(mediaPlayerAudioAdapter.getProgress()).isEqualTo(123L);
    }

    @Test
    public void setVolumeDoesNothingWithNoPlayer() {
        mediaPlayerAudioAdapter.setVolume(1.0f);
        verifyZeroInteractions(mediaPlayer);
    }

    @Test
    public void setVolumeSetsVolumeOnPlayer() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.setVolume(1.0f);
        verify(mediaPlayer).setVolume(1.0f, 1.0f);
    }

    @Test
    public void destroyShouldCallStop() {
        playUrlAndSetPrepared();
        mediaPlayerAudioAdapter.destroy();
        verify(mediaPlayer).stop();
    }

    @Test
    public void destroyShouldClearHandler() {
        mediaPlayerAudioAdapter.destroy();
        verify(playerHandler).removeCallbacksAndMessages(null);
    }

    @Test
    public void shouldNotFirePerformanceEventWhenPreparedIfUserIsNotLoggedIn(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        verify(accountOperations, never()).getLoggedInUserUrn();
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_PERFORMANCE);
    }

    @Test
    public void shouldNotSendBufferUnderrunEventWhenBufferingInitially() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(0);
        mediaPlayerAudioAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void shouldNotInteractWithMediaPlayertWhenPreparedIfUserIsNotLoggedIn(){
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);

        verifyZeroInteractions(mediaPlayer);
    }

    private void playUrlAndSetPrepared() {
        mediaPlayerAudioAdapter.play(playbackItem);
        mediaPlayerAudioAdapter.onPrepared(mediaPlayer);
        reset(mediaPlayer);
        reset(mediaPlayerManager);
        reset(listener);
        when(mediaPlayer.getDuration()).thenReturn(duration);
        when(mediaPlayerManager.create()).thenReturn(mediaPlayer);
        when(listener.requestAudioFocus()).thenReturn(true);
    }

    private void causeMediaPlayerErrors(int numberOfErrors) {
        for (int i = 0; i < numberOfErrors; i++) {
            mediaPlayerAudioAdapter.onError(mediaPlayer, 0, 0);
        }
    }

}
