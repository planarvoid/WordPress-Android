package com.soundcloud.android.likes;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LikesStorageTest extends StorageIntegrationTest {

    LikesStorage likesStorage;

    @Before
    public void setUp() throws Exception {
        likesStorage = new LikesStorage(propellerRx());
    }

    @Test
    public void loadsLikesFromStorage() {
        testFixtures().insertLike(1, Tables.Sounds.TYPE_TRACK, new Date());
        testFixtures().insertLike(2, Tables.Sounds.TYPE_PLAYLIST, new Date());

        TestSubscriber<List<Urn>> subscriber = new TestSubscriber<>();
        likesStorage.loadLikes().subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(Urn.forTrack(1), Urn.forPlaylist(2)));

    }
}
