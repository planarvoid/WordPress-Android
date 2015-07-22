package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlayQueueDataSourceTest {

    private PlayQueueDataSource playQueueDataSource;

    @Mock private PlayQueueManager playQueueManager;

    @Before
    public void setUp() throws Exception {
        when(playQueueManager.getQueueSize()).thenReturn(2);
        when(playQueueManager.getUrnAtPosition(0)).thenReturn(Urn.forTrack(123L));
        when(playQueueManager.getUrnAtPosition(1)).thenReturn(Urn.forTrack(456L));
        when(playQueueManager.getMetaDataAt(0)).thenReturn(PropertySet.create());
        when(playQueueManager.getMetaDataAt(1)).thenReturn(TestPropertySets.leaveBehindForPlayer());
        playQueueDataSource = new PlayQueueDataSource(playQueueManager);

    }

    @Test
    public void getFullQueueReturnsFullPlayQueue() throws Exception {

        List<TrackPageData> queue = playQueueDataSource.getFullQueue();
        expect(queue).toNumber(2);
        checkTrackPageData(queue.get(0), 0, Urn.forTrack(123L), PropertySet.create());
        checkTrackPageData(queue.get(1), 1, Urn.forTrack(456L), TestPropertySets.leaveBehindForPlayer());
    }

    @Test
    public void getCurrentTrackAsQueueReturnsSingleTrackQueueForAd() throws Exception {
        when(playQueueManager.getCurrentPosition()).thenReturn(1);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(456L));
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.leaveBehindForPlayer());

        List<TrackPageData> queue = playQueueDataSource.getCurrentTrackAsQueue();
        expect(queue).toNumber(1);
        checkTrackPageData(queue.get(0), 1, Urn.forTrack(456L), TestPropertySets.leaveBehindForPlayer());
    }

    private void checkTrackPageData(TrackPageData trackPageData, int position, Urn trackUrn, PropertySet propertySet){
        expect(trackPageData.getPositionInPlayQueue()).toBe(position);
        expect(trackPageData.getTrackUrn()).toEqual(trackUrn);
        expect(trackPageData.getProperties()).toEqual(propertySet);
    }
}