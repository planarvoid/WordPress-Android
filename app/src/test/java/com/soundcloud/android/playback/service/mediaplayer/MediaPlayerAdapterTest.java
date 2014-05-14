package com.soundcloud.android.playback.service.mediaplayer;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.events.PlaybackPerformanceEvent.PlayerType;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
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

import com.nostra13.universalimageloader.utils.L;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.StreamPlaya;
import com.soundcloud.android.playback.streaming.StreamProxy;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import java.io.IOException;


@RunWith(SoundCloudTestRunner.class)
public class MediaPlayerAdapterTest {

    public static final String STREAM_URL = "http://url.com";
    public static final Uri STREAM_URI = Uri.parse("http://url.com");
    public static final int DURATION = 10000;
    MediaPlayerAdapter mediaPlayerAdapter;

    @Mock
    private Context context;
    @Mock
    private MediaPlayer mediaPlayer;
    @Mock
    private MediaPlayerAdapter.MediaPlayerManager mediaPlayerManager;
    @Mock
    private StreamProxy streamProxy;
    @Mock
    private MediaPlayerAdapter.PlayerHandler playerHandler;
    @Mock
    private StreamPlaya.PlayaListener listener;
    @Mock
    private Track track;
    @Mock
    private EventBus eventBus;
    @Mock
    private NetworkConnectionHelper networkConnectionHelper;

    @Before
    public void setUp() throws Exception {
        when(context.getApplicationContext()).thenReturn(context);
        when(mediaPlayerManager.create()).thenReturn(mediaPlayer);
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(TestObservables.MockObservable.<Uri>empty());
        when(listener.requestAudioFocus()).thenReturn(true);
        when(mediaPlayer.getDuration()).thenReturn(DURATION);
        when(track.getStreamUrlWithAppendedId()).thenReturn(STREAM_URL);
        when(track.getStreamUrl()).thenReturn(STREAM_URL);
        mediaPlayerAdapter = new MediaPlayerAdapter(context, mediaPlayerManager, streamProxy, playerHandler, eventBus, networkConnectionHelper);
        mediaPlayerAdapter.setListener(listener);
    }

    @Test
    public void constructorShouldStartProxy() throws Exception {
        verify(streamProxy).start();
    }

    @Test
    public void constructorSetsPlayerListener() throws Exception {
        verify(playerHandler).setMediaPlayerAdapter(mediaPlayerAdapter);
    }

    @Test
    public void playUrlShouldCreateConfiguredMediaPlayer() throws Exception {
        mediaPlayerAdapter.play(track);
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
    public void playUrlShouldCallBufferingState() throws Exception {
        mediaPlayerAdapter.play(track);
        verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.BUFFERING, Reason.NONE)));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void preparedListenerShouldStartPlayback() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer).start();
    }

    @Test
    public void preparedListenerShouldNotStartPlaybackIfFocusNotGranted() throws Exception {
        when(listener.requestAudioFocus()).thenReturn(false);
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer, never()).start();
    }

    @Test
    public void preparedListenerShouldCallStatesBufferingToPlaying() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE)));
    }

    @Test
    public void preparedListenerShouldReportTimeToPlay() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        when(track.getStreamUrl()).thenReturn("https://stream-url");
        when(networkConnectionHelper.getCurrentConnectionType()).thenReturn(PlaybackPerformanceEvent.ConnectionType.TWO_G);

        mediaPlayerAdapter.play(track, 123L);
        mediaPlayerAdapter.onPrepared(mediaPlayer);

        final PlaybackPerformanceEvent event = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_PERFORMANCE);
        expect(event.getMetric()).toEqual(PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY);
        expect(event.getMetricValue()).toBeGreaterThan(0L);
        expect(event.getCdnHost()).toEqual(Uri.parse(track.getStreamUrl()).getHost());
        expect(event.getPlayerType()).toEqual(PlayerType.MEDIA_PLAYER);
        expect(event.getProtocol()).toEqual(PlaybackProtocol.HTTPS);
        expect(event.getConnectionType()).toEqual(PlaybackPerformanceEvent.ConnectionType.TWO_G);
    }

    @Test
    public void playUrlShouldResetAndReuseOldMediaPlayer() throws Exception {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.play(track);
        verify(mediaPlayer).reset();
    }

    @Test
    public void pauseShouldStopReleaseMediaPlayerIfPausedWhilePreparing() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.pause();
        verify(mediaPlayerManager).stopAndReleaseAsync(mediaPlayer);
    }

    @Test
    public void pauseNotifyIdleStateIfPausedWhilePreparing() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.pause();
        verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.IDLE, Reason.NONE)));
    }

    @Test
    public void playUrlShouldSubscribeToProxyObservable() throws Exception {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        mediaPlayerAdapter.play(track);
        verify(mediaPlayer).setDataSource(STREAM_URL);
        verify(mediaPlayer).prepareAsync();
    }

    @Test
    public void seekShouldReturnInvalidSeekPositionWithNoMediaPlayer() {
        expect(mediaPlayerAdapter.seek(123l)).toEqual(-1L);
    }

    @Test
    public void seekShouldNotSeekOnMediaPlayerWhilePreparing() {
        mediaPlayerAdapter.play(track);
        expect(mediaPlayerAdapter.seek(123l)).toEqual(-1L);
        verify(mediaPlayer, never()).seekTo(anyInt());
    }

    @Test
    public void seekShouldCallSeekOnMediaPlayer() {
        playUrlAndSetPrepared();
        expect(mediaPlayerAdapter.seek(123l)).toEqual(123l);
        verify(mediaPlayer).seekTo(123);
    }

    @Test
    public void seekShouldCallSeekOnMediaPlayerWithTimeOfZeroWhenPositionNotZero() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(1);
        expect(mediaPlayerAdapter.seek(0L)).toEqual(0L);
        verify(mediaPlayer).seekTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void seekShouldThrowAnIllegalArgumentExceptionWithNegativeSeekTime() {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.seek(-1L);
    }

    @Test
    public void seekShouldReturnSeekPositionWhenGettingProgressWhileSeeking() {
        playUrlAndSetPrepared();
        expect(mediaPlayerAdapter.seek(123l)).toEqual(123l);
        expect(mediaPlayerAdapter.getProgress()).toBe(123l);
    }

    @Test
    public void playUrlShouldCreateNewMediaPlayerIfWaitingForSeekToReturn() {
        playUrlAndSetPrepared();
        expect(mediaPlayerAdapter.seek(123l)).toEqual(123l);
        mediaPlayerAdapter.play(track);
        verify(mediaPlayerManager).create();
    }

    @Test
    public void playUrlShouldReleaseOldMediaPlayerIfWaitingForSeekToReturn() {
        playUrlAndSetPrepared();
        expect(mediaPlayerAdapter.seek(123l)).toEqual(123l);
        mediaPlayerAdapter.play(track);
        verify(mediaPlayerManager).stopAndReleaseAsync(mediaPlayer);
    }

    @Test
    public void resumeShouldReturnFalseWithNoMediaPlayer() {
        expect(mediaPlayerAdapter.resume()).toBeFalse();
    }

    @Test
    public void resumeShouldDoNothingWithNoMediaPlayer() {
        mediaPlayerAdapter.resume();
        verifyNoMoreInteractions(mediaPlayer);
    }

    @Test
    public void resumeShouldReturnFalseIfInPreparingState() {
        mediaPlayerAdapter.play(track);
        expect(mediaPlayerAdapter.resume()).toBeFalse();
    }

    @Test
    public void resumeShouldDoNothingIfInPreparingState() {
        mediaPlayerAdapter.play(track);
        reset(mediaPlayer);
        mediaPlayerAdapter.resume();
        verifyNoMoreInteractions(mediaPlayer);
    }

    @Test
    public void pauseShouldDoNothingWithNoMediaPlayer() {
        mediaPlayerAdapter.pause();
        verifyNoMoreInteractions(mediaPlayer);
    }

    @Test
    public void pauseShouldCallPauseWhenMediaPlayerPlaying() {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void pauseShouldCallPauseOnWhenInPauseState() {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.pause();
        reset(mediaPlayer);
        mediaPlayerAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void pauseShouldCallPauseOnWhenInBufferingState() {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mediaPlayerAdapter.pause();
        verify(mediaPlayer).pause();
    }

    @Test
    public void seekShouldReturn0WithNoMediaPlayer() {
        expect(mediaPlayerAdapter.getProgress()).toBe(0l);
    }

    @Test
    public void seekShouldReturnMediaPlayerPosition() {
        playUrlAndSetPrepared();
        when(mediaPlayer.getCurrentPosition()).thenReturn(999);
        expect(mediaPlayerAdapter.getProgress()).toEqual(999l);
    }

    @Test
    public void seekShouldReturnSeekPositionIfWaitingForSeek() {
        playUrlAndSetPrepared();
        expect(mediaPlayerAdapter.seek(123l)).toEqual(123l);
        expect(mediaPlayerAdapter.getProgress()).toBe(123l);
    }

    @Test
    public void playUrlShouldSetErrorStateIfProxyObservableCallsOnError() throws Exception {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.<Uri>error(new IOException("uhoh")));
        mediaPlayerAdapter.play(track);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.BUFFERING, Reason.NONE)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED)));
    }

    @Test
    public void playUrlShouldRetryMaxTimesIfMediaPlayerFailsToPrepare() throws Exception {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        Mockito.doThrow(new IOException()).when(mediaPlayer).setDataSource(any(String.class));
        mediaPlayerAdapter.play(track);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(4)).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.BUFFERING, Reason.NONE)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED)));
    }

    @Test
    public void playUrlSetsDataSourceOnMediaPlayer() throws Exception {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        mediaPlayerAdapter.play(track);
        verify(mediaPlayer).setDataSource(STREAM_URI.toString());
    }

    @Test
    public void playUrlCallsPrepareAsyncOnMediaPlayer() throws Exception {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        mediaPlayerAdapter.play(track);
        verify(mediaPlayer).prepareAsync();
    }

    @Test
    public void playUrlUnsubscribesFromPreviousProxySubscription() throws Exception {
        final Subscription subscription = Mockito.mock(Subscription.class);
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(TestObservables.<Uri>endlessObservablefromSubscription(subscription));
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.play(track);
        verify(subscription).unsubscribe();
    }

    @Test
    public void stopUnsubscribesFromPreviousProxySubscription() throws Exception {
        final Subscription subscription = Mockito.mock(Subscription.class);
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(TestObservables.<Uri>endlessObservablefromSubscription(subscription));
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.stop();
        verify(subscription).unsubscribe();
    }

    @Test
    public void shouldReleaseMediaPlayerOnlyAfterRetryingPlaybackThreeTimes() throws IOException {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        for (int i = 0; i < MediaPlayerAdapter.MAX_CONNECT_RETRIES; i++) {
            mediaPlayerAdapter.onError(mediaPlayer, 0, 0);
        }
        verify(mediaPlayer, times(3)).reset();
        verify(mediaPlayer, times(4)).setDataSource(STREAM_URL);
        verify(mediaPlayer, never()).release();
        mediaPlayerAdapter.onError(mediaPlayer, 0, 0);
        verify(mediaPlayer).release();
    }

    @Test
    public void onErroShouldRetryStreamPlaybacksMaxRetryTimesThenReportError() throws IOException {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        for (int i = 0; i < MediaPlayerAdapter.MAX_CONNECT_RETRIES + 1; i++) {
            mediaPlayerAdapter.onError(mediaPlayer, 0, 0);
        }

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener, times(4)).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.BUFFERING, Reason.NONE)));
        inOrder.verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED)));
    }

    @Test
    public void onErrorShouldReturnTrueWhenRetrying() throws IOException {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        expect(mediaPlayerAdapter.onError(mediaPlayer, 0, 0)).toBeTrue();
    }

    @Test
    public void onErrorShouldReturnTrueWhenRetriesExhausted() throws IOException {
        when(streamProxy.uriObservable(STREAM_URL, null)).thenReturn(Observable.just(STREAM_URI));
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        for (int i = 0; i < MediaPlayerAdapter.MAX_CONNECT_RETRIES; i++) {
            mediaPlayerAdapter.onError(mediaPlayer, 0, 0);
        }
        expect(mediaPlayerAdapter.onError(mediaPlayer, 0, 0)).toBeTrue();
    }

    @Test
    public void shouldReturnMediaPlayerProgressAfterOnSeekCompleteCalled() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        when(mediaPlayer.getCurrentPosition()).thenReturn(123);
        expect(mediaPlayerAdapter.seek(456l)).toEqual(456l);
        expect(mediaPlayerAdapter.getProgress()).toEqual(456l);
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);
        expect(mediaPlayerAdapter.getProgress()).toEqual(123l);
    }

    @Test
    public void onSeekCompleteShouldPauseIfInPauseState() throws Exception {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.pause();
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);
        verify(mediaPlayer, times(2)).pause();
    }

    @Test
    public void onSeekCompleteShouldPauseAndPlayMediaPlayerIfInPlayStateOnKitKat() throws Exception {
        TestHelper.setSdkVersion(Build.VERSION_CODES.JELLY_BEAN + 1);
        playUrlAndSetPrepared();
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(mediaPlayer).pause();
        verify(mediaPlayer).start();
    }

    @Test
    public void onSeekCompleteShouldClearSeekPosThroughHandler() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        verify(playerHandler).removeMessages(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK);
        verify(playerHandler).sendEmptyMessageDelayed(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK, MediaPlayerAdapter.SEEK_COMPLETE_PROGRESS_DELAY);
    }

    @Test
    public void stopShouldDoNothingWithIncorrectMediaPlayer() throws Exception {
        mediaPlayerAdapter.stop(mediaPlayer);
        verifyZeroInteractions(listener);
    }

    @Test
    public void onBufferingListenerClearsSeekMessageThroughHandlerWhenBuffering() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);

        verify(playerHandler).removeMessages(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK);
    }

    @Test
    public void onBufferingListenerSetsBufferingStateWhenBuffering() throws Exception {
        playUrlAndSetPrepared();

        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.BUFFERING, Reason.NONE)));
    }

    @Test
    public void onBufferingListenerClearsSeekPosThroughHandlerWhenBufferingComplete() throws Exception {
        playUrlAndSetPrepared();

        mediaPlayerAdapter.seek(456l);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
        mediaPlayerAdapter.onSeekComplete(mediaPlayer);

        // reflects the seek position
        expect(mediaPlayerAdapter.getSeekPosition()).toEqual(456l);
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);

        // seek position cleared via handler
        verify(playerHandler, times(2)).removeMessages(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK);
        verify(playerHandler).sendEmptyMessageDelayed(MediaPlayerAdapter.PlayerHandler.CLEAR_LAST_SEEK, 3000);
    }

    @Test
    public void onBufferingListenerPausesWhenNotPlayingInOnBufferingComplete() throws Exception {
        playUrlAndSetPrepared();

        mediaPlayerAdapter.pause();
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);

        verify(mediaPlayer, times(2)).pause();
    }

    @Test
    public void shouldSetStateToPlayingAfterBufferingCompletes() throws Exception {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.onInfo(mediaPlayer, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
        verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE)));
    }

    @Test
    public void stopDoesNothingWithNoMediaPlayer() throws Exception {
        mediaPlayerAdapter.stop();
        verifyZeroInteractions(listener);
    }

    @Test
    public void stopDoesNothingIfNotStoppable() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.stop();
        verify(mediaPlayer, never()).stop();
    }

    @Test
    public void stopCallsStopAndSetsIdleStateIfStoppable() throws Exception {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.stop();
        verify(mediaPlayer).stop();
        verify(listener).onPlaystateChanged(eq(new Playa.StateTransition(PlayaState.IDLE, Reason.NONE)));
    }

    @Test
    public void shouldNotSeekIfPlayFromPositionIsZero() throws Exception {
        mediaPlayerAdapter.play(track, 0L);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        verify(mediaPlayer, never()).seekTo(anyInt());
    }

    @Test
    public void shouldResumePlaybackAtSpecifiedTime() throws Exception {
        mediaPlayerAdapter.play(track, 123L);
        mediaPlayerAdapter.onPrepared(mediaPlayer);

        InOrder inOrder = inOrder(mediaPlayer);
        inOrder.verify(mediaPlayer).start();
        inOrder.verify(mediaPlayer).seekTo(123);
        expect(mediaPlayerAdapter.getProgress()).toEqual(123L);
    }

    @Test
    public void setVolumeDoesNothingWithNoPlayer() throws Exception {
        mediaPlayerAdapter.setVolume(1.0f);
        verifyZeroInteractions(mediaPlayer);
    }

    @Test
    public void setVolumeSetsVolumeOnPlayer() throws Exception {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.setVolume(1.0f);
        verify(mediaPlayer).setVolume(1.0f, 1.0f);
    }

    @Test
    public void destroyShouldCallStop() throws Exception {
        playUrlAndSetPrepared();
        mediaPlayerAdapter.destroy();
        verify(mediaPlayer).stop();
    }

    @Test
    public void destroyShouldClearHandler() throws Exception {
        mediaPlayerAdapter.destroy();
        verify(playerHandler).removeCallbacksAndMessages(null);
    }

    @Test
    public void destroyShouldStopProxy() throws Exception {
        when(streamProxy.isRunning()).thenReturn(true);
        mediaPlayerAdapter.destroy();
        verify(streamProxy).stop();
    }

    private void playUrlAndSetPrepared() {
        mediaPlayerAdapter.play(track);
        mediaPlayerAdapter.onPrepared(mediaPlayer);
        reset(mediaPlayer);
        reset(mediaPlayerManager);
        reset(listener);
        when(mediaPlayer.getDuration()).thenReturn(DURATION);
        when(mediaPlayerManager.create()).thenReturn(mediaPlayer);
        when(listener.requestAudioFocus()).thenReturn(true);

    }
}
