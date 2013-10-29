package com.soundcloud.android.player;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.PlayQueue;
import com.soundcloud.android.utils.PlaybackOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;


@RunWith(SoundCloudTestRunner.class)
public class PlayerTrackPagerAdapterTest {

    private PlayerTrackPagerAdapter adapter;

    @Mock
    private PlayerQueueView playerQueueView;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private PlayQueue playQueue;


    @Before
    public void setUp() throws Exception {
        // TODO remove the override when we move to Robolectric 2
        adapter = new PlayerTrackPagerAdapter(playbackOperations) {
            @Override
            protected PlayerQueueView createPlayerQueueView(Context context) {
                return playerQueueView;
            }
        };

        when(playQueue.isLoading()).thenReturn(false);
        when(playQueue.lastLoadWasEmpty()).thenReturn(false);
        when(playQueue.lastLoadFailed()).thenReturn(false);
    }

    @Test
    public void shouldBeEmptyWhenQueueIsEmpty() {
        when(playQueue.size()).thenReturn(0);
        adapter.setPlayQueue(playQueue);
        expect(adapter.getCount()).toEqual(0);
    }

    @Test
    public void shouldCreateSingleItemQueue() throws Exception {
        final Track track = TestHelper.getModelFactory().createModel(Track.class);
        adapter.setPlayQueue(new PlayQueue(track.getId()));
        expect(adapter.getCount()).toEqual(1);
        expect(adapter.getItem(0)).toBe(track.getId());
    }

    @Test
    public void shouldCreateNewPlayerTrackViewFromPlayQueueItem() {
        final Observable<Track> trackObservable = Observable.just(new Track());
        when(playbackOperations.loadTrackForPlayback(123L)).thenReturn(trackObservable);

        expect((PlayerQueueView) adapter.getView(123L, null, mock(ViewGroup.class))).toBe(playerQueueView);
        verify(playerQueueView).showTrack(refEq(trackObservable), anyInt(), anyBoolean());
    }

    @Test
    public void shouldConvertPlayerQueueView() {
        final PlayerQueueView convertView = mock(PlayerQueueView.class);

        expect((PlayerQueueView) adapter.getView(123L, convertView, mock(ViewGroup.class))).toBe(convertView);
        verify(convertView).showTrack(any(Observable.class), anyInt(), anyBoolean());
    }

    @Test
    public void shouldReturnPlayerTrackViewsByPosition() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);

        expect(adapter.getPlayerTrackViewByPosition(0)).toBe(trackView1);
        expect(adapter.getPlayerTrackViewByPosition(1)).toBe(trackView2);
    }

    @Test
    public void shouldReturnPlayerTrackViewsById() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        when(trackView1.getTrackId()).thenReturn(1L);
        when(trackView2.getTrackId()).thenReturn(2L);

        addPlayerTrackViews(trackView1, trackView2);

        expect(adapter.getPlayerTrackViewById(1L)).toBe(trackView1);
        expect(adapter.getPlayerTrackViewById(2L)).toBe(trackView2);
    }

    @Test
    public void allPlayerTrackViewsShouldReceiveOnConnected() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.onConnected();

        verify(trackView1).onDataConnected();
        verify(trackView2).onDataConnected();
    }

    @Test
    public void allPlayerTrackViewsShouldReceiveOnStop() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.onStop();

        verify(trackView1).onStop(true);
        verify(trackView2).onStop(true);
    }

    @Test
    public void allPlayerTrackViewsShouldReceiveOnDestroy() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.onDestroy();

        verify(trackView1).onDestroy();
        verify(trackView2).onDestroy();
    }

    @Test
    public void commentingPositionShouldBeDefault() {
        expect(adapter.getCommentingPosition()).toBe(-1);
    }

    @Test
    public void commentingPositionShouldBeDefaultAfterAddition() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        expect(adapter.getCommentingPosition()).toBe(-1);
    }

    @Test
    public void shouldSetCommentingPositionAfterAddition() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.setCommentingPosition(1, true);
        verify(trackView1).setCommentMode(false);
        verify(trackView2).setCommentMode(true, true);

        expect(adapter.getCommentingPosition()).toBe(1);
    }

    @Test
    public void shouldReplaceEmptyView() {
        adapter.setPlayQueue(new PlayQueue(Lists.newArrayList(1L), 0, PlayQueue.AppendState.LOADING));

        final ViewGroup parent = mock(ViewGroup.class);
        final PlayerQueueView playerQueueView1 = mock(PlayerQueueView.class);

        when(playerQueueView1.isShowingPlayerTrackView()).thenReturn(true);

        final PlayerQueueView playerQueueView2 = mock(PlayerQueueView.class);
        when(playerQueueView2.isShowingPlayerTrackView()).thenReturn(false);

        adapter.getView(1L, playerQueueView1, parent);
        adapter.getView(PlayerTrackPagerAdapter.EMPTY_VIEW_ID, playerQueueView2, parent);

        verify(playerQueueView2).showEmptyViewWithState(PlayQueue.AppendState.LOADING);

        adapter.setPlayQueue(new PlayQueue(Lists.newArrayList(1L, 2L), 0, PlayQueue.AppendState.IDLE));
        adapter.reloadEmptyView();
        verify(playerQueueView2).showTrack(any(rx.Observable.class), anyInt(), anyBoolean());

    }

    @Test
    public void shouldReturnExtraItemWhenQueueFetching() {
        when(playQueue.size()).thenReturn(1);
        when(playQueue.isLoading()).thenReturn(true);

        adapter.setPlayQueue(playQueue);
        expect(adapter.getCount()).toBe(2);
    }

    @Test
    public void shouldReturnExtraItemWhenLastQueueFetchFailed() {
        when(playQueue.size()).thenReturn(1);
        when(playQueue.lastLoadFailed()).thenReturn(true);

        adapter.setPlayQueue(playQueue);
        expect(adapter.getCount()).toBe(2);
    }

    @Test
    public void shouldReturnUnchangedPositionForEmptyItemWhenFetching() {
        when(playQueue.size()).thenReturn(1);
        when(playQueue.isLoading()).thenReturn(true);

        adapter.setPlayQueue(playQueue);
        expect(adapter.getItemPosition(PlayerTrackPagerAdapter.EMPTY_VIEW_ID)).toBe(PagerAdapter.POSITION_UNCHANGED);
    }

    @Test
    public void shouldReturnNoItemPositionForEmptyItemWhenNotFetching() {
        adapter.setPlayQueue(playQueue);
        expect(adapter.getItemPosition(PlayerTrackPagerAdapter.EMPTY_VIEW_ID)).toBe(PagerAdapter.POSITION_NONE);
    }

    private void addPlayerTrackViews(PlayerTrackView trackView1, PlayerTrackView trackView2) {
        addPlayerTrackViews(new PlayQueue(Lists.newArrayList(1L, 2L), 0), trackView1, trackView2);
    }

    private void addPlayerTrackViews(PlayQueue playQueue, PlayerTrackView trackView1, PlayerTrackView trackView2) {
        adapter.setPlayQueue(playQueue);

        final ViewGroup parent = mock(ViewGroup.class);
        final PlayerQueueView playerQueueView1 = mock(PlayerQueueView.class);

        when(playerQueueView1.getTrackView()).thenReturn(trackView1);
        when(playerQueueView1.isShowingPlayerTrackView()).thenReturn(true);

        final PlayerQueueView playerQueueView2 = mock(PlayerQueueView.class);
        when(playerQueueView2.getTrackView()).thenReturn(trackView2);
        when(playerQueueView2.isShowingPlayerTrackView()).thenReturn(true);

        adapter.getView(1L, playerQueueView1, parent);
        adapter.getView(2L, playerQueueView2, parent);
    }

}
