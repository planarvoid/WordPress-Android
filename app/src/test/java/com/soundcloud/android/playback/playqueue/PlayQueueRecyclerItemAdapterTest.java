package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayQueueRecyclerItemAdapterTest extends AndroidUnitTest {

    private PlayQueueRecyclerItemAdapter adapter;

    @Mock
    private PlayQueueItemRenderer playQueueItemRenderer;

    private PlayQueueUIItem playQueueItem1;
    private PlayQueueUIItem playQueueItem2;
    private PlayQueueUIItem playQueueItem3;

    @Before
    public void setUp() throws Exception {
        adapter = new PlayQueueRecyclerItemAdapter(playQueueItemRenderer);

        playQueueItem1 = getPlayQueueItem(1);
        adapter.addItem(playQueueItem1);

        playQueueItem2 = getPlayQueueItem(2);
        adapter.addItem(playQueueItem2);

        playQueueItem3 = getPlayQueueItem(3);
        adapter.addItem(playQueueItem3);
    }

    @Test
    public void updateNowPlayingSetsDraggableStateOnItems() {
        adapter.updateNowPlaying(1);

        assertThat(playQueueItem1.getPlayState()).isEqualTo(PlayQueueUIItem.PlayState.PLAYED);
        assertThat(playQueueItem2.getPlayState()).isEqualTo(PlayQueueUIItem.PlayState.PLAYING);
        assertThat(playQueueItem3.getPlayState()).isEqualTo(PlayQueueUIItem.PlayState.COMING_UP);
    }

    @Test
    public void updateRepeatModeStateOnItems() {
        adapter.updateInRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);

        assertThat(playQueueItem1.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem2.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem3.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
    }

    public PlayQueueUIItem getPlayQueueItem(int uniqueId) {
        final Urn track = Urn.forTrack(uniqueId);
        final TrackQueueItem trackQueueItem = new TrackQueueItem(track, Urn.NOT_SET, Urn.NOT_SET, "source", "version", Optional.<AdData>absent(), false, Urn.NOT_SET, Urn.NOT_SET, false);
        final TrackItem trackItem = new TrackItem(TestPropertySets.expectedTrackForListItem(track));
        final int someReourceId = 123;
        final int color = 321;
        return new PlayQueueUIItem(trackQueueItem, trackItem, uniqueId, someReourceId, color, null);
    }
}
