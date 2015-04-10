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
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.res.Resources;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CastPlayerTest {

    public static final String HTTP_IMAGE_URL = "http://image.url";
    private static final Urn URN = Urn.forTrack(123L);

    private CastPlayer castPlayer;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private VideoCastManager castManager;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private ImageOperations imageOperations;
    @Mock private Resources resources;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private ProgressReporter progressReporter;
    @Mock private PendingResult<RemoteMediaPlayer.MediaChannelResult> pendingResultCallback;
    @Mock private TrackRepository trackRepository;
    @Mock private PlayQueueManager playQueueManager;

    @Captor private ArgumentCaptor<Playa.StateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPusher> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = new CastPlayer(castManager, progressReporter, imageOperations, resources, eventBus, trackRepository, playQueueManager);
    }

    @Test
    public void pushProgressSendsProgressReportToListener() throws Exception {
        when(castManager.getCurrentMediaPosition()).thenReturn(123L);
        when(castManager.getMediaDuration()).thenReturn(456L);

        verify(progressReporter).setProgressPusher(progressPusherArgumentCaptor.capture());
        progressPusherArgumentCaptor.getValue().pushProgress();

        verifyProgress(123L, 456L);
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

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void onStatusUpdatedWithIdleUnknownStateDoesNotReportTranslatedState() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_INTERRUPTED);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void playWithUrnAlreadyLoadedDoesNotLoadMedia() throws Exception {
        final PropertySet track = setupSuccesfulTrackInfoLoad();

        final MediaInfo media = getMediaInfoForTrack(track);
        when(castManager.getRemoteMediaInformation()).thenReturn(media);

        castPlayer.playCurrent();

        verify(castManager, never()).loadMedia(any(MediaInfo.class), anyBoolean(), anyInt());
    }

    @Test
    public void playWithUrnAlreadyLoadedOutputsExistingState() throws Exception {
        final PropertySet track = setupSuccesfulTrackInfoLoad();
        final MediaInfo media = getMediaInfoForTrack(track);
        when(castManager.getRemoteMediaInformation()).thenReturn(media);
        when(castManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(castManager.getIdleReason()).thenReturn(MediaStatus.IDLE_REASON_NONE);

        castPlayer.playCurrent();

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
        final PropertySet track = setupSuccesfulTrackInfoLoad();
        when(imageOperations.getUrlForLargestImage(resources, track.get(TrackProperty.URN))).thenReturn(HTTP_IMAGE_URL);

        castPlayer.playCurrent();

        ArgumentCaptor<MediaInfo> mediaInfoArgumentCaptor = ArgumentCaptor.forClass(MediaInfo.class);
        verify(castManager).loadMedia(mediaInfoArgumentCaptor.capture(), anyBoolean(), anyInt(), any(JSONObject.class));
        final MediaInfo value = mediaInfoArgumentCaptor.getValue();
        expect(value.getContentType()).toEqual("audio/mpeg");
        expect(value.getMetadata().getImages().get(0).getUrl().toString()).toEqual(HTTP_IMAGE_URL);
        expect(value.getContentId()).toEqual(track.get(TrackProperty.URN).toString());
        expect(new Urn(value.getMetadata().getString(CastPlayer.KEY_URN))).toEqual(track.get(TrackProperty.URN));
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerWithAutoPlayTrue() throws Exception {
        setupSuccesfulTrackInfoLoad();

        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.playCurrent();

        verify(castManager).loadMedia(any(MediaInfo.class), eq(true), anyInt(), any(JSONObject.class));
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerWithDefaultPosition() throws Exception {
        setupSuccesfulTrackInfoLoad();

        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.playCurrent();

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(0), any(JSONObject.class));
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerWithRequestedPosition() throws Exception {
        setupSuccesfulTrackInfoLoad();
        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.playCurrent(100);

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(100), any(JSONObject.class));
    }

    @Test
    public void playCallsLoadOnRemoteMediaPlayerWithQueueAsCustomData() throws Exception {
        final List<Urn> playQueue = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));
        when(playQueueManager.getCurrentQueueAsUrnList()).thenReturn(playQueue);
        setupSuccesfulTrackInfoLoad();
        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.playCurrent(100);

        ArgumentCaptor<JSONObject> customDataCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(100), customDataCaptor.capture());
        JSONObject customData = customDataCaptor.getValue();

        final JSONArray playQueueArr = (JSONArray) customData.get("play_queue");
        expect(playQueueArr.get(0)).toEqual(Urn.forTrack(123L).toString());
        expect(playQueueArr.get(1)).toEqual(Urn.forTrack(456L).toString());
    }

    @Test
    public void playReportsBufferingEvent() throws Exception {
        final PropertySet track = setupSuccesfulTrackInfoLoad();
        when(imageOperations.getUrlForLargestImage(same(resources), any(Urn.class))).thenReturn(HTTP_IMAGE_URL);
        castPlayer.playCurrent(100);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.BUFFERING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
        expect(stateTransition.getTrackUrn()).toEqual(track.get(TrackProperty.URN));
    }

    private PropertySet setupSuccesfulTrackInfoLoad() {
        final PropertySet track = TestPropertySets.expectedTrackForPlayer();
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);
        when(trackRepository.track(URN)).thenReturn(Observable.just(track));
        return track;
    }

    @Test
    public void playCallsReportsErrorStateToEventBusOnUnsuccessfulLoad() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);
        when(trackRepository.track(URN)).thenReturn(Observable.<PropertySet>error(new Throwable("loading error")));

        castPlayer.playCurrent();

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.ERROR_FAILED);
        expect(stateTransition.getTrackUrn()).toBe(URN);
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
    public void onDisconnectedBroadcastsIdleState() throws Exception {
        castPlayer.onDisconnected();

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    private Playa.StateTransition captureFirstStateTransition() {
        return eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    private void verifyProgress(long position, long duration) {
        PlaybackProgress playbackProgress = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS).getPlaybackProgress();
        expect(playbackProgress.getPosition()).toEqual(position);
        expect(playbackProgress.getDuration()).toEqual(duration);
    }
}