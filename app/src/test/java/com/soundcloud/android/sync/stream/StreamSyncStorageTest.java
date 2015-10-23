package com.soundcloud.android.sync.stream;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;

public class StreamSyncStorageTest extends AndroidUnitTest {

    private StreamSyncStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new StreamSyncStorage(sharedPreferences(StorageModule.STREAM_SYNC, Context.MODE_PRIVATE));
    }

    @Test
    public void clearRemovesStoredLinks() {
        storage.storeNextPageUrl(Optional.of(new Link("next")));
        storage.storeFuturePageUrl(new Link("future"));

        storage.clear();

        assertThat(storage.hasNextPageUrl()).isFalse();
        assertThat(storage.isMissingFuturePageUrl()).isTrue();
    }

    @Test
    public void setNextPageUrlStoresLinkIfExists() {
        storage.storeNextPageUrl(Optional.of(new Link("next")));

        assertThat(storage.hasNextPageUrl()).isTrue();
        assertThat(storage.getNextPageUrl()).isEqualTo("next");
    }

    @Test
    public void setNextPageUrlRemovesStoredLinkIfEmpty() {
        storage.storeNextPageUrl(Optional.of(new Link("next")));

        storage.storeNextPageUrl(Optional.<Link>absent());

        assertThat(storage.hasNextPageUrl()).isFalse();
    }

    @Test
    public void setFuturePageUrlStoresLink() {
        storage.storeFuturePageUrl(new Link("future"));

        assertThat(storage.isMissingFuturePageUrl()).isFalse();
        assertThat(storage.getFuturePageUrl()).isEqualTo("future");
    }

}
