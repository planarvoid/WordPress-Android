package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;

import android.support.v4.app.FragmentActivity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerFragmentTest {

    @Mock
    private EventBus eventBus;
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
    @Mock
    private Subscription playQueueSubscription;
    @Mock
    private Subscription playStateSubscription;
    @Mock
    private Subscription playProgressSubscription;
    @Mock
    private Subscription playerUiSubscription;

    @InjectMocks
    private PlayerFragment fragment;

    @Before
    public void setUp() throws Exception {
        when(presenterFactory.create(same(view))).thenReturn(presenter);
        Robolectric.shadowOf(fragment).setActivity(activity);

        when(eventBus.subscribe(same(EventQueue.PLAYBACK_STATE_CHANGED), any(Observer.class))).thenReturn(playStateSubscription);
        when(eventBus.subscribeImmediate(same(EventQueue.PLAY_QUEUE), any(Observer.class))).thenReturn(playQueueSubscription);
        when(eventBus.subscribe(same(EventQueue.PLAYBACK_PROGRESS), any(Observer.class))).thenReturn(playProgressSubscription);
        when(eventBus.subscribe(same(EventQueue.PLAYER_UI), any(Observer.class))).thenReturn(playerUiSubscription);
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
    public void onViewCreateDoesNotInteractWithPresenterIfPlayQueueIsEmpty() throws Exception {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        fragment.onViewCreated(view, null);
        verifyNoMoreInteractions(presenter);
    }

    @Test
    public void onPlayingStateEventCallsOnPlaystateChangedOnFragmentWithIsPlaying() {
        createFragment();
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE);
        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);
        verify(presenter).onPlayStateChanged(true);
    }

    @Test
    public void onPlayQueueEventForTrackChangeCallsSetQueuePositionOnPresenterWithCurrentPlayQueueManagerPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.monitorQueue(EventQueue.PLAY_QUEUE);
        createFragment();

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(Urn.forTrack(123)));

        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void onPlayQueueEventForNewQueueCallsSetQueuePositionOnPresenterWithCurrentPlayQueueManagerPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.monitorQueue(EventQueue.PLAY_QUEUE);
        createFragment();

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.forTrack(123)));

        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void onPlayQueueEventForNewQueueCallsOnPlayQueueChangedOnPresenter() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.monitorQueue(EventQueue.PLAY_QUEUE);
        createFragment();

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.forTrack(123)));

        verify(presenter).onPlayQueueChanged();
    }

    @Test
    public void onPlayQueueEventForQueueUpdateCallsOnPlayQueueChangedOnPresenter() {
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.monitorQueue(EventQueue.PLAY_QUEUE);
        createFragment();

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(Urn.forTrack(123)));

        verify(presenter).onPlayQueueChanged();
    }

    @Test
    public void onPlaybackProgressEventSetsPlayerProgressOnPresenter() {
        createFragment();
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        PlaybackProgressEvent progressEvent = new PlaybackProgressEvent(5l, 10l);

        eventMonitor.publish(EventQueue.PLAYBACK_PROGRESS, progressEvent);
        verify(presenter).onPlayerProgress(progressEvent);
    }

    @Test
    public void onPlayerExpandedEventSetsFullScreenPlayerOnPresenter() {
        createFragment();
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        verify(presenter).setFullScreenPlayer(true);
    }

    @Test
    public void onPlayerCollapsedEventSetsFooterPlayerOnPresenter() {
        createFragment();
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        verify(presenter).setFullScreenPlayer(false);
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesOnDestroyView() {
        createFragment();
        fragment.onDestroyView();
        verify(playQueueSubscription).unsubscribe();
        verify(playStateSubscription).unsubscribe();
        verify(playProgressSubscription).unsubscribe();
        verify(playerUiSubscription).unsubscribe();
    }

    private void createFragment() {
        fragment.onCreate(null);
        fragment.onViewCreated(view, null);
    }

}