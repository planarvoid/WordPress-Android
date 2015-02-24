package com.soundcloud.android.utils;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.wifi.WifiManager;

@RunWith(SoundCloudTestRunner.class)
public class LockUtilTest {

    private LockUtil lockUtil;

    @Mock private WifiManager wifiManager;
    @Mock private WifiManager.WifiLock wifiLock;
    @Mock private PowerManagerWrapper powerManager;
    @Mock private PowerManagerWakeLockWrapper wakeLock;

    @Before
    public void setUp() throws Exception {
        when(powerManager.newPartialWakeLock(anyString())).thenReturn(wakeLock);
        when(wifiManager.createWifiLock(eq(WifiManager.WIFI_MODE_FULL), anyString())).thenReturn(wifiLock);

        lockUtil = new LockUtil(wifiManager, powerManager);
    }

    @Test
    public void lockDoesNotAcquireWifiLockIfAlreadyHeld() throws Exception {
        when(wifiLock.isHeld()).thenReturn(true);

        lockUtil.lock();

        verify(wifiLock, never()).acquire();
    }

    @Test
    public void lockDoesNotAcquireWakeLockIfAlreadyHeld() throws Exception {
        when(wakeLock.isHeld()).thenReturn(true);

        lockUtil.lock();

        verify(wakeLock, never()).acquire();
    }

    @Test
    public void lockAcquiresWifiLock() throws Exception {
        lockUtil.lock();

        verify(wifiLock).acquire();
    }

    @Test
    public void lockAcquiresWakeLock() throws Exception {
        lockUtil.lock();

        verify(wakeLock).acquire();
    }

    @Test
    public void unlockDoesNotReleaseWifiLockIfWifiLockNotHeld() throws Exception {
        lockUtil.unlock();

        verify(wifiLock, never()).release();
    }

    @Test
    public void unlockDoesNotReleasesWakeLockIfWakeLockNotHeld() throws Exception {
        lockUtil.unlock();

        verify(wakeLock, never()).release();
    }

    @Test
    public void unlockReleasesWifiLock() throws Exception {
        when(wifiLock.isHeld()).thenReturn(true);

        lockUtil.unlock();

        verify(wifiLock).release();
    }

    @Test
    public void unlockReleasesWakeLock() throws Exception {
        when(wakeLock.isHeld()).thenReturn(true);

        lockUtil.unlock();

        verify(wakeLock).release();
    }
}