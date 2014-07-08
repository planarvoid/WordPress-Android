package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagerAdapterTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);
    private static final int ADAPTER_POSITION = 0;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionController playSessionController;
    @Mock private TrackOperations trackOperations;
    @Mock private TrackPagePresenter trackPagePresenter;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private View view;
    @Mock private ViewGroup container;

    private TrackPagerAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new TrackPagerAdapter(playQueueManager, playSessionController, trackOperations, trackPagePresenter);
        when(playQueueManager.getUrnAtPosition(ADAPTER_POSITION)).thenReturn(TRACK_URN);
        when(playQueueManager.getCurrentPosition()).thenReturn(ADAPTER_POSITION);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.just(PropertySet.from(
                TrackProperty.URN.bind(TRACK_URN)
        )));
    }

    @Test
    public void getCountReturnsCurrentPlayQueueSize() {
        when(playQueueManager.getQueueSize()).thenReturn(10);
        expect(adapter.getCount()).toBe(10);
    }

    @Test
    public void getViewReturnsConvertViewWhenNotNull() {
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.<PropertySet>empty());
        expect(adapter.getView(ADAPTER_POSITION, view, container)).toBe(view);
    }

    @Test
    public void getViewReturnsCreatedViewWhenConvertViewIsNull() {
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.<PropertySet>empty());
        when(trackPagePresenter.createTrackPage(container)).thenReturn(view);
        expect(adapter.getView(ADAPTER_POSITION, null, container)).toBe(view);
    }

    @Test
    public void getViewLoadsTrackWithProgressForGivenPlayQueuePosition() {
        final PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);
        when(playSessionController.getCurrentProgress(TRACK_URN)).thenReturn(playbackProgress);

        adapter.getView(ADAPTER_POSITION, view, container);
        verify(trackPagePresenter).populateTrackPage(refEq(view), any(PlayerTrack.class), eq(playbackProgress));
    }

    @Test
    public void setCurrentPlayStateOnTrackViewWhenBindingTrack() {
        final Playa.StateTransition transition = createStateTransition();
        when(playSessionController.getPlayState()).thenReturn(transition);

        adapter.getView(ADAPTER_POSITION, view, container);

        verify(trackPagePresenter, times(1)).setPlayState(any(View.class), eq(transition), anyBoolean());
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        adapter.getView(ADAPTER_POSITION, view, container);
        verify(trackOperations).track(any(TrackUrn.class));
    }

    @Test
    public void setProgressOnCurrentTrackSetsProgressOnPresenter() {
        final PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);

        when(playQueueManager.isCurrentTrack(TRACK_URN)).thenReturn(true);

        adapter.getView(ADAPTER_POSITION, view, container);
        adapter.setProgressOnCurrentTrack(new PlaybackProgressEvent(playbackProgress, TRACK_URN));

        verify(trackPagePresenter).setProgress(view, playbackProgress);
    }

    @Test
    public void setProgressOnCurrentTrackDoesNothingIfNotPlayingPlayQueueTrack() {
        final PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);

        adapter.getView(ADAPTER_POSITION, view, container);
        adapter.setProgressOnCurrentTrack(new PlaybackProgressEvent(playbackProgress, TRACK_URN));

        verify(trackPagePresenter, never()).setProgress(any(View.class), any(PlaybackProgress.class));
    }

    @Test
    public void setProgressOnCurrentTrackWhenSetProgressOnAllViews() {
        PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);
        when(playSessionController.getCurrentProgress(TRACK_URN)).thenReturn(playbackProgress);
        when(playSessionController.isPlayingTrack(playQueueManager.getUrnAtPosition(4))).thenReturn(true);
        adapter.getView(ADAPTER_POSITION, view, container);

        adapter.onCurrentPageChanged();

        verify(trackPagePresenter).reset(view);
        verify(trackPagePresenter).setProgress(view, playbackProgress);
    }

    @Test
    public void setPlayStateSetsTrackPlayingStateForCurrentTrack() {
        when(playQueueManager.isCurrentPosition(ADAPTER_POSITION)).thenReturn(true);

        adapter.getView(ADAPTER_POSITION, view, container);
        adapter.setPlayState(createStateTransition());

        verify(trackPagePresenter).setPlayState(view, createStateTransition(), true);
    }

    @Test
    public void setPlayStateSetsNotPlayingStateForOtherTrack() {
        when(playQueueManager.isCurrentPosition(ADAPTER_POSITION)).thenReturn(false);

        adapter.getView(ADAPTER_POSITION, view, container);
        adapter.setPlayState(createStateTransition());

        verify(trackPagePresenter).setPlayState(view, createStateTransition(), false);
    }

    @Test
    public void setTrackPagePresenterFullScreenMode() {
        adapter.getView(ADAPTER_POSITION, view, container);

        adapter.setExpandedMode(true);

        verify(trackPagePresenter).setExpanded(view, false);
    }

    @Test
    public void clearsOutTrackViewMapWhenDataSetIsChanged() {
        adapter.getView(ADAPTER_POSITION, view, container);
        adapter.notifyDataSetChanged();

        expect(adapter.getTrackViewByPosition(ADAPTER_POSITION)).toBeNull();
    }

    private Playa.StateTransition createStateTransition() {
        return new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE);
    }
}