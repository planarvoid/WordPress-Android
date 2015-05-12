package com.soundcloud.android.utils;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LockUtilTest {

    private LockUtil lockUtil;

    @Mock private PowerManagerWrapper powerManager;
    @Mock private PowerManagerWakeLockWrapper wakeLock;

    @Before
    public void setUp() throws Exception {
        when(powerManager.newPartialWakeLock(anyString())).thenReturn(wakeLock);

        lockUtil = new LockUtil(powerManager);
    }

    @Test
    public void lockDoesNotAcquireWakeLockIfAlreadyHeld() throws Exception {
        when(wakeLock.isHeld()).thenReturn(true);

        lockUtil.lock();

        verify(wakeLock, never()).acquire();
    }

    @Test
    public void lockAcquiresWakeLock() throws Exception {
        lockUtil.lock();

        verify(wakeLock).acquire();
    }

    @Test
    public void unlockDoesNotReleasesWakeLockIfWakeLockNotHeld() throws Exception {
        lockUtil.unlock();

        verify(wakeLock, never()).release();
    }

    @Test
    public void unlockReleasesWakeLock() throws Exception {
        when(wakeLock.isHeld()).thenReturn(true);

        lockUtil.unlock();

        verify(wakeLock).release();
    }
}