package com.soundcloud.android.likes;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;

public class LikesOfflineStateStorageTest extends StorageIntegrationTest {

    private LikesOfflineStateStorage likesStorage;

    @Before
    public void setUp() throws Exception {
        likesStorage = new LikesOfflineStateStorage(propellerRxV2());
    }

    @Test
    public void shouldLoadLikedTrackOfflineStates() {
        ApiTrack apiTrack1 = testFixtures().insertLikedTrack(new Date());
        ApiTrack apiTrack2 = testFixtures().insertLikedTrack(new Date());
        ApiTrack apiTrack3 = testFixtures().insertLikedTrack(new Date());
        ApiTrack apiTrack4 = testFixtures().insertLikedTrack(new Date());

        testFixtures().insertCompletedTrackDownload(apiTrack1.getUrn(), 1000, 2000);
        testFixtures().insertUnavailableTrackDownload(apiTrack2.getUrn(), 1000);
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack3.getUrn(), 3000);
        testFixtures().insertTrackPendingDownload(apiTrack4.getUrn(), 1000);

        final Collection<OfflineState> likedTracks = likesStorage.loadLikedTrackOfflineState().test().assertValueCount(1).values().get(0);

        assertThat(likedTracks).containsExactly(
                OfflineState.DOWNLOADED,
                OfflineState.UNAVAILABLE,
                OfflineState.NOT_OFFLINE,
                OfflineState.REQUESTED);
    }
}
