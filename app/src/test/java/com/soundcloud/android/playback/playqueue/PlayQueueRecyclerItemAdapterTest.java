package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.NonNull;

public class PlayQueueRecyclerItemAdapterTest extends AndroidUnitTest {

    private PlayQueueRecyclerItemAdapter adapter;

    @Mock private PlayQueueItemRenderer playQueueItemRenderer;

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
    public void updateNowPlayingSetsPlayingStateOnItems() {
        adapter.updateNowPlaying(1);

        assertThat(playQueueItem1.isPlaying()).isFalse();
        assertThat(playQueueItem2.isPlaying()).isTrue();
        assertThat(playQueueItem3.isPlaying()).isFalse();
    }

    @Test
    public void updateNowPlayingSetsDraggableStateOnItems() {
        adapter.updateNowPlaying(1);

        assertThat(playQueueItem1.isDraggable()).isFalse();
        assertThat(playQueueItem2.isDraggable()).isFalse();
        assertThat(playQueueItem3.isDraggable()).isTrue();
    }

    @Test
    public void updateRepeatModeStateOnItems() {
        adapter.updateInRepeatMode(true);

        assertThat(playQueueItem1.isInRepeatMode()).isTrue();
        assertThat(playQueueItem2.isInRepeatMode()).isTrue();
        assertThat(playQueueItem3.isInRepeatMode()).isTrue();
    }

    @NonNull
    public PlayQueueUIItem getPlayQueueItem(int id) {
        return new PlayQueueUIItem(id,
                                   Urn.forTrack(1),
                                   "title1",
                                   "creator1",
                                   false,
                                   1,
                                   mock(SimpleImageResource.class),
                                   TrackItem.from(TestPropertySets.fromApiTrack()),
                                   false);
    }
}
