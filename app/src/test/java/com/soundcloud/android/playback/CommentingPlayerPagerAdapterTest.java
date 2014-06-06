package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.LegacyPlayerTrackView;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.track.LegacyTrackOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Activity;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class CommentingPlayerPagerAdapterTest {

    private CommentingPlayerPagerAdapter adapter;
    @Mock
    private ViewGroup container;
    @Mock
    private LegacyTrackOperations trackOperations;
    @Mock
    private PlaybackStateProvider stateProvider;

    @Before
    public void setUp() throws Exception {
        adapter = new CommentingPlayerPagerAdapter(trackOperations, stateProvider);

        when(trackOperations.loadSyncedTrack(anyLong(), same(AndroidSchedulers.mainThread()))).thenReturn(Observable.just(new Track()));
        when(container.getContext()).thenReturn(mock(Activity.class));
    }

    @Test
    public void returnsDefaultCommentingPositionIfNotSet() {
        expect(adapter.getCommentingPosition()).toBe(-1);
    }

    @Test
    public void setsCommentingPositionWithValidPosition() {
        final LegacyPlayerTrackView trackView1 = mock(LegacyPlayerTrackView.class);
        final LegacyPlayerTrackView trackView2 = mock(LegacyPlayerTrackView.class);

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
        final LegacyPlayerTrackView trackView1 = mock(LegacyPlayerTrackView.class);
        final LegacyPlayerTrackView trackView2 = mock(LegacyPlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.setCommentingPosition(1, true);
        verify(trackView2).setCommentMode(true, true);
    }

    @Test
    public void setsCommentingModeToFalseOnTrackViewAtOtherPositionsWithValidPositionArgument() {
        final LegacyPlayerTrackView trackView1 = mock(LegacyPlayerTrackView.class);
        final LegacyPlayerTrackView trackView2 = mock(LegacyPlayerTrackView.class);

        addPlayerTrackViews(trackView1, trackView2);
        adapter.setCommentingPosition(1, true);
        verify(trackView1).setCommentMode(false);
    }

    private void addPlayerTrackViews(LegacyPlayerTrackView trackView1, LegacyPlayerTrackView trackView2) {
        adapter.setPlayQueue(new PlayQueueView(Lists.newArrayList(1L, 2L), 0));
        adapter.getView(0, trackView1, container);
        adapter.getView(1, trackView2, container);
    }
}
