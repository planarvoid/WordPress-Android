package com.soundcloud.android.cast;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.soundcloud.android.Actions;
import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.app.Activity;
import android.content.Intent;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CastSessionControllerTest extends AndroidUnitTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(URN, URN2);

    private CastSessionController castSessionController;

    @Mock private CastOperations castOperations;
    @Mock private PlaybackServiceInitiator serviceInitiator;
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
                                                          serviceInitiator,
                                                          castPlayer,
                                                          playQueueManager,
                                                          videoCastManager,
                                                          eventBus,
                                                          playSessionStateProvider,
                                                          expandPlayerSubscriberProvider);
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(Collections.<Urn>emptyList(),
                                                                                  Urn.NOT_SET));
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

        verify(castPlayer, never()).reloadCurrentQueue();
    }

    @Test
    public void onConnectedToReceiverAppStopsPlaybackService() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(URN));
        when(playSessionStateProvider.getLastProgressForItem(URN)).thenReturn(PlaybackProgress.empty());
        when(castPlayer.reloadCurrentQueue()).thenReturn(Observable.<PlaybackResult>empty());

        callOnConnectedToReceiverApp();

        verify(serviceInitiator).stopPlaybackService();
    }

    @Test
    public void onConnectedToReceiverAppExpandsPlayerWhenLocalQueueIsPopulated() throws Exception {
        castSessionController.startListening();
        PlaybackResult playbackResult = PlaybackResult.success();
        when(playSessionStateProvider.getLastProgressForItem(URN)).thenReturn(new PlaybackProgress(123L, 456L));
        when(castPlayer.reloadCurrentQueue()).thenReturn(Observable.just(playbackResult));
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(URN));

        callOnConnectedToReceiverApp();

        assertThat(expandPlayerSubscriber.getOnNextEvents()).hasSize(1);
        assertThat(expandPlayerSubscriber.getOnNextEvents().get(0)).isEqualTo(playbackResult);
    }

    @Test
    public void onMetaDataUpdatedDoesNotSetPlayQueueWithSameTrackList() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);

        callOnMetadatUpdated();

        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), any(PlaySessionSource.class), anyInt());
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWithSameRemoteQueueAndRemoteIsPlaying() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);
        when(videoCastManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);

        callOnMetadatUpdated();

        verify(castPlayer).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedSetsPlayQueueWithDifferentTracklist() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).isEqualTo(1);
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).isTrue();
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWhenRemotePlayQueueIsDifferent() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        verify(castPlayer).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedShowsPlayer() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).isEqualTo(1);
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).isTrue();
    }

    @Test
    public void onMediaRouteDialogCellClickOpensStreamAndExpandsPlayer() {
        castSessionController.startListening();

        final Activity activityContext = new Activity();

        castSessionController.onMediaRouteDialogCellClick(activityContext);

        Assertions.assertThat(activityContext).nextStartedIntent()
                  .containsAction(Actions.STREAM)
                  .containsExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true)
                  .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
                  .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private void callOnMetadatUpdated() {
        castSessionController.onRemoteMediaPlayerMetadataUpdated();
    }

    private void callOnConnectedToReceiverApp() {
        castSessionController.onApplicationConnected(null, null, true);
    }
}
