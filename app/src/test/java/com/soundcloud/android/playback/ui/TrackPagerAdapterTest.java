package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.LegacyTrackOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class TrackPagerAdapterTest {

    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private PlaySessionController playSessionController;
    @Mock
    private LegacyTrackOperations trackOperations;
    @Mock
    private TrackPagePresenter trackPagePresenter;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private View view;
    @Mock
    private ViewGroup container;
    @Mock
    private Track track;

    private TrackPagerAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new TrackPagerAdapter(playQueueManager, playSessionController, trackOperations, trackPagePresenter);
    }

    @Test
    public void getCountReturnsCurrentPlayQueueSize() {
        when(playQueueManager.getQueueSize()).thenReturn(10);
        expect(adapter.getCount()).toBe(10);
    }

    @Test
    public void getViewReturnsConvertViewWhenNotNull() {
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<Track>empty());
        expect(adapter.getView(0, view, container)).toBe(view);
    }

    @Test
    public void getViewReturnsCreatedViewWhenConvertViewIsNull() {
        when(trackOperations.loadTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<Track>empty());
        when(trackPagePresenter.createTrackPage(container)).thenReturn(view);
        expect(adapter.getView(0, null, container)).toBe(view);
    }

    @Test
    public void getViewLoadsTrackWithProgressForGivenPlayQueuePosition() {
        setupGetCurrentViewPreconditions();
        final PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);
        when(playSessionController.getCurrentProgress(track.getUrn())).thenReturn(playbackProgress);

        adapter.getView(3, view, container);
        verify(trackPagePresenter).populateTrackPage(view, track, playbackProgress);
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        setupGetCurrentViewPreconditions();
        adapter.getView(3, view, container);
        verify(trackOperations).loadTrack(anyLong(), any(Scheduler.class));
    }

    @Test
    public void setProgressOnCurrentTrackSetsProgressOnPresenter() {
        setupGetCurrentViewPreconditions();
        final PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);

        when(playQueueManager.isCurrentTrack(track.getUrn())).thenReturn(true);

        adapter.getView(3, view, container);
        adapter.setProgressOnCurrentTrack(new PlaybackProgressEvent(playbackProgress, track.getUrn()));

        verify(trackPagePresenter).setProgress(view, playbackProgress);
    }

    @Test
    public void setProgressOnCurrentTrackDoesNothingIfNotPlayingPlayQueueTrack() {
        setupGetCurrentViewPreconditions();
        final PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);

        adapter.getView(3, view, container);
        adapter.setProgressOnCurrentTrack(new PlaybackProgressEvent(playbackProgress, track.getUrn()));

        verify(trackPagePresenter, never()).setProgress(any(View.class), any(PlaybackProgress.class));
    }

    @Test
    public void setProgressOnCurrentTrackWhenSetProgressOnAllViews() {
        setupGetCurrentViewPreconditions();
        PlaybackProgress playbackProgress = new PlaybackProgress(5l, 10l);
        when(playSessionController.getCurrentProgress(track.getUrn())).thenReturn(playbackProgress);
        when(playSessionController.isPlayingTrack(playQueueManager.getUrnAtPosition(4))).thenReturn(true);
        adapter.getView(3, view, container);

        adapter.setProgressOnAllViews();

        verify(trackPagePresenter).setProgress(view, playbackProgress);
    }
    @Test
    public void setPlayStateSetsTrackPlayingStateForCurrentTrack() {
        setupGetCurrentViewPreconditions();
        when(playQueueManager.isCurrentPosition(3)).thenReturn(true);

        adapter.getView(3, view, container);
        adapter.setPlayState(new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));

        verify(trackPagePresenter).setPlayState(view, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), true);
    }

    @Test
    public void setPlayStateSetsNotPlayingStateForOtherTrack() {
        setupGetCurrentViewPreconditions();
        when(playQueueManager.isCurrentPosition(3)).thenReturn(false);

        adapter.getView(3, view, container);
        adapter.setPlayState(new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));

        verify(trackPagePresenter).setPlayState(view, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE), false);
    }

    @Test
    public void setTrackPagePresenterFullScreenMode() {
        setupGetCurrentViewPreconditions();
        adapter.getView(3, view, container);

        adapter.setExpandedMode(true);

        verify(trackPagePresenter).setExpanded(view, false);
    }

    @Test
    public void clearsOutTrackViewMapWhenDataSetIsChanged() {
        setupGetCurrentViewPreconditions();

        adapter.getView(3, view, container);
        adapter.notifyDataSetChanged();

        expect(adapter.getTrackViewByPosition(3)).toBeNull();
    }

    private void setupGetCurrentViewPreconditions() {
        when(playQueueManager.getIdAtPosition(3)).thenReturn(123L);
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        when(trackOperations.loadTrack(123L, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));
    }

}