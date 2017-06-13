package com.soundcloud.android.collection.recentlyplayed;

import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class RecentlyPlayedStorageTest extends StorageIntegrationTest {

    private RecentlyPlayedStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new RecentlyPlayedStorage(propeller(), propellerRxV2());
    }

    @Test
    public void loadCorrectOfflineStateForPlaylistMarkedAsOffline() {
        final ApiPlaylist apiPlaylist = insertDownloadedOfflinePlaylist();
        testFixtures().insertRecentlyPlayed(100, apiPlaylist.getUrn());

        final TestObserver<List<RecentlyPlayedPlayableItem>> subscriber = storage.loadContexts(1).test();

        subscriber.assertValue(singletonList(getRecentlyPlayedItem(apiPlaylist, Optional.of(OfflineState.DOWNLOADED), 100, false)));
    }

    @Test
    public void loadCorrectOfflineStateForPlaylistNotMarkedAsOffline() {
        final ApiPlaylist apiPlaylist = insertRecentPlaylist(0L);

        final TestObserver<List<RecentlyPlayedPlayableItem>> subscriber = storage.loadContexts(1).test();

        subscriber.assertValue(singletonList(getRecentlyPlayedItem(apiPlaylist, Optional.absent(), 0L, false)));

    }

    @Test
    public void loadCorrectLikedPlaylist() {
        ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        testFixtures().insertRecentlyPlayed(100, apiPlaylist.getUrn());

        final TestObserver<List<RecentlyPlayedPlayableItem>> subscriber = storage.loadContexts(1).test();

        subscriber.assertValue(singletonList(getRecentlyPlayedItem(apiPlaylist, Optional.absent(), 100L, true)));

    }

    @Test
    public void loadPlaylistsInCorrectDescOrder() {
        final long firstTimestamp = 100L;
        final ApiPlaylist firstRecentPlaylist = insertRecentPlaylist(firstTimestamp);
        final long secondTimestamp = 200L;
        final ApiPlaylist secondRecentPlaylist = insertRecentPlaylist(secondTimestamp);

        final TestObserver<List<RecentlyPlayedPlayableItem>> subscriber = storage.loadContexts(2).test();

        subscriber.assertValue(Lists.newArrayList(getRecentlyPlayedItem(secondRecentPlaylist, Optional.absent(), secondTimestamp, true),
                                                  getRecentlyPlayedItem(firstRecentPlaylist, Optional.absent(), firstTimestamp, true)));

    }

    private ApiPlaylist insertRecentPlaylist(long timestamp) {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertRecentlyPlayed(timestamp, apiPlaylist.getUrn());
        return apiPlaylist;
    }

    private ApiPlaylist insertDownloadedOfflinePlaylist() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(apiPlaylist);
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), 100L, 101L);
        return apiPlaylist;
    }

    private RecentlyPlayedPlayableItem getRecentlyPlayedItem(ApiPlaylist playlist,
                                                             Optional<OfflineState> offlineState,
                                                             long timestamp,
                                                             boolean liked) {
        return new RecentlyPlayedPlayableItem(playlist.getUrn(),
                                              playlist.getImageUrlTemplate(),
                                              playlist.getTitle(),
                                              playlist.getTrackCount(),
                                              playlist.isAlbum(),
                                              offlineState,
                                              liked,
                                              !playlist.isPublic(),
                                              timestamp);

    }

}
