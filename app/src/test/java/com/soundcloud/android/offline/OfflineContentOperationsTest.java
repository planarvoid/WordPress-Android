package com.soundcloud.android.offline;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflineContentOperationsTest {

    @Mock private TrackDownloadsStorage downloadsStorage;
    @Mock private SoundAssociationOperations soundAssociationOps;
    @Mock private OfflineSettingsStorage settingsStorage;

    private final List<Urn> LIKED_TRACKS = Arrays.asList(Urn.forTrack(123L));
    private OfflineContentOperations offlineOperations;

    @Before
    public void setUp() throws Exception {
        offlineOperations = new OfflineContentOperations(downloadsStorage, soundAssociationOps, settingsStorage);
        when(soundAssociationOps.getLikedTracks()).thenReturn(Observable.just(LIKED_TRACKS));
    }

    @Test
    public void updateFromLikesCallsSoundAssociationOperations() {
        offlineOperations.updateOfflineLikes().subscribe();

        verify(soundAssociationOps).getLikedTracks();
    }

    @Test
    public void updateFromLikesCallsDownloadStorageWithAGivenListOfLikes() {
        offlineOperations.updateOfflineLikes().subscribe();

        verify(downloadsStorage).filterAndStoreNewDownloadRequests(LIKED_TRACKS);
    }
}
