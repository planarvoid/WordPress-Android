package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.playqueue.TrackPlayQueueUIItem.PlayState;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayQueuePresenterTest extends AndroidUnitTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlayQueueAdapter adapter;
    @Mock private PlayQueueOperations playQueueOperations;
    @Mock private PlayQueueArtworkController playerArtworkController;
    @Mock private PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory;
    @Mock private EventBus eventbus;
    @Mock private PlayQueueUIItem item;
    @Mock private PlayQueueUIItemMapper playQueueUIItemMapper;
    @Mock private FeedbackController feedbackController;

    private PlayQueuePresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new PlayQueuePresenter(
                adapter,
                playQueueManager,
                playQueueOperations,
                playerArtworkController,
                swipeToRemoveCallbackFactory,
                eventbus,
                context(),
                feedbackController,
                playQueueUIItemMapper);
        when(adapter.getItem(anyInt())).thenReturn(item);
        when(item.isTrack()).thenReturn(true);
    }

    @Test
    public void returnTrueWhenUpcomingTrack() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueItemWithPlayState(PlayState.COMING_UP);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);

        assertThat(presenter.isRemovable(2)).isTrue();
    }

    @Test
    public void returnFalseWhenCurrentTrack() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueItemWithPlayState(PlayState.PLAYING);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void returnFalseWhenPlayedTrack() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueItemWithPlayState(PlayState.PLAYED);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);

        assertThat(presenter.isRemovable(2)).isFalse();
    }

    @Test
    public void shouldRemoveItemAtPosition() {
        final TrackPlayQueueUIItem upcomingTrack = trackPlayQueueItemWithPlayState(PlayState.COMING_UP);
        when(adapter.getItem(2)).thenReturn(upcomingTrack);

        presenter.remove(2);

        verify(adapter).getAdapterPosition(upcomingTrack.getPlayQueueItem());
        verify(adapter).removeItem(2);
        verify(playQueueManager).removeItem(upcomingTrack.getPlayQueueItem());
    }

    private TrackPlayQueueUIItem trackPlayQueueItemWithPlayState(PlayState playState) {
        final Urn track = Urn.forTrack(123);
        final TrackPlayQueueUIItem playQueueUIItem = TrackPlayQueueUIItem
                .from(TestPlayQueueItem.createTrack(track),
                      new TrackItem(TestPropertySets.expectedTrackForListItem(track)),
                      context(),
                      Optional.<String>absent(),
                      PlayQueueManager.RepeatMode.REPEAT_ONE    );

        playQueueUIItem.setPlayState(playState);

        return playQueueUIItem;
    }

}
