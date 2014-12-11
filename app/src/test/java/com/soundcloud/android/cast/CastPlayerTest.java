package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.res.Resources;

@RunWith(SoundCloudTestRunner.class)
public class CastPlayerTest {

    private static final String CLIENT_ID = "123asdf";
    public static final String HTTP_IMAGE_URL = "http://image.url";

    private CastPlayer castPlayer;

    @Mock private VideoCastManager castManager;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private HttpProperties httpProperties;
    @Mock private ImageOperations imageOperations;
    @Mock private Resources resources;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private ProgressReporter progressReporter;
    @Mock private PendingResult<RemoteMediaPlayer.MediaChannelResult> pendingResultCallback;
    @Mock private Playa.PlayaListener playerListener;

    @Captor private ArgumentCaptor<Playa.StateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPusher> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(httpProperties.getClientId()).thenReturn(CLIENT_ID);

        castPlayer = new CastPlayer(castManager, httpProperties, progressReporter, imageOperations, resources);
        castPlayer.setListener(playerListener);
    }

    @Test
    public void pushProgressSendsProgressReportToListener() throws Exception {
        when(castManager.getCurrentMediaPosition()).thenReturn(123L);
        when(castManager.getMediaDuration()).thenReturn(456L);

        verify(progressReporter).setProgressPusher(progressPusherArgumentCaptor.capture());
        progressPusherArgumentCaptor.getValue().pushProgress();

        verify(playerListener).onProgressEvent(123, 456);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateReturnsPlayingNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.PLAYING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateStartsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).start();
    }

    @Test
    public void onStatusUpdatedWithPausedStateReturnsIdleNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPausedStateWhileShouldBePlayingDoesNotEmitState() throws Exception {
        castPlayer.resume();

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        verify(playerListener, never()).onPlaystateChanged(any(Playa.StateTransition.class));
    }

    @Test
    public void onStatusUpdatedWithPausedStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithBufferingStateReturnsBufferingNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.BUFFERING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithBufferingStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateReturnsIdleFailed() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.ERROR_FAILED);
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateReturnsTrackComplete() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.TRACK_COMPLETE);
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateReturnsIdleNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleInterruptedStateDoesNotReportTranslatedState() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_INTERRUPTED);

        verify(playerListener, never()).onPlaystateChanged(any(Playa.StateTransition.class));
    }

    @Test
    public void onStatusUpdatedWithIdleUnknownStateDoesNotReportTranslatedState() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_INTERRUPTED);

        verify(playerListener, never()).onPlaystateChanged(any(Playa.StateTransition.class));
    }

    @Test
    public void playWithUrnAlreadyLoadedDoesNotLoadMedia() throws Exception {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final MediaInfo media = getMediaInfoForTrack(track);
        when(castManager.getRemoteMediaInformation()).thenReturn(media);

        castPlayer.play(track);

        verify(castManager, never()).loadMedia(any(MediaInfo.class), anyBoolean(), anyInt());
    }

    @Test
    public void playWithUrnAlreadyLoadedOutputsExistingState() throws Exception {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        final MediaInfo media = getMediaInfoForTrack(track);
        when(castManager.getRemoteMediaInformation()).thenReturn(media);
        when(castManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(castManager.getIdleReason()).thenReturn(MediaStatus.IDLE_REASON_NONE);

        castPlayer.play(track);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.PLAYING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
        expect(stateTransition.getTrackUrn()).toEqual(track.get(TrackProperty.URN));

    }

    private MediaInfo getMediaInfoForTrack(PropertySet track) {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(CastPlayer.KEY_URN, String.valueOf(track.get(TrackProperty.URN)));

        return new MediaInfo.Builder("some-url")
                .setContentType("audio/mpeg")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerStreamContent() throws Exception {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        when(imageOperations.getUrlForLargestImage(resources, track.get(TrackProperty.URN))).thenReturn(HTTP_IMAGE_URL);

        castPlayer.play(track);

        ArgumentCaptor<MediaInfo> mediaInfoArgumentCaptor = ArgumentCaptor.forClass(MediaInfo.class);
        verify(castManager).loadMedia(mediaInfoArgumentCaptor.capture(), anyBoolean(), anyInt());
        final MediaInfo value = mediaInfoArgumentCaptor.getValue();
        expect(value.getContentType()).toEqual("audio/mpeg");
        expect(value.getMetadata().getImages().get(0).getUrl().toString()).toEqual(HTTP_IMAGE_URL);
        expect(value.getContentId()).toEqual(track.get(TrackProperty.STREAM_URL) + "?client_id=" + CLIENT_ID);
        expect(new Urn(value.getMetadata().getString(CastPlayer.KEY_URN))).toEqual(track.get(TrackProperty.URN));
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerWithAutoPlayTrue() throws Exception {
        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.play(TestPropertySets.expectedTrackForPlayer());

        verify(castManager).loadMedia(any(MediaInfo.class), eq(true), anyInt());
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerWithDefaultPosition() throws Exception {
        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.play(TestPropertySets.expectedTrackForPlayer());

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(0));
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerWithRequestedPosition() throws Exception {
        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.play(TestPropertySets.expectedTrackForPlayer(), 100);

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(100));
    }

    @Test
    public void playReportsBufferingEvent() throws Exception {
        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        castPlayer.play(track, 100);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.BUFFERING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
        expect(stateTransition.getTrackUrn()).toEqual(track.get(TrackProperty.URN));
    }

    @Test
    public void resumeCallsResumeOnRemoteMediaPlayerWithApiClient() throws Exception {
        castPlayer.resume();

        verify(castManager).play();
    }

    @Test
    public void pauseCallsPauseOnRemoteMediaPlayerWithApiClient() throws Exception {
        castPlayer.pause();

        verify(castManager).pause();
    }

    @Test
    public void setVolumeSetsVolumeOnRemotePlayer() throws Exception {
        castPlayer.setVolume(123F);

        verify(castManager).setVolume(123F);
    }

    @Test
    public void stopCallsPausepOnRemoteMediaPlayer() throws Exception {
        castPlayer.stop();

        verify(castManager).pause();
    }

    @Test
    public void getProgressReturnsGetApproximateStreamPositionFromRemoteMediaPlayer() throws Exception {
        when(castManager.getCurrentMediaPosition()).thenReturn(123L);

        expect(castPlayer.getProgress()).toEqual(123L);
    }

    @Test
    public void stopsListeningInDestroy() throws Exception {

        castPlayer.destroy();

        verify(castManager).removeVideoCastConsumer(castPlayer);
    }

    @Test
    public void isSeekableIsTrue() throws Exception {
        expect(castPlayer.isSeekable()).toBeTrue();
    }

    @Test
    public void isNotSeekablePastBufferIsFalse() throws Exception {
        expect(castPlayer.isNotSeekablePastBuffer()).toBeFalse();
    }

    @Test
    public void onDisconnectedBroadcastsIdleState() throws Exception {
        castPlayer.onDisconnected();

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    private Playa.StateTransition captureFirstStateTransition() {
        verify(playerListener).onPlaystateChanged(transitionArgumentCaptor.capture());
        return transitionArgumentCaptor.getValue();
    }
}