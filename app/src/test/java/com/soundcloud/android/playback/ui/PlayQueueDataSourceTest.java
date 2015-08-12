package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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
        when(playQueueManager.getQueueSize()).thenReturn(2);
        when(playQueueManager.getUrnAtPosition(0)).thenReturn(Urn.forTrack(123L));
        when(playQueueManager.getUrnAtPosition(1)).thenReturn(Urn.forTrack(456L));
        when(playQueueManager.getMetaDataAt(0)).thenReturn(PropertySet.create());
        when(playQueueManager.getMetaDataAt(1)).thenReturn(TestPropertySets.leaveBehindForPlayer());
        when(playQueueManager.getRelatedEntity(0)).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getRelatedEntity(1)).thenReturn(Urn.forTrack(789));
        playQueueDataSource = new PlayQueueDataSource(playQueueManager);

    }

    @Test
    public void getFullQueueReturnsFullPlayQueue() throws Exception {

        List<TrackPageData> queue = playQueueDataSource.getFullQueue();
        assertThat(queue).hasSize(2);
        checkTrackPageData(queue.get(0), 0, Urn.forTrack(123L), PropertySet.create(), Urn.NOT_SET);
        checkTrackPageData(queue.get(1), 1, Urn.forTrack(456L), TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(789));
    }

    @Test
    public void getCurrentTrackAsQueueReturnsSingleTrackQueueForAd() throws Exception {
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(456L));
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.leaveBehindForPlayer());
        when(playQueueManager.getCurrentRelatedEntity()).thenReturn(Urn.forTrack(789));

        List<TrackPageData> queue = playQueueDataSource.getCurrentTrackAsQueue();
        assertThat(queue).hasSize(1);
        checkTrackPageData(queue.get(0), 1, Urn.forTrack(456L), TestPropertySets.leaveBehindForPlayer(), Urn.forTrack(789));
    }

    private void checkTrackPageData(TrackPageData trackPageData,
                                    int position, Urn trackUrn, PropertySet propertySet, Urn relatedEntity){
        assertThat(trackPageData.getPositionInPlayQueue()).isSameAs(position);
        assertThat(trackPageData.getTrackUrn()).isEqualTo(trackUrn);
        assertThat(trackPageData.getProperties()).isEqualTo(propertySet);
        assertThat(trackPageData.getRelatedTrackUrn()).isEqualTo(relatedEntity);
    }
}