package com.soundcloud.android.offline;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class OfflineSettingsStorageTest {

    private OfflineSettingsStorage storage;

    private TestObserver<Boolean> testObserver = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        SharedPreferences prefs = new ScTestSharedPreferences();
        storage = new OfflineSettingsStorage(prefs);
    }

    @Test
    public void savesOfflineLikesStatus() {
        storage.setLikesOfflineSync(true);
        expect(storage.isLikesOfflineSyncEnabled()).toBeTrue();
    }

    @Test
    public void receivesUpdatesToLikeStatusChanges() {
        storage.getLikesOfflineSyncChanged().subscribe(testObserver);
        storage.setLikesOfflineSync(true);
        expect(testObserver.getOnNextEvents().get(0)).toBeTrue();
    }

    @Test
    public void clearsSettingsStorage() {
        storage.setLikesOfflineSync(true);
        storage.clear();
        expect(storage.isLikesOfflineSyncEnabled()).toBeFalse();
    }

}