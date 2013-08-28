package com.soundcloud.android.adapter.player;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.playback.PlayQueueItem;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.play.PlayerTrackView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class PlayerTrackPagerAdapterTest {

    PlayerTrackPagerAdapter adapter;

    @Mock
    PlayQueueManager playQueueManager;
    @Mock
    PlayerTrackView playerTrackView;
    @Mock
    EmptyListView emptyListView;

    @Before
    public void setUp() throws Exception {
        // TODO remove the override when we move to Robolectric 2
        adapter = new PlayerTrackPagerAdapter(playQueueManager) {
            @Override
            protected PlayerTrackView createPlayerQueueView(Context context) {
                return playerTrackView;
            }

            @Override
            protected EmptyListView createEmptyListView(Context context) {
                return emptyListView;
            }
        };
    }

    @Test
    public void shouldBeEmptyWhenQueueIsEmpty() throws Exception {
        when(playQueueManager.isEmpty()).thenReturn(true);
        expect(adapter.getCount()).toEqual(0);
    }

    @Test
    public void shouldReturnPlaceholderTrack() throws Exception {
        final Track placeholderTrack = Mockito.mock(Track.class);
        adapter.setPlaceholderTrack(placeholderTrack);
        expect(adapter.getCount()).toEqual(1);
        expect(adapter.getItem(0).getTrack()).toBe(placeholderTrack);
    }

    @Test
    public void shouldReturnPlayqueueItem() throws Exception {
        final Track track = Mockito.mock(Track.class);
        when(playQueueManager.length()).thenReturn(1);
        when(playQueueManager.getPlayQueueItem(0)).thenReturn(new PlayQueueItem(track, 0));

        expect(adapter.getCount()).toEqual(1);
        expect(adapter.getItem(0).getTrack()).toBe(track);
    }

    @Test
    public void shouldCreateNewPlayerTrackViewFromPlayQueueItem() throws Exception {
        final Track track = new Track(123L);
        final ViewGroup parent = Mockito.mock(ViewGroup.class);
        final PlayQueueItem playQueueItem = new PlayQueueItem(track, 0);
        when(parent.getContext()).thenReturn(Robolectric.application);

        expect((PlayerTrackView) adapter.getView(playQueueItem, null, parent)).toBe(playerTrackView);
        verify(playerTrackView).setPlayQueueItem(playQueueItem);
        verify(playerTrackView).setOnScreen(true);
    }

    @Test
    public void shouldConvertPlayerTrackViewFromPlayQueueItem() throws Exception {
        final Track track = new Track(123L);
        final ViewGroup parent = Mockito.mock(ViewGroup.class);
        final PlayQueueItem playQueueItem = new PlayQueueItem(track, 0);
        final PlayerTrackView convertView = Mockito.mock(PlayerTrackView.class);

        expect((PlayerTrackView) adapter.getView(playQueueItem, convertView, parent)).toBe(convertView);
        verify(convertView).setPlayQueueItem(playQueueItem);
        verify(convertView).setOnScreen(true);
        verifyZeroInteractions(playerTrackView);
    }

    @Test
    public void shouldReturnPlayerTrackViewsByPosition() throws Exception {
        final PlayerTrackView trackView1= Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        add2PlayerTrackViews(trackView1, trackView2);

        expect(adapter.getPlayerTrackViewByPosition(0)).toBe(trackView1);
        expect(adapter.getPlayerTrackViewByPosition(1)).toBe(trackView2);
    }

    @Test
    public void shouldReturnPlayerTrackViewsById() throws Exception {
        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        when(trackView1.getTrackId()).thenReturn(123L);
        when(trackView2.getTrackId()).thenReturn(456L);

        add2PlayerTrackViews(trackView1, trackView2);

        expect(adapter.getPlayerTrackViewById(123L)).toBe(trackView1);
        expect(adapter.getPlayerTrackViewById(456L)).toBe(trackView2);
    }

    @Test
    public void allPlayerTrackViewsShouldReceiveOnConnected() throws Exception {
        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        add2PlayerTrackViews(trackView1, trackView2);
        adapter.onConnected();

        verify(trackView1).onDataConnected();
        verify(trackView2).onDataConnected();
    }

    @Test
    public void allPlayerTrackViewsShouldReceiveOnStop() throws Exception {
        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        add2PlayerTrackViews(trackView1, trackView2);
        adapter.onStop();

        verify(trackView1).onStop(true);
        verify(trackView2).onStop(true);
    }

    @Test
    public void allPlayerTrackViewsShouldReceiveOnDestroy() throws Exception {
        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        add2PlayerTrackViews(trackView1, trackView2);
        adapter.onDestroy();

        verify(trackView1).onDestroy();
        verify(trackView2).onDestroy();
    }

    @Test
    public void commentingPositionShouldBeDefault() throws Exception {
        expect(adapter.getCommentingPosition()).toBe(-1);
    }

    @Test
    public void commentingPositionShouldBeDefaultAfterAddition() throws Exception {
        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        add2PlayerTrackViews(trackView1, trackView2);
        expect(adapter.getCommentingPosition()).toBe(-1);
    }

    @Test
    public void shouldSetCommentingPositionBeforeAddition() throws Exception {
        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        adapter.setCommentingPosition(1, true);
        expect(adapter.getCommentingPosition()).toBe(1);

        add2PlayerTrackViews(trackView1, trackView2);
        verify(trackView1).setCommentMode(false);
        verify(trackView2).setCommentMode(true);
    }

    @Test
    public void shouldSetCommentingPositionAfterAddition() throws Exception {
        final PlayerTrackView trackView1 = Mockito.mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = Mockito.mock(PlayerTrackView.class);

        add2PlayerTrackViews(trackView1, trackView2);
        verify(trackView1).setCommentMode(false);
        verify(trackView2).setCommentMode(false);

        adapter.setCommentingPosition(1, true);
        verify(trackView1, times(2)).setCommentMode(false);
        verify(trackView2).setCommentMode(true, true);

        expect(adapter.getCommentingPosition()).toBe(1);
    }

    @Test
    public void shouldReturnExtraItemWhenQueueFetching() throws Exception {
        when(playQueueManager.length()).thenReturn(1);
        when(playQueueManager.isFetchingRelated()).thenReturn(true);
        expect(adapter.getCount()).toBe(2);
    }

    @Test
    public void shouldReturnExtraItemWhenLastQueueFetchFailed() throws Exception {
        when(playQueueManager.length()).thenReturn(1);
        when(playQueueManager.lastRelatedFetchFailed()).thenReturn(true);
        expect(adapter.getCount()).toBe(2);
    }

    public void shouldReturnUnchangedPositionForEmptyItemWhenFetching() throws Exception {
        when(playQueueManager.length()).thenReturn(1);
        when(playQueueManager.isFetchingRelated()).thenReturn(true);
        expect(adapter.getItemPosition(PlayQueueItem.EMPTY)).toBe(PagerAdapter.POSITION_UNCHANGED);
    }

    @Test
    public void shouldReturnNoItemPositionForEmptyItemWhenNotFetching() throws Exception {
        when(playQueueManager.length()).thenReturn(1);
        when(playQueueManager.isFetchingRelated()).thenReturn(false);
        expect(adapter.getItemPosition(PlayQueueItem.EMPTY)).toBe(PagerAdapter.POSITION_NONE);
    }

    @Test
    public void shouldReturnEmptyViewWhenTrackIsNull() throws Exception {
        final ViewGroup parent = Mockito.mock(ViewGroup.class);
        final PlayQueueItem playQueueItem = PlayQueueItem.EMPTY;

        expect(adapter.getView(playQueueItem, null, parent)).toBe(emptyListView);
        verifyZeroInteractions(playerTrackView);
    }

    @Test
    public void shouldConvertEmptyViewWhenPlayqueueItemIsEmpty() throws Exception {
        final ViewGroup parent = Mockito.mock(ViewGroup.class);
        final PlayQueueItem playQueueItem = PlayQueueItem.EMPTY;
        final EmptyListView convertView = Mockito.mock(EmptyListView.class);

        expect(adapter.getView(playQueueItem, convertView, parent)).toBe(convertView);
        verifyZeroInteractions(playerTrackView);
    }

    @Test
    public void shouldSetWaitingStateOnEmptyView() throws Exception {
        final ViewGroup parent = Mockito.mock(ViewGroup.class);
        final PlayQueueItem playQueueItem = PlayQueueItem.EMPTY;
        when(playQueueManager.isFetchingRelated()).thenReturn(true);

        expect(adapter.getView(playQueueItem, null, parent)).toBe(emptyListView);
        verify(emptyListView).setStatus(EmptyListView.Status.WAITING);
    }

    @Test
    public void shouldSetUnknownErrorStateOnEmptyView() throws Exception {
        final ViewGroup parent = Mockito.mock(ViewGroup.class);
        final PlayQueueItem playQueueItem = PlayQueueItem.EMPTY;

        expect(adapter.getView(playQueueItem, null, parent)).toBe(emptyListView);
        verify(emptyListView).setStatus(EmptyListView.Status.ERROR);
    }


    private void add2PlayerTrackViews(PlayerTrackView trackView1, PlayerTrackView trackView2) {
        final ViewGroup parent = Mockito.mock(ViewGroup.class);
        final PlayQueueItem playQueueItem1 = new PlayQueueItem(Mockito.mock(Track.class), 0);
        final PlayQueueItem playQueueItem2 = new PlayQueueItem(Mockito.mock(Track.class), 1);

        expect((PlayerTrackView) adapter.getView(playQueueItem1, trackView1, parent)).toBe(trackView1);
        expect((PlayerTrackView) adapter.getView(playQueueItem2, trackView2, parent)).toBe(trackView2);
    }
}
