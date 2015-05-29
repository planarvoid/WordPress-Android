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
    private static final long GB = 1024l * 1024l * 1024l;
    private static final long TOTAL = 8l * GB;
    private static final long AVAILABLE = 5l * GB;
    private static final long OFFLINE_USED = 2l * GB;
    private static final long OFFLINE_LIMIT = 3l * GB;

    private OfflineUsage offlineUsage;

    @Mock private SecureFileStorage fileStorage;
    @Mock private OfflineSettingsStorage offlineSettings;

    @Before
    public void setup() {
        when(fileStorage.getStorageUsed()).thenReturn(OFFLINE_USED);
        when(fileStorage.getStorageAvailable()).thenReturn(AVAILABLE);
        when(fileStorage.getStorageCapacity()).thenReturn(TOTAL);
        when(offlineSettings.getStorageLimit()).thenReturn(OFFLINE_LIMIT);

        offlineUsage = new OfflineUsage(fileStorage, offlineSettings);
        offlineUsage.update();
    }

    @Test
    public void shouldReturnCurrentLimit() {
        expect(offlineUsage.getOfflineLimit()).toEqual(OFFLINE_LIMIT);
    }

    @Test
    public void shouldReturnCurrentUsage() {
        expect(offlineUsage.getOfflineUsed()).toEqual(OFFLINE_USED);
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
        expect(offlineUsage.getUsedOthers()).toEqual(TOTAL - AVAILABLE - OFFLINE_USED);
    }

    @Test
    public void shouldReturnOfflineAvailable() {
        expect(offlineUsage.getOfflineAvailable()).toEqual(OFFLINE_LIMIT - OFFLINE_USED);
    }

    @Test
    public void shouldReturnUnusedWhenExists() {
        expect(offlineUsage.getUnused()).toEqual(AVAILABLE - (OFFLINE_LIMIT - OFFLINE_USED));
    }

    @Test
    public void shouldReturnZeroWhenMaximumLimit() {
        offlineUsage.setOfflineLimitPercentage(100);

        expect(offlineUsage.getUnused()).toEqual(0L);
    }

    @Test
    public void shouldSetLimitFromPercentage() {
        offlineUsage.setOfflineLimitPercentage(50);

        expect(offlineUsage.getOfflineLimitPercentage()).toEqual(50);
        expect(offlineUsage.getOfflineLimit()).toEqual(4L * GB);
    }

    @Test
    public void shouldRoundPercentage() {
        offlineUsage.setOfflineLimitPercentage(2);

        expect(offlineUsage.getOfflineLimitPercentage()).toEqual(6);
        expect(offlineUsage.getOfflineLimit()).toEqual((long) (0.5 * GB));
    }

    @Test
    public void shouldSetMaximumLimitPercentage() {
        offlineUsage.setOfflineLimitPercentage(95);

        expect(offlineUsage.getOfflineLimitPercentage()).toEqual(100);
        expect(offlineUsage.isMaximumLimit()).toBeTrue();
    }

    @Test
    public void shouldChangeOnUpdate() {
        when(fileStorage.getStorageAvailable()).thenReturn(1000l);
        when(fileStorage.getStorageUsed()).thenReturn(2000l);
        when(offlineSettings.getStorageLimit()).thenReturn(3000l);
        when(fileStorage.getStorageCapacity()).thenReturn(4000l);
        offlineUsage.update();

        expect(offlineUsage.getDeviceAvailable()).toEqual(1000l);
        expect(offlineUsage.getOfflineUsed()).toEqual(2000l);
        expect(offlineUsage.getOfflineLimit()).toEqual(3000l);
        expect(offlineUsage.getDeviceTotal()).toEqual(4000l);
    }

    @Test
    public void shouldSetOfflineTotalToMaxAvailable() {
        when(fileStorage.getStorageAvailable()).thenReturn(500l);
        when(fileStorage.getStorageUsed()).thenReturn(2000l);
        when(offlineSettings.getStorageLimit()).thenReturn(4000l);
        when(fileStorage.getStorageCapacity()).thenReturn(3500l);
        offlineUsage.update();

        expect(offlineUsage.getDeviceAvailable()).toEqual(500l);
        expect(offlineUsage.getOfflineUsed()).toEqual(2000l);
        expect(offlineUsage.getDeviceTotal()).toEqual(3500l);
        expect(offlineUsage.getOfflineLimit()).toEqual(2500l);
    }
}
