package com.soundcloud.android.playback.playqueue;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackContext;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlayQueueAdapterTest extends AndroidUnitTest {

    private PlayQueueAdapter adapter;

    @Mock private TrackPlayQueueItemRenderer trackPlayQueueItemRenderer;
    @Mock private HeaderPlayQueueItemRenderer headerPlayQueueItemRenderer;

    private TrackPlayQueueUIItem playQueueItem1;
    private TrackPlayQueueUIItem playQueueItem2;
    private TrackPlayQueueUIItem playQueueItem3;

    @Before
    public void setUp() throws Exception {
        adapter = new PlayQueueAdapter(trackPlayQueueItemRenderer, headerPlayQueueItemRenderer);

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

        assertThat(playQueueItem1.getPlayState()).isEqualTo(TrackPlayQueueUIItem.PlayState.PLAYED);
        assertThat(playQueueItem2.getPlayState()).isEqualTo(TrackPlayQueueUIItem.PlayState.PLAYING);
        assertThat(playQueueItem3.getPlayState()).isEqualTo(TrackPlayQueueUIItem.PlayState.COMING_UP);
    }

    @Test
    public void updateRepeatModeStateOnItems() {
        adapter.updateInRepeatMode(PlayQueueManager.RepeatMode.REPEAT_ONE);

        assertThat(playQueueItem1.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem2.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
        assertThat(playQueueItem3.getRepeatMode()).isEqualTo(PlayQueueManager.RepeatMode.REPEAT_ONE);
    }

    public TrackPlayQueueUIItem getPlayQueueItem(int uniqueId) {
        final Urn track = Urn.forTrack(uniqueId);
        final PlaybackContext playbackContext = PlaybackContext.create(PlaySessionSource.EMPTY);
        final TrackQueueItem trackQueueItem = new TrackQueueItem(track, Urn.NOT_SET, Urn.NOT_SET, "source", "version",
                                                                 Optional.<AdData>absent(), false, Urn.NOT_SET,
                                                                 Urn.NOT_SET, false, playbackContext);
        final TrackItem trackItem = new TrackItem(TestPropertySets.expectedTrackForListItem(track));
        final int someReourceId = 123;
        final int color = 321;
        return new TrackPlayQueueUIItem(trackQueueItem, trackItem, uniqueId, someReourceId, color, null);
    }
}
