package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
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
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class CommentingPlayerPagerAdapterTest {

    private CommentingPlayerPagerAdapter adapter;
    @Mock
    private ViewGroup container;
    @Mock
    private PlayerTrackPagerAdapter.ViewFactory viewFactory;
    @Mock
    private TrackOperations trackOperations;
    @Mock
    private PlaybackStateProvider stateProvider;
    @Mock
    private EmptyListView emptyListView;
    @Mock
    private PlayerTrackView playerTrackView;

    @Before
    public void setUp() throws Exception {
        adapter = new CommentingPlayerPagerAdapter(trackOperations, stateProvider, viewFactory);

        when(trackOperations.loadSyncedTrack(anyLong(), same(AndroidSchedulers.mainThread()))).thenReturn(Observable.just(new Track()));
        when(viewFactory.createEmptyListView(any(Context.class))).thenReturn(emptyListView);
        when(viewFactory.createPlayerTrackView(any(Context.class))).thenReturn(playerTrackView);
        when(container.getContext()).thenReturn(mock(Activity.class));
    }

    @Test
    public void returnsDefaultCommentingPositionIfNotSet() {
        expect(adapter.getCommentingPosition()).toBe(-1);
    }

    @Test
    public void setsCommentingPositionWithValidPosition() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.setCommentingPosition(1, true);
        expect(adapter.getCommentingPosition()).toBe(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIllegalArgumentExceptionWhenInvalidCommentingPositionPassed() {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L, 2L), 0));
        adapter.setCommentingPosition(3, true);
    }

    @Test
    public void setsCommentingModeToTrueOnTrackViewAtValidPosition() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.setCommentingPosition(1, true);
        verify(trackView2).setCommentMode(true, true);
    }

    @Test
    public void setsCommentingModeToFalseOnTrackViewAtOtherPositionsWithValidPositionArgument() {
        final PlayerTrackView trackView1 = mock(PlayerTrackView.class);
        final PlayerTrackView trackView2 = mock(PlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.setCommentingPosition(1, true);
        verify(trackView1).setCommentMode(false);
    }

    private void addPlayerTrackViews(PlayerTrackView trackView1, PlayerTrackView trackView2) {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L, 2L), 0));
        adapter.getView(0, trackView1, container);
        adapter.getView(1, trackView2, container);
    }
}
