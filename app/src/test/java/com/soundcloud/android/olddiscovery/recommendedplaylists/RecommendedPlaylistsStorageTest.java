package com.soundcloud.android.olddiscovery.recommendedplaylists;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class RecommendedPlaylistsStorageTest extends StorageIntegrationTest {

    private TestObserver<List<RecommendedPlaylistsEntity>> testSubscriber = new TestObserver<>();
    private RecommendedPlaylistsStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new RecommendedPlaylistsStorage(propellerRxV2(), Schedulers.trampoline());
    }

    @Test
    public void retrieve() throws Exception {
        ApiRecommendedPlaylistBucket apiBucket = RecommendedPlaylistsFixtures.createApiBucket();
        testFixtures().insertRecommendedPlaylist(apiBucket);
        storage.recommendedPlaylists().subscribe(testSubscriber);

        testSubscriber.assertComplete();
        testSubscriber.assertValueCount(1);

        List<RecommendedPlaylistsEntity> entities = testSubscriber.values().get(0);
        assertThat(entities.size()).isEqualTo(1);

        RecommendedPlaylistsEntity entity = entities.get(0);
        assertThat(entity.getKey()).isEqualTo(apiBucket.key());
        assertThat(entity.getDisplayName()).isEqualTo(apiBucket.displayName());
        assertThat(entity.getOptionalArtworkUrl()).isEqualTo(apiBucket.artworkUrl());

        List<Urn> urns = entity.getPlaylistUrns();
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

        assertThat(testSubscriber.values().get(0).size()).isEqualTo(0);
    }
}
