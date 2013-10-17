package com.soundcloud.android.player;

import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlayerTrackPagerAdapterTest {

    PlayerTrackPagerAdapter adapter;

    @Mock
    PlayerQueueView playerQueueView;

    TrackStorage trackStorage;

    @Before
    public void setUp() throws Exception {
        // TODO remove the override when we move to Robolectric 2
        adapter = new PlayerTrackPagerAdapter(trackStorage) {
            @Override
            protected PlayerQueueView createPlayerQueueView(Context context) {
                return playerQueueView;
            }
        };
    }

//    @Test
//    public void shouldBeEmptyWhenQueueIsEmpty() throws Exception {
//        when(playQueueState.getCurrentTrackIds()).thenReturn(Collections.<Long>emptyList());
//        adapter.setPlayQueue(playQueueState);
//        expect(adapter.getCount()).toEqual(0);
//    }
//
//    @Test
//    public void shouldReturnPlaceholderTrack() throws Exception {
//        final Track placeholderTrack = Mockito.mock(Track.class);
//        adapter.setPlaceholderTrack(placeholderTrack);
//        expect(adapter.getCount()).toEqual(1);
//        expect(adapter.getItem(0).getTrack().toBlockingObservable().lastOrDefault(null)).toBe(placeholderTrack);
//    }
//
//    @Test
//    public void shouldCreateSingleItemQueue() throws Exception {
//        final Track track = TestHelper.getModelFactory().createModel(Track.class);
//        when(playQueueState.getCurrentTrackIds()).thenReturn(Lists.newArrayList(track.getId()));
//        when(trackStorage.getTrack(track.getId())).thenReturn(Observable.just(track));
//
//        adapter.setPlayQueue(playQueueState);
//        expect(adapter.getCount()).toEqual(1);
//        expect(adapter.getItem(0).getTrack().toBlockingObservable().lastOrDefault(null)).toBe(track);
//    }
//
//    @Test
//    public void shouldCreateNewPlayerTrackViewFromPlayQueueItem() throws Exception {
//        final Track track = new Track(123L);
//        final ViewGroup parent = Mockito.mock(ViewGroup.class);
//        final PlayQueueItem playQueueItem = new PlayQueueItem(track);
//        when(parent.getContext()).thenReturn(Robolectric.application);
//
//        expect((PlayerQueueView) adapter.getView(playQueueItem, null, parent)).toBe(playerQueueView);
//        verify(playerQueueView).setPlayQueueItem(playQueueItem, false);
//    }
//
//    @Test
//    public void shouldConvertPlayerQueueViewFromPlayQueueItem() throws Exception {
//        final Track track = new Track(123L);
//        final ViewGroup parent = Mockito.mock(ViewGroup.class);
//        final PlayQueueItem playQueueItem = new PlayQueueItem(track);
//        final PlayerQueueView convertView = Mockito.mock(PlayerQueueView.class);
//
//        expect((PlayerQueueView) adapter.getView(playQueueItem, convertView, parent)).toBe(convertView);
//        verify(convertView).setPlayQueueItem(playQueueItem, false);
//        verifyZeroInteractions(playerQueueView);
//    }

//    @Test
//    public void shouldReturnPlayerTrackViewsByPosition() throws Exception {
//        final PlayerTrackView trackView1= Mockito.mock(PlayerTrackView.class);
//        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);
//
//        add2PlayerTrackViews(trackView1, trackView2);
//
//        expect(adapter.getPlayerTrackViewByPosition(0)).toBe(trackView1);
//        expect(adapter.getPlayerTrackViewByPosition(1)).toBe(trackView2);
//    }
//
//    @Test
//    public void shouldReturnPlayerTrackViewsById() throws Exception {
//        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
//        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);
//
//        when(trackView1.getTrackId()).thenReturn(123L);
//        when(trackView2.getTrackId()).thenReturn(456L);
//
//        add2PlayerTrackViews(trackView1, trackView2);
//
//        expect(adapter.getPlayerTrackViewById(123L)).toBe(trackView1);
//        expect(adapter.getPlayerTrackViewById(456L)).toBe(trackView2);
//    }
//
//    @Test
//    public void allPlayerTrackViewsShouldReceiveOnConnected() throws Exception {
//        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
//        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);
//
//        add2PlayerTrackViews(trackView1, trackView2);
//        adapter.onConnected();
//
//        verify(trackView1).onDataConnected();
//        verify(trackView2).onDataConnected();
//    }
//
//    @Test
//    public void allPlayerTrackViewsShouldReceiveOnStop() throws Exception {
//        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
//        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);
//
//        add2PlayerTrackViews(trackView1, trackView2);
//        adapter.onStop();
//
//        verify(trackView1).onStop(true);
//        verify(trackView2).onStop(true);
//    }
//
//    @Test
//    public void allPlayerTrackViewsShouldReceiveOnDestroy() throws Exception {
//        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
//        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);
//
//        add2PlayerTrackViews(trackView1, trackView2);
//        adapter.onDestroy();
//
//        verify(trackView1).onDestroy();
//        verify(trackView2).onDestroy();
//    }
//
//    @Test
//    public void commentingPositionShouldBeDefault() throws Exception {
//        expect(adapter.getCommentingPosition()).toBe(-1);
//    }
//
//    @Test
//    public void commentingPositionShouldBeDefaultAfterAddition() throws Exception {
//        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
//        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);
//
//        add2PlayerTrackViews(trackView1, trackView2);
//        expect(adapter.getCommentingPosition()).toBe(-1);
//    }
//
//    @Test
//    public void shouldSetCommentingPositionAfterAddition() throws Exception {
//        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
//        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);
//
//        add2PlayerTrackViews(trackView1, trackView2);
//        adapter.setCommentingPosition(1, true);
//        verify(trackView1).setCommentMode(false);
//        verify(trackView2).setCommentMode(true, true);
//
//        expect(adapter.getCommentingPosition()).toBe(1);
//    }

//    @Test
//    public void shouldReplaceEmptyView() throws Exception {
//        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
//
//        final ViewGroup parent = Mockito.mock(ViewGroup.class);
//        final PlayerQueueView playerQueueView1 = Mockito.mock(PlayerQueueView.class);
//
//        when(playerQueueView1.getTrackView()).thenReturn(trackView1);
//        when(playerQueueView1.isShowingPlayerTrackView()).thenReturn(true);
//
//        final PlayerQueueView playerQueueView2 = Mockito.mock(PlayerQueueView.class);
//        when(playerQueueView2.isShowingPlayerTrackView()).thenReturn(false);
//
//        expect((PlayerQueueView) adapter.getView(new PlayQueueItem(Mockito.mock(Track.class), 0), playerQueueView1, parent)).toBe(playerQueueView1);
//        expect((PlayerQueueView) adapter.getView(PlayQueueItem.empty(1), playerQueueView2, parent)).toBe(playerQueueView2);
//
//        when(playQueueManager.length()).thenReturn(2);
//        when(playQueueManager.getPlayQueueItem(1)).thenReturn(new PlayQueueItem(Mockito.mock(Track.class), 0));
//
//        adapter.reloadEmptyView();
//
//        ArgumentCaptor<PlayQueueItem> captor = ArgumentCaptor.forClass(PlayQueueItem.class);
//        verify(playerQueueView2, times(2)).setPlayQueueItem(captor.capture(), anyBoolean());
//
//        expect(captor.getAllValues().get(0).isEmpty()).toBeTrue();
//        expect(captor.getAllValues().get(1).isEmpty()).toBeFalse();
//    }
//
//    @Test
//    public void shouldReturnExtraItemWhenQueueFetching() throws Exception {
//        when(playQueueManager.length()).thenReturn(1);
//        when(playQueueManager.getState().isLoading()).thenReturn(true);
//        expect(adapter.getCount()).toBe(2);
//    }
//
//    @Test
//    public void shouldReturnExtraItemWhenLastQueueFetchFailed() throws Exception {
//        when(playQueueManager.length()).thenReturn(1);
//        //when(playQueueManager.lastLoadFailed()).thenReturn(true);
//        expect(adapter.getCount()).toBe(2);
//    }
//
//    public void shouldReturnUnchangedPositionForEmptyItemWhenFetching() throws Exception {
//        when(playQueueManager.length()).thenReturn(1);
//        //when(playQueueManager.isLoading()).thenReturn(true);
//        expect(adapter.getItemPosition(PlayQueueItem.empty(0))).toBe(PagerAdapter.POSITION_UNCHANGED);
//    }
//
//    @Test
//    public void shouldReturnNoItemPositionForEmptyItemWhenNotFetching() throws Exception {
//        when(playQueueManager.length()).thenReturn(1);
//        //when(playQueueManager.isLoading()).thenReturn(false);
//        expect(adapter.getItemPosition(PlayQueueItem.empty(0))).toBe(PagerAdapter.POSITION_NONE);
//    }

//    private void add2PlayerTrackViews(PlayerTrackView trackView1, PlayerTrackView trackView2) {
//
//        final ViewGroup parent = Mockito.mock(ViewGroup.class);
//        final PlayerQueueView playerQueueView1 = Mockito.mock(PlayerQueueView.class);
//
//        when(playerQueueView1.getTrackView()).thenReturn(trackView1);
//        when(playerQueueView1.isShowingPlayerTrackView()).thenReturn(true);
//
//        final PlayerQueueView playerQueueView2 = Mockito.mock(PlayerQueueView.class);
//        when(playerQueueView2.getTrackView()).thenReturn(trackView2);
//        when(playerQueueView2.isShowingPlayerTrackView()).thenReturn(true);
//
//        final Observable<Track> trackObservable = Observable.just(Mockito.mock(Track.class));
//        expect((PlayerQueueView) adapter.getView(new PlayQueueItem(trackObservable, 0), playerQueueView1, parent)).toBe(playerQueueView1);
//
//        final Observable<Track> trackObservable2 = Observable.just(Mockito.mock(Track.class));
//        expect((PlayerQueueView) adapter.getView(new PlayQueueItem(trackObservable2, 0), playerQueueView2, parent)).toBe(playerQueueView2);
//    }

}
