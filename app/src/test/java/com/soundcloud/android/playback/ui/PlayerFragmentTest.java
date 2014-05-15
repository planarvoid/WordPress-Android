package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

    private PlayerFragment fragment;
    private PlayerPresenter.Listener listener;

    @Before
    public void setUp() throws Exception {
        fragment = new PlayerFragment(eventBus, playQueueManager, playbackOperations, presenterFactory);
        when(presenterFactory.create(same(view), any(PlayerPresenter.Listener.class))).thenReturn(presenter);
        Robolectric.shadowOf(fragment).setActivity(activity);

        when(eventBus.subscribe(same(EventQueue.PLAYBACK_STATE_CHANGED), any(Observer.class))).thenReturn(playStateSubscription);
        when(eventBus.subscribe(same(EventQueue.PLAY_QUEUE), any(Observer.class))).thenReturn(playQueueSubscription);

        fragment.onCreate(null);
        fragment.onViewCreated(view, null);
        ArgumentCaptor<PlayerPresenter.Listener> captor = ArgumentCaptor.forClass(PlayerPresenter.Listener.class);
        verify(presenterFactory).create(same(view), captor.capture());
        listener = captor.getValue();
    }

    @Test
    public void onViewCreateCallsSetQueuePositionWithCurrentPositionFromPlayqueueManager() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        fragment.onViewCreated(view, null);
        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void callingOnTogglePlayOnPresenterListenerCallsTogglePlaybackOnPlaybackOperations() {
        listener.onTogglePlay();
        verify(playbackOperations).togglePlayback(activity);
    }

    @Test
    public void callingOnNextOnPresenterListenerCallsNextTrackOnPlaybackOperations() {
        listener.onNext();
        verify(playbackOperations).nextTrack();
    }

    @Test
    public void callingOnPreviousOnPresenterListenerCallsPreviousTrackOnPlaybackOperations() {
        listener.onPrevious();
        verify(playbackOperations).previousTrack();
    }

    @Test
    public void callingOnTrackChangedOnPresenterListenerCallsSetPlayQueuePositionOnPlaybackOperationsWithArgument() {
        listener.onTrackChanged(3);
        verify(playbackOperations).setPlayQueuePosition(3);
    }

    @Test
    public void OnPlayingStateEventCallsOnPlaystateChangedOnFragmentWithIsPlaying() {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE);
        eventMonitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);
        verify(presenter).onPlayStateChanged(true);
    }

    @Test
    public void OnPlayQueueEventForTrackChangeCallsSetQueuePositionOnPresenterWithCurrentPlayQueueManagerPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange());
        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void OnPlayQueueEventForQueueChangeCallsSetQueuePositionOnPresenterWithCurrentPlayQueueManagerPosition() {
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueChange());
        verify(presenter).setQueuePosition(3);
    }

    @Test
    public void OnPlayQueueEventForQueueChangeCallsOnPlayQueueChangedOnPresenter() {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);
        eventMonitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueChange());
        verify(presenter).onPlayQueueChanged();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroy() {
        fragment.onDestroy();
        verify(playQueueSubscription).unsubscribe();
        verify(playStateSubscription).unsubscribe();
    }

}