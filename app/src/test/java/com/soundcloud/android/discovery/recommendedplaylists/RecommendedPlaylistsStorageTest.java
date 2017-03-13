package com.soundcloud.android.discovery.recommendedplaylists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;

public class RecommendedPlaylistsStorageTest extends StorageIntegrationTest {

    private TestSubscriber<List<RecommendedPlaylistsEntity>> testSubscriber = new TestSubscriber<>();
    private RecommendedPlaylistsStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new RecommendedPlaylistsStorage(propellerRx(), Schedulers.immediate());
    }

    @Test
    public void retrieve() throws Exception {
        ApiRecommendedPlaylistBucket apiBucket = RecommendedPlaylistsFixtures.createApiBucket();
        testFixtures().insertRecommendedPlaylist(apiBucket);
        storage.recommendedPlaylists().subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        testSubscriber.assertValueCount(1);

        List<RecommendedPlaylistsEntity> entities = testSubscriber.getOnNextEvents().get(0);
        assertThat(entities.size()).isEqualTo(1);

        RecommendedPlaylistsEntity entity = entities.get(0);
        assertThat(entity.key()).isEqualTo(apiBucket.key());
        assertThat(entity.displayName()).isEqualTo(apiBucket.displayName());
        assertThat(entity.artworkUrl()).isEqualTo(apiBucket.artworkUrl());

        List<Urn> urns = entity.playlistUrns();
        assertThat(urns.size()).isEqualTo(apiBucket.playlists().size());

        for (ApiPlaylist apiPlaylist : apiBucket.playlists()) {
            assertThat(urns.contains(apiPlaylist.getUrn())).isTrue();
        }
    }

    @Test
    public void retrieveEmpty() throws Exception {
        storage.recommendedPlaylists().subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        assertThat(testSubscriber.getOnNextEvents().get(0).size()).isEqualTo(0);
    }
}
