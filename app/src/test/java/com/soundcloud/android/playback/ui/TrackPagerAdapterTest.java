package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagerAdapterTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionController playSessionController;
    @Mock private TrackOperations trackOperations;
    @Mock private TrackPagePresenter trackPagePresenter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ViewGroup container;

    private TestEventBus eventBus;
    private TrackPagerAdapter adapter;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        adapter = new TrackPagerAdapter(playQueueManager, playSessionController, trackOperations, trackPagePresenter, eventBus);
        final View mockedView1 = mock(View.class);
        final View mockedView2 = mock(View.class);
        when(trackPagePresenter.createTrackPage(container)).thenReturn(mockedView1, mockedView2);
    }

    @Test
    public void getCountReturnsCurrentPlayQueueSize() {
        when(playQueueManager.getQueueSize()).thenReturn(10);
        expect(adapter.getCount()).toBe(10);
    }

    @Test
    public void getViewReturnsConvertViewWhenNotNull() {
        View view = mock(View.class);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.<PropertySet>empty());
        when(playQueueManager.getUrnAtPosition(0)).thenReturn(Urn.forTrack(123L));
        when(trackPagePresenter.clearView(any(View.class), any(TrackUrn.class))).thenReturn(view);
        expect(adapter.getView(0, view, container)).toBe(view);
    }

    @Test
    public void getViewReturnsCreatedViewWhenConvertViewIsNull() {
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.<PropertySet>empty());

        getPageView();

        verify(trackPagePresenter).createTrackPage(container);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateOnPresenter() {
        final View currentTrackView = getPageView();
        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(currentTrackView, state, true);
    }

    @Test
    public void onPlayingStateEventCallsSetPlayStateForOtherPage() {
        setCurrentTrackState(3, Urn.forTrack(123L), true);
        setCurrentTrackState(4, Urn.forTrack(234L), false);
        final View viewForCurrentTrack = getPageView(3, Urn.forTrack(123L));
        final View viewForOtherTrack = getPageView(4, Urn.forTrack(234L));

        Mockito.reset(trackPagePresenter);
        StateTransition state = new StateTransition(PlayaState.PLAYING, Reason.NONE);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        verify(trackPagePresenter).setPlayState(viewForCurrentTrack, state, true);
        verify(trackPagePresenter).setPlayState(viewForOtherTrack, state, false);
    }

    @Test
    public void onPlaybackProgressEventSetsProgressOnCurrentPlayingTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), Urn.forTrack(123L));

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoNotSetProgressOnOtherTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), Urn.forTrack(234L));

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void onPlaybackProgressEventDoesNotSetProgressOnNotPlayingTrackPage() {
        View currentPageView = getPageView();
        PlaybackProgressEvent event = new PlaybackProgressEvent(new PlaybackProgress(5l, 10l), Urn.forTrack(999L));

        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, event);

        verify(trackPagePresenter, never()).setProgress(currentPageView, event.getPlaybackProgress());
    }

    @Test
    public void shouldSetExpandModeOnSubscribe() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());

        verify(trackPagePresenter).setCollapsed(currentTrackView);
    }

    @Test
    public void onPlayerExpandedEventSetsFullScreenPlayerMode() {
        View currentTrackView = getPageView();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        InOrder inOrder = Mockito.inOrder(trackPagePresenter);
        inOrder.verify(trackPagePresenter).setCollapsed(currentTrackView);
        inOrder.verify(trackPagePresenter).setExpanded(eq(currentTrackView), anyBoolean());
    }

    @Test
    public void creatingNewTrackViewSetThePlayState() {
        View currentPageView = getPageView();

        verify(trackPagePresenter).setPlayState(eq(currentPageView), any(StateTransition.class), eq(true));
    }

    @Test
    public void recyclingTrackViewDoesNotSetThePlayState() {
        final View view = getPageView();
        Mockito.reset(trackPagePresenter);
        when(trackPagePresenter.clearView(view, Urn.forTrack(123L))).thenReturn(view);
        View currentPageView = adapter.getView(3, view, container);

        verify(trackPagePresenter, never()).setPlayState(eq(currentPageView), any(StateTransition.class), eq(true));
    }

    @Test
    public void getViewClearsRecycledViewWithUrnForCurrentPosition() {
        final View view = getPageView();
        TrackUrn trackUrn = Urn.forTrack(123L);
        when(playQueueManager.getUrnAtPosition(2)).thenReturn(trackUrn);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.<PropertySet>empty());

        adapter.getView(2, view, container);

        verify(trackPagePresenter).clearView(view, trackUrn);
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        getPageView();
        verify(trackOperations).track(TRACK_URN);
    }

    @Test
    public void resetAllScrubStateWhenTrackChange() {
        View currentPageView = getPageView();

        adapter.onTrackChange();

        verify(trackPagePresenter).clearScrubState(currentPageView);
    }

    @Test
    public void clearsOutTrackViewMapWhenDataSetIsChanged() {
        getPageView();
        adapter.notifyDataSetChanged();

        expect(adapter.isPositionByViewMapEmpty()).toBeTrue();
    }

    private View getPageView() {
        setCurrentTrackState(3, Urn.forTrack(123L), true);
        return getPageView(3, Urn.forTrack(123L));
    }

    private View getPageView(int position, TrackUrn trackUrn) {
        setupGetCurrentViewPreconditions(position, trackUrn);
        return adapter.getView(position, null, container);
    }

    private void setupGetCurrentViewPreconditions(int position, TrackUrn trackUrn) {
        when(playQueueManager.getUrnAtPosition(position)).thenReturn(trackUrn);
        when(trackOperations.track(trackUrn)).thenReturn(Observable.just(PropertySet.from(TrackProperty.URN.bind(TRACK_URN))));
    }

    private void setCurrentTrackState(int position, TrackUrn trackUrn, boolean isCurrentTrack) {
        if (isCurrentTrack) {
            when(playQueueManager.getCurrentPosition()).thenReturn(position);
        }
        when(playQueueManager.isCurrentPosition(position)).thenReturn(isCurrentTrack);
        when(playQueueManager.isCurrentTrack(trackUrn)).thenReturn(isCurrentTrack);
        when(playQueueManager.getUrnAtPosition(position)).thenReturn(trackUrn);
    }
}