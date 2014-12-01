package com.soundcloud.android.offline;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineTracksOperationsTest {

    @Mock private TrackDownloadsStorage storage;

    private OfflineTracksOperations tracksOperations;

    @Before
    public void setUp() throws Exception {
        tracksOperations = new OfflineTracksOperations(storage, Robolectric.application);
    }

    @Test
    public void addTracksWritesToTrackDownloadStorage() {
        List<Urn> tracks = Arrays.asList(Urn.forTrack(123L));
        tracksOperations.enqueueTracks(tracks);

        verify(storage).storeRequestedDownloads(tracks);
    }
}

