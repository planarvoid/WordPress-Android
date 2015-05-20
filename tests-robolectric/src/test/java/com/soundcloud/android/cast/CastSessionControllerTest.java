package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.soundcloud.android.Actions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.Intent;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CastSessionControllerTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(URN, URN2);

    private CastSessionController castSessionController;

    @Mock private CastOperations castOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private CastPlayer castPlayer;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private VideoCastManager videoCastManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    private TestSubscriber expandPlayerSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(expandPlayerSubscriber);

    private final TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        castSessionController = new CastSessionController(castOperations,
                playbackOperations,
                castPlayer,
                playQueueManager,
                videoCastManager,
                eventBus,
                playSessionStateProvider,
                expandPlayerSubscriberProvider);
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(Collections.<Urn>emptyList(), Urn.NOT_SET));
    }

    @Test
    public void isNotListeningByDefault() throws Exception {
        verify(videoCastManager, never()).addVideoCastConsumer(any(VideoCastConsumer.class));
    }

    @Test
    public void onConnectedToReceiverAppDoesNotReloadAndPlayCurrentQueueIfQueueIsEmpty() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        callOnConnectedToReceiverApp();

        verify(castPlayer, never()).reloadAndPlayCurrentQueue(anyInt());
    }

    @Test
    public void onConnectedToReceiverAppStopsPlaybackService() throws Exception {
        castSessionController.startListening();
        when(playSessionStateProvider.getLastProgressByUrn(any(Urn.class))).thenReturn(PlaybackProgress.empty());
        when(castPlayer.reloadAndPlayCurrentQueue(anyLong())).thenReturn(Observable.<PlaybackResult>empty());

        callOnConnectedToReceiverApp();

        verify(playbackOperations).stopService();
    }

    @Test
    public void onConnectedToReceiverAppExpandsPlayerWhenLocalQueueIsPopulated() throws Exception {
        castSessionController.startListening();
        PlaybackResult playbackResult = PlaybackResult.success();
        PlaybackProgress lastPlaybackProgress = new PlaybackProgress(123L, 456L);
        when(playSessionStateProvider.getLastProgressByUrn(URN)).thenReturn(lastPlaybackProgress);
        when(castPlayer.reloadAndPlayCurrentQueue(lastPlaybackProgress.getPosition())).thenReturn(Observable.just(playbackResult));
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);

        callOnConnectedToReceiverApp();

        expect(expandPlayerSubscriber.getOnNextEvents()).toNumber(1);
        expect(expandPlayerSubscriber.getOnNextEvents().get(0)).toEqual(playbackResult);
    }

    @Test
    public void onMetaDataUpdatedDoesNotSetPlayQueueWithSameTrackList() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);

        callOnMetadatUpdated();

        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), anyInt(), any(PlaySessionSource.class));
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWithSameRemoteQueueAndRemoteIsPlaying() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);
        when(videoCastManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);

        callOnMetadatUpdated();

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedSetsPlayQueueWithDifferentTracklist() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).toEqual(1);
        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).toBeTrue();
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWhenRemotePlayQueueIsDifferent() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedShowsPlayer() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).toEqual(1);
        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).toBeTrue();
    }

    @Test
    public void onMediaRouteDialogCellClickOpensStreamAndExpandsPlayer() {
        castSessionController.startListening();

        castSessionController.onMediaRouteDialogCellClick(Robolectric.application);

        final Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.STREAM);
        expect(intent.getBooleanExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, false)).toBeTrue();
        expect(intent.getFlags()).toEqual(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private void callOnMetadatUpdated() {
        castSessionController.onRemoteMediaPlayerMetadataUpdated();
    }

    private void callOnConnectedToReceiverApp() {
        castSessionController.onApplicationConnected(null, null, true);
    }
}