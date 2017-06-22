package com.soundcloud.android.likes;

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
}
