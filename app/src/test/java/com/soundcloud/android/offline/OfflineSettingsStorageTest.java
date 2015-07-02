package com.soundcloud.android.offline;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.PlatformUnitTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;

import android.content.Context;
import android.content.SharedPreferences;

public class OfflineSettingsStorageTest extends PlatformUnitTest {

    private final SharedPreferences preferences = sharedPreferences("test", Context.MODE_PRIVATE);

    private OfflineSettingsStorage storage;
    private TestObserver<Boolean> testObserver = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        storage = new OfflineSettingsStorage(preferences);
    }

    @Test
    public void savesOfflineLikesStatus() {
        storage.setOfflineLikedTracksEnabled(true);
        assertThat(storage.isOfflineLikedTracksEnabled()).isTrue();
    }

    @Test
    public void receivesUpdatesFromLikeStatusChanges() {
        storage.getOfflineLikedTracksStatusChange().subscribe(testObserver);
        storage.setOfflineLikedTracksEnabled(true);
        assertThat(testObserver.getOnNextEvents().get(0)).isTrue();
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
    public void clearsSettingsStorage() {
        storage.setOfflineLikedTracksEnabled(true);
        storage.clear();

        assertThat(storage.isOfflineLikedTracksEnabled()).isFalse();
        assertThat(storage.isWifiOnlyEnabled()).isTrue();
    }

    @Test
    public void getPolicyUpdateCheckTimeReturns0ByDefault() {
        assertThat(storage.getPolicyUpdateCheckTime()).isEqualTo(0L);
    }

    @Test
    public void getReturnsSetPolicyUpdateCheckTime() {
        storage.setPolicyUpdateCheckTime(123456789L);
        assertThat(storage.getPolicyUpdateCheckTime()).isEqualTo(123456789L);
    }
}