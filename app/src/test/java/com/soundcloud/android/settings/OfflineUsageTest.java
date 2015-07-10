package com.soundcloud.android.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.SecureFileStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OfflineUsageTest {

    private static final long GB = 1024l * 1024l * 1024l;

    private OfflineUsage offlineUsage;

    @Mock private SecureFileStorage fileStorage;
    @Mock private OfflineSettingsStorage offlineSettings;

    @Before
    public void setup() {
        when(fileStorage.getStorageCapacity()).thenReturn(10 * GB);
        when(fileStorage.getStorageAvailable()).thenReturn(4 * GB);
        when(fileStorage.getStorageUsed()).thenReturn(1 * GB);

        when(offlineSettings.hasStorageLimit()).thenReturn(true);
        when(offlineSettings.getStorageLimit()).thenReturn(7 * GB);

        offlineUsage = new OfflineUsage(fileStorage, offlineSettings);
        offlineUsage.update();
    }

    @Test
    public void returnsCurrentUsableStorageLimit() {
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(5 * GB);
    }

    @Test
    public void returnsCurrentSelectedStorageLimit() {
        assertThat(offlineUsage.getActualOfflineLimit()).isEqualTo(7 * GB);
    }

    @Test
    public void returnsAvailableOfflineSpaceWhenUnlimited() {
        when(offlineSettings.hasStorageLimit()).thenReturn(false);

        offlineUsage.update();

        assertThat(offlineUsage.getActualOfflineLimit()).isEqualTo(5 * GB);
    }

    @Test
    public void returnsSpaceUsed() {
        assertThat(offlineUsage.getOfflineUsed()).isEqualTo(1 * GB);
    }

    @Test
    public void returnsSpaceAvailable() {
        assertThat(offlineUsage.getDeviceAvailable()).isEqualTo(4 * GB);
    }

    @Test
    public void returnsTotalDeviceSpace() {
        assertThat(offlineUsage.getDeviceTotal()).isEqualTo(10 * GB);
    }

    @Test
    public void returnsSpaceUsedByOtherApps() {
        assertThat(offlineUsage.getUsedOthers()).isEqualTo(5 * GB);
    }

    @Test
    public void returnsOfflineAvailable() {
        assertThat(offlineUsage.getOfflineAvailable()).isEqualTo(4 * GB);
    }

    @Test
    public void returnsUnusedSpace() {
        assertThat(offlineUsage.getUnused()).isEqualTo(0 * GB);
    }

    @Test
    public void returnsZeroWhenMaximumLimit() {
        offlineUsage.setOfflineLimitPercentage(100);

        assertThat(offlineUsage.getUnused()).isEqualTo(0L);
    }

    @Test
    public void setsLimitFromPercentage() {
        offlineUsage.setOfflineLimitPercentage(50);

        assertThat(offlineUsage.getOfflineLimitPercentage()).isEqualTo(50);
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(5L * GB);
    }

    @Test
    public void roundsOfflineLimitPercentage() {
        when(fileStorage.getStorageUsed()).thenReturn(0L);
        offlineUsage.update();

        offlineUsage.setOfflineLimitPercentage(2);

        assertThat(offlineUsage.getOfflineLimitPercentage()).isEqualTo(5);
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo((long) (0.5 * GB));
    }

    @Test
    public void setsUnlimitedPercentage() {
        offlineUsage.setOfflineLimitPercentage(100);

        assertThat(offlineUsage.getOfflineLimitPercentage()).isEqualTo(100);
        assertThat(offlineUsage.isUnlimited()).isTrue();
    }

    @Test
    public void blocksSettingOfflineLimitBelowAlreadyUsedSpace() {
        final long offlineStorageUsed = (long) (0.6 * GB);
        when(fileStorage.getStorageUsed()).thenReturn(offlineStorageUsed);

        offlineUsage.update();

        boolean result = offlineUsage.setOfflineLimitPercentage(5);
        assertThat(result).isFalse();
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(offlineStorageUsed);
    }

    @Test
    public void returnsNewValuesAfterUpdate() {
        when(fileStorage.getStorageAvailable()).thenReturn(1000l);
        when(fileStorage.getStorageUsed()).thenReturn(2000l);
        when(offlineSettings.getStorageLimit()).thenReturn(3000l);
        when(fileStorage.getStorageCapacity()).thenReturn(4000l);

        offlineUsage.update();

        assertThat(offlineUsage.getDeviceAvailable()).isEqualTo(1000l);
        assertThat(offlineUsage.getOfflineUsed()).isEqualTo(2000l);
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(3000l);
        assertThat(offlineUsage.getDeviceTotal()).isEqualTo(4000l);
    }

    @Test
    public void setsOfflineTotalToMaxAvailable() {
        when(fileStorage.getStorageAvailable()).thenReturn(500l);
        when(fileStorage.getStorageUsed()).thenReturn(2000l);
        when(offlineSettings.getStorageLimit()).thenReturn(4000l);
        when(fileStorage.getStorageCapacity()).thenReturn(3500l);

        offlineUsage.update();

        assertThat(offlineUsage.getDeviceAvailable()).isEqualTo(500l);
        assertThat(offlineUsage.getOfflineUsed()).isEqualTo(2000l);
        assertThat(offlineUsage.getDeviceTotal()).isEqualTo(3500l);
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(2500l);
    }

}
