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
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(OFFLINE_LIMIT);
    }

    @Test
    public void shouldReturnCurrentUsage() {
        assertThat(offlineUsage.getOfflineUsed()).isEqualTo(OFFLINE_USED);
    }

    @Test
    public void shouldReturnCurrentSpaceAvailable() {
        assertThat(offlineUsage.getDeviceAvailable()).isEqualTo(AVAILABLE);
    }

    @Test
    public void shouldReturnCurrentTotalSpace() {
        assertThat(offlineUsage.getDeviceTotal()).isEqualTo(TOTAL);
    }

    @Test
    public void shouldReturnUsageByOtherApps() {
        assertThat(offlineUsage.getUsedOthers()).isEqualTo(TOTAL - AVAILABLE - OFFLINE_USED);
    }

    @Test
    public void shouldReturnOfflineAvailable() {
        assertThat(offlineUsage.getOfflineAvailable()).isEqualTo(OFFLINE_LIMIT - OFFLINE_USED);
    }

    @Test
    public void shouldReturnUnusedWhenExists() {
        assertThat(offlineUsage.getUnused()).isEqualTo(AVAILABLE - (OFFLINE_LIMIT - OFFLINE_USED));
    }

    @Test
    public void shouldReturnZeroWhenMaximumLimit() {
        offlineUsage.setOfflineLimitPercentage(100);

        assertThat(offlineUsage.getUnused()).isEqualTo(0L);
    }

    @Test
    public void shouldSetLimitFromPercentage() {
        offlineUsage.setOfflineLimitPercentage(50);

        assertThat(offlineUsage.getOfflineLimitPercentage()).isEqualTo(50);
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(4L * GB);
    }

    @Test
    public void shouldRoundPercentage() {
        when(fileStorage.getStorageUsed()).thenReturn(0L);
        offlineUsage.update();

        offlineUsage.setOfflineLimitPercentage(2);

        assertThat(offlineUsage.getOfflineLimitPercentage()).isEqualTo(6);
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo((long) (0.5 * GB));
    }

    @Test
    public void shouldSetUnlimitedPercentage() {
        offlineUsage.setOfflineLimitPercentage(100);

        assertThat(offlineUsage.getOfflineLimitPercentage()).isEqualTo(100);
        assertThat(offlineUsage.isUnlimited()).isTrue();
    }

    @Test
    public void shouldBlockSettingOfflineLimitBelowAlreadyUsedSpace() {
        final long offlineStorageUsed = (long) (0.6 * GB);

        when(fileStorage.getStorageUsed()).thenReturn(offlineStorageUsed);
        offlineUsage.update();

        boolean result = offlineUsage.setOfflineLimitPercentage(5);

        assertThat(result).isFalse();
        assertThat(offlineUsage.getUsableOfflineLimit()).isEqualTo(offlineStorageUsed);
    }

    @Test
    public void shouldChangeOnUpdate() {
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
    public void shouldSetOfflineTotalToMaxAvailable() {
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
