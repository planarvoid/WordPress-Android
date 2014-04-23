package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playback.PlayerTrackPagerAdapter.ViewFactory;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.PlayerTrackView;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.view.EmptyListView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class PlayerTrackPagerAdapterTest {

    private PlayerTrackPagerAdapter adapter;

    @Mock
    private ViewFactory viewFactory;
    @Mock
    private TrackOperations trackOperations;
    @Mock
    private PlaybackStateProvider stateProvider;
    @Mock
    private PlayQueueView playQueue;
    @Mock
    private EmptyListView emptyListView;
    @Mock
    private PlayerTrackView playerTrackView;
    @Mock
    private ViewGroup container;

    @Before
    public void setUp() throws Exception {
        // TODO remove the override when we move to Robolectric 2
        adapter = new PlayerTrackPagerAdapter(trackOperations, stateProvider, viewFactory);

        when(playQueue.isLoading()).thenReturn(false);
        when(playQueue.lastLoadWasEmpty()).thenReturn(false);
        when(playQueue.lastLoadFailed()).thenReturn(false);
        when(trackOperations.loadSyncedTrack(anyLong(), same(AndroidSchedulers.mainThread()))).thenReturn(Observable.just(new Track()));
        when(viewFactory.createEmptyListView(any(Context.class))).thenReturn(emptyListView);
        when(viewFactory.createPlayerTrackView(any(Context.class))).thenReturn(playerTrackView);
        when(container.getContext()).thenReturn(mock(Activity.class));
    }

    @Test
    public void countIsZeroByDefault() {
        expect(adapter.getCount()).toEqual(0);
    }

    @Test
    public void returnsPlayQueueSize() {
        when(playQueue.size()).thenReturn(10);
        adapter.setPlayQueue(playQueue);
        expect(adapter.getCount()).toEqual(10);
    }

    @Test
    public void returnsPlayQueueSizeFromLatestPlayQueue() {
        adapter.setPlayQueue(playQueue);

        PlayQueueView playQueue2 = Mockito.mock(PlayQueueView.class);
        when(playQueue2.size()).thenReturn(10);
        adapter.setPlayQueue(playQueue2);

        expect(adapter.getCount()).toEqual(10);
    }

    @Test
    public void returnsPlayQueueItemByPosition() throws Exception {
        adapter.setPlayQueue(new PlayQueueView(1L));
        expect(adapter.getIdByPosition(0)).toBe(1L);
    }

    @Test
    public void returnsPlayQueueItemFromLatestPlayQueue() throws Exception {
        adapter.setPlayQueue(new PlayQueueView(1L));
        adapter.setPlayQueue(new PlayQueueView(2L));
        expect(adapter.getIdByPosition(0)).toBe(2L);
    }

    @Test
    public void createEmptyViewWithoutValidPlayQueuePosition() {
        expect(adapter.getView(0, null, mock(ViewGroup.class))).toBe(emptyListView);
    }

    @Test
    public void createsNewPlayerTrackViewFromPlayQueueItemWithValidPlayQueuePosition() {
        adapter.setPlayQueue(new PlayQueueView(1L));
        expect(adapter.getView(0, null, mock(ViewGroup.class))).toBe(playerTrackView);
    }

    @Test
    public void convertsEmptyViewWithoutValidPlayQueuePosition() {
        final EmptyListView convertView = mock(EmptyListView.class);
        expect(adapter.getView(0, convertView, mock(ViewGroup.class))).toBe(convertView);
    }

    @Test
    public void convertsPlayerTrackViewWithValidPlayQueuePosition() {
        adapter.setPlayQueue(new PlayQueueView(1L));
        final PlayerTrackView convertView = mock(PlayerTrackView.class);
        expect(adapter.getView(0, convertView, mock(ViewGroup.class))).toBe(convertView);
    }


    @Test
    public void returnsPlayerTrackViewsByPosition() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);

        expect(adapter.getPlayerTrackViewByPosition(0)).toBe(trackView1);
        expect(adapter.getPlayerTrackViewByPosition(1)).toBe(trackView2);
    }

    @Test
    public void returnsPlayerTrackViewsById() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        when(trackView1.getTrackId()).thenReturn(1L);
        when(trackView2.getTrackId()).thenReturn(2L);

        addPlayerTrackViews(trackView1, trackView2);

        expect(adapter.getPlayerTrackViewById(1L)).toBe(trackView1);
        expect(adapter.getPlayerTrackViewById(2L)).toBe(trackView2);
    }

    @Test
    public void onConnectedIsCalledOnAllPlayerTrackViews() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.onConnected();

        verify(trackView1).onDataConnected();
        verify(trackView2).onDataConnected();
    }

    @Test
    public void onStopIsCalledOnAllPlayerTrackViews() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.onStop();

        verify(trackView1).onStop(true);
        verify(trackView2).onStop(true);
    }

    @Test
    public void onDestroyIsCalledOnAllPlayerTrackViews() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.onDestroy();

        verify(trackView1).onDestroy();
        verify(trackView2).onDestroy();
    }

    @Test
    public void loadsSyncedTrackOnUIThreadByIdWhenGettingViewAtValidPlayQueuePosition() {
        adapter.setPlayQueue(new PlayQueueView(1L));
        adapter.getView(0, null, container);
        verify(trackOperations).loadSyncedTrack(1, AndroidSchedulers.mainThread());
    }

    @Test
    public void setsTrackStatenOnPlayerTrackViewWhenGettingViewAtValidPlayQueuePosition() throws Exception {
        final Track track = new Track(1L);
        adapter.setPlayQueue(new PlayQueueView(1L));
        when(trackOperations.loadSyncedTrack(1, AndroidSchedulers.mainThread())).thenReturn(Observable.just(track));
        adapter.getView(0, null, mock(ViewGroup.class));
        verify(playerTrackView).setTrackState(track, 0, stateProvider);
    }

    @Test
    public void setsAppendStateFromQueueOnEmptyViewWhenGettingViewAtInvalidPlayQueuePosition() throws Exception {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L), 0, PlaybackOperations.AppendState.LOADING));
        adapter.getView(1, null, mock(ViewGroup.class));
        verify(emptyListView).setStatus(EmptyListView.Status.WAITING);
    }

    @Test
    public void setsErrorStateFromQueueOnEmptyViewWhenGettingViewAtInvalidPlayQueuePosition() throws Exception {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L), 0, PlaybackOperations.AppendState.ERROR));
        adapter.getView(1, null, mock(ViewGroup.class));
        verify(emptyListView).setStatus(EmptyListView.Status.ERROR);
    }

    @Test
    public void setsOkStateFromQueueOnEmptyViewWhenGettingViewAtInvalidPlayQueuePosition() throws Exception {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L), 0, PlaybackOperations.AppendState.EMPTY));
        adapter.getView(1, null, mock(ViewGroup.class));
        verify(emptyListView).setStatus(EmptyListView.Status.OK);
    }

    @Test
    public void returnsExtraItemWhenQueueFetching() {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L), 0, PlaybackOperations.AppendState.LOADING));
        expect(adapter.getCount()).toBe(2);
    }

    @Test
    public void returnsExtraItemWhenLastQueueFetchFailed() {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L), 0, PlaybackOperations.AppendState.ERROR));
        expect(adapter.getCount()).toBe(2);
    }

    @Test
    public void returnsExtraItemWhenLastQueueFetchWasEmpty() {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L), 0, PlaybackOperations.AppendState.EMPTY));
        expect(adapter.getCount()).toBe(2);
    }

    @Test
    public void returnsPlayQueueSizeWhenLastQueueFetchIsIdle() {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L), 0, PlaybackOperations.AppendState.IDLE));
        expect(adapter.getCount()).toBe(1);
    }

    private void addPlayerTrackViews(PlayerTrackView trackView1, PlayerTrackView trackView2) {
        addPlayerTrackViews(new PlayQueueView(Lists.newArrayList(1L, 2L), 0), trackView1, trackView2);
    }

    private void addPlayerTrackViews(PlayQueueView playQueue, PlayerTrackView trackView1, PlayerTrackView trackView2) {
        adapter.setPlayQueue(playQueue);
        adapter.getView(0, trackView1, container);
        adapter.getView(1, trackView2, container);
    }

}
