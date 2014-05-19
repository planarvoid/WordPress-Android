package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.TrackOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    private TrackOperations trackOperations;
    @Mock
    private TrackPagePresenter trackPagePresenter;
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
        when(playQueueManager.getCurrentPlayQueueSize()).thenReturn(10);
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
    public void getViewLoadsTrackForGivenPlayQueuePosition() {
        when(playQueueManager.getIdAtPosition(3)).thenReturn(123L);
        when(trackOperations.loadTrack(123L, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));
        adapter.getView(3, view, container);
        verify(trackPagePresenter).populateTrackPage(view, track);
    }

    @Test
    public void getViewLoadsTrackWithProgressForGivenPlayQueuePositionIfPositionIsPlaying() {
        PlaybackProgressEvent progressEvent = new PlaybackProgressEvent(5l, 10l);
        when(playSessionController.getCurrentProgress()).thenReturn(progressEvent);
        when(playQueueManager.getIdAtPosition(3)).thenReturn(123L);
        when(trackOperations.loadTrack(123L, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));
        when(playSessionController.isPlayingTrack(track)).thenReturn(true);

        adapter.getView(3, view, container);
        verify(trackPagePresenter).populateTrackPage(view, track, progressEvent);
    }

    @Test
    public void getViewUsesCachedObservableIfAlreadyInCache() {
        when(playQueueManager.getIdAtPosition(3)).thenReturn(123L);
        when(trackOperations.loadTrack(123L, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));
        adapter.getView(3, view, container);
        adapter.getView(3, view, container);
        verify(trackOperations).loadTrack(anyLong(), any(Scheduler.class));
    }

    @Test
    public void setProgressOnCurrentTrackSetsProgressOnPresenter() {
        PlaybackProgressEvent progressEvent = new PlaybackProgressEvent(5l, 10l);
        when(playQueueManager.getIdAtPosition(3)).thenReturn(123L);
        when(playQueueManager.getCurrentPosition()).thenReturn(3);
        when(trackOperations.loadTrack(123L, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));

        adapter.getView(3, view, container);
        adapter.setProgressOnCurrentTrack(progressEvent);

        verify(trackPagePresenter).setProgressOnTrackView(view, progressEvent);
    }
}