package com.soundcloud.android.storage;

import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.Pair;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

public class DebugStorageTest extends StorageIntegrationTest {

    private DebugStorage debugStorage;

    @Before
    public void setUp() throws Exception {
        debugStorage = new DebugStorage(propellerRxV2());
    }

    @Test
    public void returnsCounts() throws Exception {
        testFixtures().insertTrack();
        testFixtures().insertUser();
        testFixtures().insertPlaylist();

        TestObserver<Pair<String, Integer>> test = debugStorage.tableSizes().test();

        test.assertValues(
                Pair.of("android_metadata", 1),
                Pair.of("Users", 3),
                Pair.of("sqlite_sequence", 1),
                Pair.of("Sounds", 2),
                Pair.of("TrackPolicies", 1),
                Pair.of("Likes", 0),
                Pair.of("Posts", 0),
                Pair.of("UserAssociations", 0),
                Pair.of("Recommendations", 0),
                Pair.of("RecommendationSeeds", 0),
                Pair.of("PlayQueue", 0),
                Pair.of("Stations", 0),
                Pair.of("StationsPlayQueues", 0),
                Pair.of("StationsCollections", 0),
                Pair.of("TrackDownloads", 0),
                Pair.of("OfflineContent", 0),
                Pair.of("Comments", 0),
                Pair.of("Charts", 0),
                Pair.of("ChartTracks", 0),
                Pair.of("PlayHistory", 0),
                Pair.of("RecentlyPlayed", 0),
                Pair.of("SuggestedCreators", 0),
                Pair.of("RecommendedPlaylist", 0),
                Pair.of("RecommendedPlaylistBucket", 0),
                Pair.of("SoundStream", 0),
                Pair.of("PromotedTracks", 0),
                Pair.of("Activities", 0),
                Pair.of("PlaylistTracks", 0),
                Pair.of("Collections", 0)
        );
    }
}
