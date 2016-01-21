package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;

public class OfflineContentStorageTest extends AndroidUnitTest {

    private OfflineContentStorage storage;
    @Mock private IsOfflineLikedTracksEnabledCommand isOfflineLikedTracksEnabledCommand;

    @Before
    public void setUp() {
        storage = new OfflineContentStorage(null, sharedPreferences("Test", Context.MODE_PRIVATE), isOfflineLikedTracksEnabledCommand);
    }

    @Test
    public void offlineContentFlagIsNotSetByDefault() {
        assertThat(storage.hasOfflineContent()).isFalse();
    }

    @Test
    public void savesOfflineContentFlag() {
        storage.setHasOfflineContent(true);

        assertThat(storage.hasOfflineContent()).isTrue();
    }

    @Test
    public void isOfflineCollectionEnabledReturnsFalseByDefault() {
        assertThat(storage.isOfflineCollectionEnabled()).isFalse();
    }

    @Test
    public void storeOfflineCollectionEnabled() {
        storage.storeOfflineCollectionEnabled();

        assertThat(storage.isOfflineCollectionEnabled()).isTrue();
    }

    @Test
    public void storeOfflineCollectionDisabled() {
        storage.storeOfflineCollectionDisabled();

        assertThat(storage.isOfflineCollectionEnabled()).isFalse();
    }
}
