package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
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
    private EventBus eventBus;
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
        when(trackPagePresenter.createTrackPage(container, false)).thenReturn(view);
        expect(adapter.getView(0, null, container)).toBe(view);
    }

    @Test
    public void getViewLoadsTrackForGivenPlayQueuePosition() {
        when(playQueueManager.getIdAtPosition(3)).thenReturn(123L);
        when(trackOperations.loadTrack(123L, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));
        adapter.getView(3, view, container);
        verify(trackPagePresenter).populateTrackPage(view, track);
    }

    @Test
    public void getViewLoadsTrackWithProgressForGivenPlayQueuePositionIfPositionIsPlaying() {
        setupGetCurrentViewPreconditions();
        PlaybackProgressEvent progressEvent = new PlaybackProgressEvent(5l, 10l);
        when(playSessionController.getCurrentProgress()).thenReturn(progressEvent);
        when(playSessionController.isPlayingTrack(track)).thenReturn(true);

        adapter.getView(3, view, container);
        verify(trackPagePresenter).populateTrackPage(view, track, progressEvent);
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
        PlaybackProgressEvent progressEvent = new PlaybackProgressEvent(5l, 10l);

        adapter.getView(3, view, container);
        adapter.setProgressOnCurrentTrack(progressEvent);

        verify(trackPagePresenter).setProgress(view, progressEvent);
    }

    @Test
    public void setProgressOnCurrentTrackWhenSetProgressOnAllViews() {
        setupGetCurrentViewPreconditions();
        PlaybackProgressEvent progressEvent = new PlaybackProgressEvent(5l, 10l);
        when(playSessionController.getCurrentProgress()).thenReturn(progressEvent);
        when(playSessionController.isPlayingTrack(playQueueManager.getUrnAtPosition(4))).thenReturn(true);
        adapter.getView(3, view, container);

        adapter.setProgressOnAllViews();

        verify(trackPagePresenter).setProgress(view, progressEvent);
    }

    @Test
    public void resetProgressOnNotCurrentTracksWhenSetProgressOnAllViews() {
        setupGetCurrentViewPreconditions();
        when(playSessionController.isPlayingTrack(any(TrackUrn.class))).thenReturn(false);
        adapter.getView(3, view, container);

        adapter.setProgressOnAllViews();

        verify(trackPagePresenter).resetProgress(view);
    }

    @Test
    public void setPlayStateSetsPlayingStateForCurrentTrack() {
        setupGetCurrentViewPreconditions();
        when(playQueueManager.isCurrentPosition(3)).thenReturn(true);

        adapter.getView(3, view, container);
        adapter.setPlayState(true);

        verify(trackPagePresenter).setPlayState(view, true);
    }

    @Test
    public void setPlayStateSetsNotPlayingStateForOtherTrack() {
        setupGetCurrentViewPreconditions();
        when(playQueueManager.isCurrentPosition(3)).thenReturn(false);

        adapter.getView(3, view, container);
        adapter.setPlayState(true);

        verify(trackPagePresenter).setPlayState(view, false);
    }

    @Test
    public void setTrackPagePresenterFullScreenMode() {
        setupGetCurrentViewPreconditions();
        adapter.getView(3, view, container);

        adapter.fullScreenMode(true);

        verify(trackPagePresenter).setFullScreen(view, true);
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