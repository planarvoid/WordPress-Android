package com.soundcloud.android.playback.flipper;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.crypto.DeviceSecret;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.HlsStreamUrlBuilder;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PreloadItem;
import com.soundcloud.android.playback.common.ProgressChangeHandler;
import com.soundcloud.android.playback.common.StateChangeHandler;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackItem;
import com.soundcloud.android.testsupport.fixtures.TestPreloadItem;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.LockUtil;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.flippernative.api.ErrorReason;
import com.soundcloud.flippernative.api.PlayerState;
import com.soundcloud.flippernative.api.StreamingProtocol;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.support.annotation.NonNull;

public class FlipperAdapterTest extends AndroidUnitTest {
    private static final String CDN_HOST = "ec-rtmp-media.soundcloud.com";
    private static final String OPUS = PlaybackConstants.MediaType.OPUS;
    private static final int BITRATE = 128000;

    private static final long POSITION = 500L;
    private static final long DURATION = 1000L;
    private FlipperAdapter flipperAdapter;
    private TestEventBusV2 eventBus = new TestEventBusV2();
    private CurrentDateProvider dateProvider = new TestDateProvider();

    @Mock FlipperWrapperFactory flipperWrapperFactory;
    @Mock FlipperWrapper flipperWrapper;
    @Mock AccountOperations accountOperations;
    @Mock HlsStreamUrlBuilder hlsStreamUrlBuilder;
    @Mock ConnectionHelper connectionHelper;
    @Mock LockUtil lockUtil;
    @Mock StateChangeHandler stateChangeHandler;
    @Mock ProgressChangeHandler progressChangeHandler;
    @Mock CryptoOperations cryptoOperations;
    @Mock PerformanceReporter performanceReporter;

    @Captor ArgumentCaptor<PlaybackStateTransition> transitionCaptor;

    @Before
    public void setUp() throws Exception {
        when(flipperWrapperFactory.create(any(FlipperAdapter.class))).thenReturn(flipperWrapper);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        flipperAdapter = new FlipperAdapter(flipperWrapperFactory,
                                            accountOperations,
                                            hlsStreamUrlBuilder,
                                            connectionHelper,
                                            lockUtil,
                                            stateChangeHandler,
                                            progressChangeHandler,
                                            dateProvider,
                                            eventBus,
                                            cryptoOperations,
                                            performanceReporter);
    }

    @Test
    public void preloadingForwardsCorrectUrl() {
        final PreloadItem preloadItem = TestPreloadItem.audio();
        final String mediaUri = "http://fakeUrl.com";
        when(hlsStreamUrlBuilder.buildStreamUrl(preloadItem)).thenReturn(mediaUri);

        flipperAdapter.preload(preloadItem);

        verify(flipperWrapper).prefetch(mediaUri);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsAndDoesNotPlayTrackIfUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        flipperAdapter.play(TestPlaybackItem.audio());

        verifyZeroInteractions(flipperWrapper);
    }

    @Test
    public void playerListenerIsSetInEveryHandler() {
        Player.PlayerListener listener = mock(Player.PlayerListener.class);

        flipperAdapter.setListener(listener);

        verify(stateChangeHandler).setPlayerListener(listener);
        verify(progressChangeHandler).setPlayerListener(listener);
    }

    @Test
    public void ensureFlipperPlayerType() {
        assertThat(flipperAdapter.getPlayerType()).isEqualTo(PlayerType.FLIPPER);
    }

    @Test
    public void playingANonOpenedAudioStreamWillOpenItAndStartPlayback() {
        final PlaybackItem playbackItem = TestPlaybackItem.audio();
        final String mediaUri = "http://fakeStream.com";
        when(hlsStreamUrlBuilder.buildStreamUrl(playbackItem)).thenReturn(mediaUri);

        flipperAdapter.play(playbackItem);

        InOrder orderVerifier = Mockito.inOrder(flipperWrapper);
        orderVerifier.verify(flipperWrapper).open(eq(mediaUri), eq(playbackItem.getStartPosition()));
        orderVerifier.verify(flipperWrapper).play();
    }

    @Test
    public void playingAnOfflineAudioStreamWillOpenItWithTheEncryptionKeys() {
        final PlaybackItem playbackItem = TestPlaybackItem.offline();
        final String mediaUri = "http://fakeStream.com";
        final DeviceSecret deviceSecret = mock(DeviceSecret.class);
        final byte[] key = "key".getBytes();
        final byte[] initVector = "vector".getBytes();
        when(hlsStreamUrlBuilder.buildStreamUrl(playbackItem)).thenReturn(mediaUri);
        when(deviceSecret.getKey()).thenReturn(key);
        when(deviceSecret.getInitVector()).thenReturn(initVector);
        when(cryptoOperations.checkAndGetDeviceKey()).thenReturn(deviceSecret);

        flipperAdapter.play(playbackItem);

        verify(flipperWrapper, never()).open(anyString(), anyLong());
        InOrder orderVerifier = Mockito.inOrder(flipperWrapper);
        orderVerifier.verify(flipperWrapper).openEncrypted(eq(mediaUri), eq(key), eq(initVector), eq(playbackItem.getStartPosition()));
        orderVerifier.verify(flipperWrapper).play();
    }

    @Test(expected = IllegalStateException.class)
    public void ensureFlipperDoesNotPlayAudioAds() {
        AudioPlaybackItem audioAd = TestPlaybackItem.audioAd();
        final String mediaUri = "http://fakeStream.com";
        when(hlsStreamUrlBuilder.buildStreamUrl(audioAd)).thenReturn(mediaUri);

        flipperAdapter.play(audioAd);
    }

    @Test(expected = IllegalStateException.class)
    public void ensureFlipperDoesNotPlayVideoAds() {
        AudioPlaybackItem videoAd = TestPlaybackItem.videoAd();
        final String mediaUri = "http://fakeStream.com";
        when(hlsStreamUrlBuilder.buildStreamUrl(videoAd)).thenReturn(mediaUri);

        flipperAdapter.play(TestPlaybackItem.videoAd());
    }

    @Test
    public void playingAnAlreadyOpenedStreamSeeksInsteadOfReopeningIt() {
        final PlaybackItem playbackItem = TestPlaybackItem.audio();
        final String mediaUri = "http://fakeStream.com";
        when(hlsStreamUrlBuilder.buildStreamUrl(playbackItem)).thenReturn(mediaUri);

        flipperAdapter.play(playbackItem);
        flipperAdapter.play(playbackItem);

        verify(flipperWrapper, times(2)).play();
        verify(flipperWrapper, times(1)).open(eq(mediaUri), anyLong());
        InOrder orderVerifier = Mockito.inOrder(flipperWrapper);
        orderVerifier.verify(flipperWrapper).seek(playbackItem.getStartPosition());
        orderVerifier.verify(flipperWrapper).play();
    }

    @Test
    public void ensureProgressChangingIsReported() {
        AudioPlaybackItem audioPlaybackItem = TestPlaybackItem.audio();
        whenPlaying(audioPlaybackItem);
        long position = 23456L;
        long duration = 12345678L;
        ProgressChange progressChange = new ProgressChange(fakeMediaUri(audioPlaybackItem.getUrn()),
                                                           position, duration);

        flipperAdapter.onProgressChanged(progressChange);

        verify(progressChangeHandler).report(position, duration);
    }

    @Test
    public void afterTrackChangeProgressIsNotReportedForPreviousTrackInMultithreadScenario() {
        AudioPlaybackItem audioPlaybackItem = TestPlaybackItem.audio(Urn.forTrack(2345678L));
        AudioPlaybackItem otherAudioPlaybackItem = TestPlaybackItem.audio(Urn.forTrack(7643223456L));

        whenPlaying(audioPlaybackItem);
        ProgressChange progressChange = new ProgressChange(fakeMediaUri(otherAudioPlaybackItem.getUrn()),
                                                           123L, 765432L);

        flipperAdapter.onProgressChanged(progressChange);

        verify(progressChangeHandler, never()).report(anyLong(), anyLong());
    }

    @Test
    public void performanceEventIsForwardedToReporter() {
        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        AudioPerformanceEvent audioPerformance = new AudioPerformanceEvent("type", 1234L, PlaybackProtocol.ENCRYPTED_HLS.getValue(), CDN_HOST, OPUS, BITRATE, null);

        flipperAdapter.onPerformanceEvent(audioPerformance);

        verify(performanceReporter).report(playbackItem, audioPerformance, PlayerType.FLIPPER);
    }

    @Test
    public void performanceEventNotBackedByPlaybackItemIsNotReported() {
        // Setting up things: connected to the internet + playing a track
        ConnectionType connectionType = ConnectionType.WIFI;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);
        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);

        // Completing the playback will clear the underlying playback item set as currently playing
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Completed, ErrorReason.Nothing, POSITION, DURATION);
        flipperAdapter.onStateChanged(stateChange);

        // Suppose a second thread posts a performance event after the playback item is disposed
        AudioPerformanceEvent audioPerformance = new AudioPerformanceEvent("type", 1234L, PlaybackProtocol.ENCRYPTED_HLS.getValue(), CDN_HOST, OPUS, BITRATE, null);
        flipperAdapter.onPerformanceEvent(audioPerformance);

        verify(performanceReporter, never()).report(any(PlaybackItem.class), any(AudioPerformanceEvent.class), any(PlayerType.class));
    }

    @Test
    public void bufferingStateIsReportedWhilePreparingStateIsSet() {
        ConnectionType connectionType = ConnectionType.FOUR_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Preparing, ErrorReason.Nothing, POSITION, DURATION);

        flipperAdapter.onBufferingChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.BUFFERING, PlayStateReason.NONE, POSITION, DURATION, connectionType);
    }

    @Test
    public void bufferingStateIsReportedWhilePreparedStateIsSet() {
        ConnectionType connectionType = ConnectionType.FOUR_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Prepared, ErrorReason.Nothing, POSITION, DURATION);

        flipperAdapter.onBufferingChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.BUFFERING, PlayStateReason.NONE, POSITION, DURATION, connectionType);
    }

    @Test
    public void bufferingStateIsReportedWhilePlayingStateIsSetAndBufferingFlag() {
        ConnectionType connectionType = ConnectionType.FOUR_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Playing, ErrorReason.Nothing, true, POSITION, DURATION);

        flipperAdapter.onBufferingChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.BUFFERING, PlayStateReason.NONE, POSITION, DURATION, connectionType);
    }

    @Test
    public void transitionToIdleReportsTheCorrectTranslatedProperties() {
        ConnectionType connectionType = ConnectionType.FOUR_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Idle, ErrorReason.Nothing, POSITION, DURATION);

        flipperAdapter.onStateChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.IDLE, PlayStateReason.NONE, POSITION, DURATION, connectionType);
    }

    @Test
    public void transitionToPlayingReportsTheCorrectTranslatedProperties() {
        ConnectionType connectionType = ConnectionType.WIFI;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Playing, ErrorReason.Nothing, POSITION, DURATION);

        flipperAdapter.onStateChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.PLAYING, PlayStateReason.NONE, POSITION, DURATION, connectionType);
    }

    @Test
    public void transitionToPausedStateReportsItAsIdle() {
        ConnectionType connectionType = ConnectionType.WIFI;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Paused, ErrorReason.Nothing, POSITION, DURATION);

        flipperAdapter.onStateChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.IDLE, PlayStateReason.NONE, POSITION, DURATION, connectionType);
    }

    @Test
    public void transitionToCompletedStateReportsThePlayStateReason() {
        ConnectionType connectionType = ConnectionType.FOUR_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Completed, ErrorReason.Nothing, POSITION, DURATION);

        flipperAdapter.onStateChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.IDLE, PlayStateReason.PLAYBACK_COMPLETE, POSITION, DURATION, connectionType);
    }

    @Test
    public void errorTransitionAsForbiddenIsReported() {
        ConnectionType connectionType = ConnectionType.THREE_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Error, ErrorReason.Forbidden, POSITION, DURATION);

        flipperAdapter.onStateChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.IDLE, PlayStateReason.ERROR_FORBIDDEN, POSITION, DURATION, connectionType);
    }

    @Test
    public void errorTransitionAsNotFoundIsReported() {
        ConnectionType connectionType = ConnectionType.THREE_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Error, ErrorReason.NotFound, POSITION, DURATION);

        flipperAdapter.onStateChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.IDLE, PlayStateReason.ERROR_NOT_FOUND, POSITION, DURATION, connectionType);
    }

    @Test
    public void errorTransitionAsFailedIsReported() {
        ConnectionType connectionType = ConnectionType.THREE_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);

        AudioPlaybackItem playbackItem = TestPlaybackItem.audio();
        whenPlaying(playbackItem);
        StateChange stateChange = stateChange(playbackItem.getUrn(), PlayerState.Error, ErrorReason.Failed, POSITION, DURATION);

        flipperAdapter.onStateChanged(stateChange);

        verifyReportedState(playbackItem, PlaybackState.IDLE, PlayStateReason.ERROR_FAILED, POSITION, DURATION, connectionType);
    }

    @Test
    public void onErrorPublishesPlaybackErrorEvent() {
        ConnectionType connectionType = ConnectionType.FOUR_G;
        when(connectionHelper.getCurrentConnectionType()).thenReturn(connectionType);
        final String category = "CODEC_DECODER";
        FlipperError errorMessage = new FlipperError(category, "sourceFile", 1, "message",
                                                     StreamingProtocol.Hls, CDN_HOST, OPUS, BITRATE);

        flipperAdapter.onError(errorMessage);

        assertOnLastPlaybackErrorEvent(category, connectionType, PlaybackProtocol.HLS, OPUS, BITRATE, CDN_HOST);
    }

    private void assertOnLastPlaybackErrorEvent(String category, ConnectionType connectionType,
                                                PlaybackProtocol protocol, String format,
                                                int bitrate, String cdnHost) {
        final PlaybackErrorEvent event = eventBus.lastEventOn(EventQueue.PLAYBACK_ERROR);

        assertThat(event.getBitrate()).isEqualTo(bitrate);
        assertThat(event.getFormat()).isEqualTo(format);
        assertThat(event.getProtocol()).isEqualTo(protocol);
        assertThat(event.getCategory()).isEqualTo(category);
        assertThat(event.getCdnHost()).isEqualTo(cdnHost);
        assertThat(event.getConnectionType()).isEqualTo(connectionType);
        assertThat(event.getPlayerType()).isEqualTo(PlayerType.FLIPPER.getValue());
    }

    private void verifyReportedState(PlaybackItem playbackItem, PlaybackState playbackState, PlayStateReason playStateReason,
                                     long position, long duration, ConnectionType connectionType) {
        verify(stateChangeHandler).report(eq(playbackItem), transitionCaptor.capture());

        PlaybackStateTransition stateTransition = transitionCaptor.getValue();
        assertThat(stateTransition.getNewState()).isEqualTo(playbackState);
        assertThat(stateTransition.getReason()).isEqualTo(playStateReason);
        assertThat(stateTransition.getUrn()).isEqualTo(playbackItem.getUrn());
        assertThat(stateTransition.getProgress().getPosition()).isEqualTo(position);
        assertThat(stateTransition.getProgress().getDuration()).isEqualTo(duration);
        assertThat(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE)).isEqualTo(PlayerType.FLIPPER.getValue());
        assertThat(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE)).isEqualTo(connectionType.getValue());
        assertThat(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_URI)).isEqualTo(fakeMediaUri(playbackItem.getUrn()));
        assertThat(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE)).isEqualTo("true");
    }

    private void whenPlaying(AudioPlaybackItem audio) {
        when(hlsStreamUrlBuilder.buildStreamUrl(audio)).thenReturn(fakeMediaUri(audio.getUrn()));
        flipperAdapter.play(audio);
    }

    @NonNull
    private String fakeMediaUri(Urn urn) {
        return "fakeUrl " + urn.getContent();
    }

    private StateChange stateChange(Urn urn, PlayerState state, ErrorReason errorReason, long position, long duration) {
        return new StateChange(fakeMediaUri(urn), state, errorReason, false, position, duration, StreamingProtocol.Hls);
    }

    private StateChange stateChange(Urn urn, PlayerState state, ErrorReason errorReason, boolean buffering, long position, long duration) {
        return new StateChange(fakeMediaUri(urn), state, errorReason, buffering, position, duration, StreamingProtocol.Hls);
    }
}
