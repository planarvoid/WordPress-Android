package com.soundcloud.android.offline;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import android.content.SharedPreferences;

public class OfflineSettingsStorageTest extends AndroidUnitTest {

    private final SharedPreferences preferences = sharedPreferences();

    private OfflineSettingsStorage storage;
    private TestSubscriber<Boolean> wifiOnlyObserver = new TestSubscriber<>();
    private TestSubscriber<Void> offlineContentLocationObserver = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        storage = new OfflineSettingsStorage(preferences, context());
    }

    @Test
    public void receivedUpdatesFromWifiOnlyOptionChange() {
        storage.getWifiOnlyOfflineSyncStateChange().subscribe(wifiOnlyObserver);
        storage.setWifiOnlyEnabled(true);
        assertThat(wifiOnlyObserver.getOnNextEvents().get(0)).isTrue();
    }

    @Test
    public void receivedUpdatesFromOfflineContentLocationChange() {
        storage.getOfflineContentLocationChange().subscribe(offlineContentLocationObserver);
        storage.setOfflineContentLocation(OfflineContentLocation.SD_CARD);
        storage.setOfflineContentLocation(OfflineContentLocation.DEVICE_STORAGE);
        assertThat(offlineContentLocationObserver.getOnNextEvents().size()).isEqualTo(2);
    }

    @Test
    public void savesWifiOnlyFlag() {
        storage.setWifiOnlyEnabled(false);
        assertThat(storage.isWifiOnlyEnabled()).isFalse();
    }

    @Test
    public void savesOfflineContentLocation() {
        storage.setOfflineContentLocation(OfflineContentLocation.SD_CARD);
        assertThat(storage.getOfflineContentLocation()).isEqualTo(OfflineContentLocation.SD_CARD);
    }

    @Test
    public void offlineContentIsAccessibleWhenLocationIsDeviceStorage() {
        storage.setOfflineContentLocation(OfflineContentLocation.DEVICE_STORAGE);
        assertThat(storage.isOfflineContentAccessible()).isTrue();
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
    public void clearsSettingsStorage() {
        storage.setWifiOnlyEnabled(false);
        storage.clear();

        assertThat(storage.isWifiOnlyEnabled()).isTrue();
    }

    @Test
    public void savesHasSeenGoSettingsOnboarding() {
        assertFalse(storage.hasSeenOfflineSettingsOnboarding());

        storage.setOfflineSettingsOnboardingSeen();

        assertTrue(storage.hasSeenOfflineSettingsOnboarding());
    }
}
