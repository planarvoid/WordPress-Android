package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;

import android.content.Context;
import android.content.SharedPreferences;

public class OfflineSettingsStorageTest extends AndroidUnitTest {

    private final SharedPreferences preferences = sharedPreferences("test", Context.MODE_PRIVATE);

    private OfflineSettingsStorage storage;
    private TestObserver<Boolean> testObserver = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        storage = new OfflineSettingsStorage(preferences);
    }

    @Test
    public void receivedUpdatesFromWifiOnlyOptionChange() {
        storage.getWifiOnlyOfflineSyncStateChange().subscribe(testObserver);
        storage.setWifiOnlyEnabled(true);
        assertThat(testObserver.getOnNextEvents().get(0)).isTrue();
    }

    @Test
    public void savesWifiOnlyFlag() {
        storage.setWifiOnlyEnabled(false);
        assertThat(storage.isWifiOnlyEnabled()).isFalse();
    }

    @Test
    public void offlineStorageIsUnlimitedByDefault() {
        assertThat(storage.hasStorageLimit()).isFalse();
    }

    @Test
    public void offlineStorageLimitCanBeSet() {
        storage.setStorageLimit(1000);

        assertThat(storage.hasStorageLimit()).isTrue();
        assertThat(storage.getStorageLimit()).isEqualTo(1000);
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
    public void clearsSettingsStorage() {
        storage.setWifiOnlyEnabled(false);
        storage.clear();

        assertThat(storage.isWifiOnlyEnabled()).isTrue();
    }
}