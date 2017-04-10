package com.soundcloud.android.collection.recentlyplayed;

import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Date;
import java.util.List;

public class RecentlyPlayedStorageTest extends StorageIntegrationTest {

    private RecentlyPlayedStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new RecentlyPlayedStorage(propeller());
    }

    @Test
    public void loadCorrectOfflineStateForPlaylistMarkedAsOffline() {
        final TestSubscriber<List<RecentlyPlayedPlayableItem>> subscriber = new TestSubscriber<>();
        final ApiPlaylist apiPlaylist = insertDownloadedOfflinePlaylist();
        testFixtures().insertRecentlyPlayed(100, apiPlaylist.getUrn());

        storage.loadContexts(1).subscribe(subscriber);

        subscriber.assertValue(singletonList(getRecentlyPlayedItem(apiPlaylist, Optional.of(OfflineState.DOWNLOADED), 100, false)));
    }

    @Test
    public void loadCorrectOfflineStateForPlaylistNotMarkedAsOffline() {
        final TestSubscriber<List<RecentlyPlayedPlayableItem>> subscriber = new TestSubscriber<>();
        final ApiPlaylist apiPlaylist = insertRecentPlaylist();

        storage.loadContexts(1).subscribe(subscriber);

        subscriber.assertValue(singletonList(getRecentlyPlayedItem(apiPlaylist, Optional.absent(), 0L, false)));

    }

    @Test
    public void loadCorrectLikedPlaylist() {
        final TestSubscriber<List<RecentlyPlayedPlayableItem>> subscriber = new TestSubscriber<>();
        ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        testFixtures().insertRecentlyPlayed(100, apiPlaylist.getUrn());

        storage.loadContexts(1).subscribe(subscriber);

        subscriber.assertValue(singletonList(getRecentlyPlayedItem(apiPlaylist, Optional.absent(), 100L, true)));

    }

    private ApiPlaylist insertRecentPlaylist() {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        testFixtures().insertRecentlyPlayed(0L, apiPlaylist.getUrn());
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
