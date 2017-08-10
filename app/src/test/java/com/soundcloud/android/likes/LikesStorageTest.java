package com.soundcloud.android.likes;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LikesStorageTest extends StorageIntegrationTest {

    private static final Date LIKED_AT_1 = new Date(1000);
    private static final Date LIKED_AT_2 = new Date(2000);
    private ApiTrack apiTrack1;
    private ApiTrack apiTrack2;

    LikesStorage likesStorage;

    @Before
    public void setUp() throws Exception {
        likesStorage = new LikesStorage(propellerRxV2());
    }

    @Test
    public void loadsLikesFromStorage() {
        testFixtures().insertLike(1, Tables.Sounds.TYPE_TRACK, new Date());
        testFixtures().insertLike(2, Tables.Sounds.TYPE_PLAYLIST, new Date());

        final TestObserver<List<Urn>> subscriber = likesStorage.loadLikes().test();

        subscriber.assertValue(Arrays.asList(Urn.forTrack(1), Urn.forPlaylist(2)));

    }

    @Test
    public void loadsLikedTracks() {
        initLikes();

        List<Like> likes = likesStorage.loadTrackLikes().test().assertValueCount(1).values().get(0);

        assertThat(likes).containsExactly(
                Like.create(apiTrack2.getUrn(), LIKED_AT_2),
                Like.create(apiTrack1.getUrn(), LIKED_AT_1)
        );
    }

    @Test
    public void loadsLikedTracksWithLimit() {
        initLikes();

        List<Like> likes = likesStorage.loadTrackLikes(3000, 1).test().assertValueCount(1).values().get(0);

        assertThat(likes).containsExactly(
                Like.create(apiTrack2.getUrn(), LIKED_AT_2)
        );
    }

    @Test
    public void loadsLikedTracksWithBeforeDate() {
        initLikes();

        List<Like> likes = likesStorage.loadTrackLikes(2000, 1).test().assertValueCount(1).values().get(0);

        assertThat(likes).containsExactly(
                Like.create(apiTrack1.getUrn(), LIKED_AT_1)
        );
    }

    private void initLikes() {
        apiTrack1 = testFixtures().insertLikedTrack(LIKED_AT_1);
        apiTrack2 = testFixtures().insertLikedTrack(LIKED_AT_2);
    }
}
