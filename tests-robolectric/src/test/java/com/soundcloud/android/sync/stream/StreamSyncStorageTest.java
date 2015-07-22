package com.soundcloud.android.sync.stream;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class StreamSyncStorageTest {

    private StreamSyncStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new StreamSyncStorage(new ScTestSharedPreferences());
    }

    @Test
    public void clearRemovesStoredLinks() {
        storage.storeNextPageUrl(Optional.of(new Link("next")));
        storage.storeFuturePageUrl(new Link("future"));

        storage.clear();

        expect(storage.hasNextPageUrl()).toBeFalse();
        expect(storage.isMissingFuturePageUrl()).toBeTrue();
    }

    @Test
    public void setNextPageUrlStoresLinkIfExists() {
        storage.storeNextPageUrl(Optional.of(new Link("next")));

        expect(storage.hasNextPageUrl()).toBeTrue();
        expect(storage.getNextPageUrl()).toEqual("next");
    }

    @Test
    public void setNextPageUrlRemovesStoredLinkIfEmpty() {
        storage.storeNextPageUrl(Optional.of(new Link("next")));

        storage.storeNextPageUrl(Optional.<Link>absent());

        expect(storage.hasNextPageUrl()).toBeFalse();
    }

    @Test
    public void setFuturePageUrlStoresLink() {
        storage.storeFuturePageUrl(new Link("future"));

        expect(storage.isMissingFuturePageUrl()).toBeFalse();
        expect(storage.getFuturePageUrl()).toEqual("future");
    }

}
