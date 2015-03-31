package com.soundcloud.android.settings;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class OfflineUsageTest {
    private static final long GB = 1024 * 1024 * 1024;
    private static final long TOTAL = 64 * GB;
    private static final long AVAILABLE = 30 * GB;
    private static final long USED = 7 * GB;
    private static final long LIMIT = 16 * GB;

    private OfflineUsage offlineUsage;

    @Mock private SecureFileStorage fileStorage;
    @Mock private OfflineSettingsStorage offlineSettings;

    @Before
    public void setup() {
        when(fileStorage.getStorageUsed()).thenReturn(USED);
        when(fileStorage.getStorageAvailable()).thenReturn(AVAILABLE);
        when(fileStorage.getStorageCapacity()).thenReturn(TOTAL);
        when(offlineSettings.getStorageLimit()).thenReturn(LIMIT);

        offlineUsage = new OfflineUsage(fileStorage, offlineSettings);
        offlineUsage.update();
    }

    @Test
    public void shouldReturnCurrentLimit() {
        expect(offlineUsage.getOfflineTotal()).toEqual(LIMIT);
    }

    @Test
    public void shouldReturnCurrentUsage() {
        expect(offlineUsage.getOfflineUsed()).toEqual(USED);
    }

    @Test
    public void shouldReturnCurrentSpaceAvailable() {
        expect(offlineUsage.getDeviceAvailable()).toEqual(AVAILABLE);
    }

    @Test
    public void shouldReturnCurrentTotalSpace() {
        expect(offlineUsage.getDeviceTotal()).toEqual(TOTAL);
    }

    @Test
    public void shouldReturnUsageByOtherApps() {
        expect(offlineUsage.getUsedOthers()).toEqual(27 * GB);
    }

    @Test
    public void shouldReturnOfflineAvailable() {
        expect(offlineUsage.getOfflineAvailable()).toEqual(9 * GB);
    }

    @Test
    public void shouldReturnAvailableAfterLimit() {
        expect(offlineUsage.getAvailableWithoutOfflineLimit()).toEqual(21 * GB);
    }

    @Test
    public void shouldGetLimitFromPercentage() {
        expect(offlineUsage.getOfflineTotalPercentage()).toEqual(43);
    }

    @Test
    public void shouldSetLimitFromPercentage() {
        offlineUsage.setOfflineTotalPercentage(45);
        expect(offlineUsage.getOfflineTotal()).toEqual(17877801370l);
    }

    @Test
    public void shouldRoundPercentage() {
        offlineUsage.setOfflineTotalPercentage(45);
        expect(offlineUsage.getOfflineTotalPercentage()).toEqual(45);
    }

    @Test
    public void shouldChangeOnUpdate() {
        when(fileStorage.getStorageAvailable()).thenReturn(1000l);
        when(fileStorage.getStorageUsed()).thenReturn(2000l);
        when(fileStorage.getStorageCapacity()).thenReturn(3000l);
        when(offlineSettings.getStorageLimit()).thenReturn(4000l);

        offlineUsage.update();

        expect(offlineUsage.getDeviceAvailable()).toEqual(1000l);
        expect(offlineUsage.getOfflineUsed()).toEqual(2000l);
        expect(offlineUsage.getDeviceTotal()).toEqual(3000l);
        expect(offlineUsage.getOfflineTotal()).toEqual(4000l);
    }
}
