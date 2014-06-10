package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestEventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.support.v4.app.FragmentActivity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerFragmentTest {

    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private PlayerPresenter.Factory presenterFactory;
    @Mock
    private PlayerPresenter presenter;
    @Mock
    private View view;
    @Mock
    private FragmentActivity activity;

    @InjectMocks
    private PlayerFragment fragment;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        fragment.eventBus = eventBus;
        when(presenterFactory.create(same(view))).thenReturn(presenter);
        Robolectric.shadowOf(fragment).setActivity(activity);
    }

    @Test
    public void onViewCreateCallsSetQueuePositionWithCurrentPositionFromPlayqueueManagerIfPlayQueueIsNotEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        fragment.onViewCreated(view, null);
        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void onViewCreateCallsPresenterOnPlayQueueChangedIfPlayQueueIsNotEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        fragment.onViewCreated(view, null);
        verify(presenter).onPlayQueueChanged();
    }

    @Test
    public void onViewCreateDoesNotInformPlayQueueChangedIfPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        fragment.onViewCreated(view, null);
        verify(presenter, never()).onPlayQueueChanged();
    }

    @Test
    public void onViewCreateDoesNotSetPlayQueuePositionIfPlayQueueIsEmpty() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        fragment.onViewCreated(view, null);
        verify(presenter, never()).setQueuePosition(anyInt());
    }

    @Test
    public void onPlayingStateEventCallsOnPlaystateChangedOnFragmentWithIsPlaying() {
        createFragment();
        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);
        verify(presenter).onPlayStateChanged(true);
    }

    @Test
    public void onPlayQueueEventForTrackChangeCallsSetQueuePositionOnPresenterWithCurrentPlayQueueManagerPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        createFragment();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(Urn.forTrack(123)));

        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void onPlayQueueEventForNewQueueCallsSetQueuePositionOnPresenterWithCurrentPlayQueueManagerPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        createFragment();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.forTrack(123)));

        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void onPlayQueueEventForNewQueueCallsOnPlayQueueChangedOnPresenter() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        createFragment();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.forTrack(123)));

        verify(presenter).onPlayQueueChanged();
    }

    @Test
    public void onPlayQueueEventForQueueUpdateCallsOnPlayQueueChangedOnPresenter() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        createFragment();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.forTrack(123)));

        verify(presenter).onPlayQueueChanged();
    }

    @Test
    public void onPlaybackProgressEventSetsPlayerProgressOnPresenter() {
        createFragment();
        PlaybackProgressEvent progressEvent = new PlaybackProgressEvent(5l, 10l);

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, progressEvent);
        verify(presenter).onPlayerProgress(progressEvent);
    }

    @Test
    public void onFragmentCreationSetsFooterPlayerOnPresenter() {
        createFragment();
        verify(presenter).setFullScreenPlayer(false);
    }

    @Test
    public void onPlayerExpandedEventSetsFullScreenPlayerOnPresenter() {
        createFragment();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        InOrder inOrder = Mockito.inOrder(presenter);
        inOrder.verify(presenter).setFullScreenPlayer(false);
        inOrder.verify(presenter).setFullScreenPlayer(true);
    }

    @Test
    public void onPlayerCollapsedEventSetsFooterPlayerOnPresenter() {
        createFragment();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        verify(presenter, times(2)).setFullScreenPlayer(false);
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesOnDestroyView() {
        createFragment();
        fragment.onDestroyView();
        eventBus.verifyUnsubscribed();
    }

    private void createFragment() {
        fragment.onCreate(null);
        fragment.onViewCreated(view, null);
    }

}