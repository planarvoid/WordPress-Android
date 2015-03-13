package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

@RunWith(SoundCloudTestRunner.class)
public class OfflineSettingsStorageTest {

    private OfflineSettingsStorage storage;

    private TestObserver<Boolean> testObserver = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        storage = new OfflineSettingsStorage(new ScTestSharedPreferences());
    }

    @Test
    public void savesOfflineLikesStatus() {
        storage.setOfflineLikesEnabled(true);
        expect(storage.isOfflineLikedTracksEnabled()).toBeTrue();
    }

    @Test
    public void receivesUpdatesToLikeStatusChanges() {
        storage.getOfflineLikesChanged().subscribe(testObserver);
        storage.setOfflineLikesEnabled(true);
        expect(testObserver.getOnNextEvents().get(0)).toBeTrue();
    }

    @Test
    public void savesWifiOnlyFlag() {
        storage.setWifiOnlyEnabled(false);
        expect(storage.isWifiOnlyEnabled()).toBeFalse();
    }

    @Test
    public void clearsSettingsStorage() {
        storage.setOfflineLikesEnabled(true);
        storage.clear();

        expect(storage.isOfflineLikedTracksEnabled()).toBeFalse();
        expect(storage.isWifiOnlyEnabled()).toBeTrue();
    }

}