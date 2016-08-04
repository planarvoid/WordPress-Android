package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayQueueRecyclerItemAdapterTest extends AndroidUnitTest {

    @Mock private PlayQueueItemRenderer renderer;
    private PlayQueueRecyclerItemAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new PlayQueueRecyclerItemAdapter(renderer);
    }

    @Test
    public void getItemIdWhenOneOccurrence() {
        final TrackItem trackItem = TrackItem.from(TestPropertySets.fromApiTrack());
        adapter.addItem(trackItem);

        assertThat(adapter.getItemId(0)).isEqualTo(trackItem.getUrn().getNumericId());
    }

    @Test
    public void getItemIdWhenTwoOccurrence() {
        final TrackItem trackItem = TrackItem.from(TestPropertySets.fromApiTrack());
        adapter.addItem(trackItem);
        adapter.addItem(trackItem);

        assertThat(adapter.getItemId(0)).isEqualTo(trackItem.getUrn().getNumericId());
        assertThat(adapter.getItemId(1)).isEqualTo(-1 * trackItem.getUrn().getNumericId());
    }

    @Test
    public void getItemIdWhenThreeOccurrence() {
        final TrackItem trackItem = TrackItem.from(TestPropertySets.fromApiTrack());
        adapter.addItem(trackItem);
        adapter.addItem(trackItem);
        adapter.addItem(trackItem);

        assertThat(adapter.getItemId(0)).isEqualTo(trackItem.getUrn().getNumericId());
        assertThat(adapter.getItemId(1)).isEqualTo(-1 * trackItem.getUrn().getNumericId());
        assertThat(adapter.getItemId(2)).isEqualTo(-2 * trackItem.getUrn().getNumericId());
    }

    @Test
    public void clearResetUniqueIds() {
        final TrackItem trackItem = TrackItem.from(TestPropertySets.fromApiTrack());
        adapter.addItem(trackItem);
        adapter.addItem(trackItem);
        adapter.clear();
        adapter.addItem(trackItem);

        assertThat(adapter.getItemId(0)).isEqualTo(trackItem.getUrn().getNumericId());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void clearClearUniqueIds() {
        final TrackItem trackItem = TrackItem.from(TestPropertySets.fromApiTrack());
        adapter.addItem(trackItem);
        adapter.addItem(trackItem);
        adapter.clear();
        adapter.addItem(trackItem);

        assertThat(adapter.getItemId(1)).isEqualTo(trackItem.getUrn().getNumericId());
    }
}
