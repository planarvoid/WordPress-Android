package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class PlayQueueDataSourceTest extends AndroidUnitTest {

    private PlayQueueDataSource playQueueDataSource;

    @Mock private PlayQueueManager playQueueManager;

    @Before
    public void setUp() throws Exception {
        when(playQueueManager.getQueueSize()).thenReturn(3);
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getPlayQueueItemAtPosition(0))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), PropertySet.create()));
        when(playQueueManager.getPlayQueueItemAtPosition(1))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(456L), TestPropertySets.audioAdProperties(Urn.forTrack(123L))));
        when(playQueueManager.getPlayQueueItemAtPosition(2))
                .thenReturn(TestPlayQueueItem.createVideo(TestPropertySets.videoAdProperties(Urn.forTrack(722L))));

        playQueueDataSource = new PlayQueueDataSource(playQueueManager);
    }

    @Test
    public void getFullQueueReturnsFullPlayQueue() throws Exception {
        List<PlayerPageData> queue = playQueueDataSource.getFullQueue();
        assertThat(queue).hasSize(3);
        checkTrackPageData(queue.get(0), 0, Urn.NOT_SET, Urn.forTrack(123L), PropertySet.create());
        checkTrackPageData(queue.get(1), 1, Urn.NOT_SET, Urn.forTrack(456L), TestPropertySets.audioAdProperties(Urn.forTrack(123L)));
        checkVideoPageData(queue.get(2), 2, TestPropertySets.videoAdProperties(Urn.forTrack(722L)));
    }

    @Test
    public void getCurrentTrackAsQueueReturnsSingleTrackQueueForAudioAd() throws Exception {
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(456L), TestPropertySets.audioAdProperties(Urn.forTrack(123L))));

        List<PlayerPageData> queue = playQueueDataSource.getCurrentItemAsQueue();
        assertThat(queue).hasSize(1);
        checkTrackPageData(queue.get(0), 1, Urn.NOT_SET, Urn.forTrack(456L), TestPropertySets.audioAdProperties(Urn.forTrack(123L)));
    }

    @Test
    public void getCurrentTrackAsQueueReturnsSingleTrackQueueForVideoAd() throws Exception {
        when(playQueueManager.getCurrentPosition()).thenReturn(2);
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(TestPropertySets.videoAdProperties(Urn.forTrack(722L))));

        List<PlayerPageData> queue = playQueueDataSource.getCurrentItemAsQueue();
        assertThat(queue).hasSize(1);
        checkVideoPageData(queue.get(0), 2, TestPropertySets.videoAdProperties(Urn.forTrack(722L)));
    }

    private void checkTrackPageData(PlayerPageData playerPageData,
                                    int position,
                                    Urn collectionUrn,
                                    Urn trackUrn,
                                    PropertySet propertySet){
        final TrackPageData trackPageData = (TrackPageData) playerPageData;
        assertThat(trackPageData.isTrackPage()).isTrue();
        assertThat(trackPageData.getPositionInPlayQueue()).isSameAs(position);
        assertThat(trackPageData.getTrackUrn()).isEqualTo(trackUrn);
        assertThat(trackPageData.getProperties()).isEqualTo(propertySet);
        assertThat(trackPageData.getCollectionUrn()).isEqualTo(collectionUrn);
    }

    private void checkVideoPageData(PlayerPageData playerPageData,
                                    int position,
                                    PropertySet propertySet){
        final VideoPageData videoPageData = (VideoPageData) playerPageData;
        assertThat(videoPageData.isVideoPage()).isTrue();
        assertThat(videoPageData.getPositionInPlayQueue()).isSameAs(position);
        assertThat(videoPageData.getProperties()).isEqualTo(propertySet);
    }
}