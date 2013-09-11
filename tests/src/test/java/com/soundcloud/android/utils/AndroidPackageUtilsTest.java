package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.AndroidUtils.AndroidPackageUtils;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

@RunWith(SoundCloudTestRunner.class)
public class AndroidPackageUtilsTest {

    private AndroidPackageUtils packageUtils;
    @Mock
    private PackageManager packageManager;

    @Before
    public void setUp(){
        packageUtils = new AndroidPackageUtils(packageManager, "packageName");
    }

    @Test
    public void shouldReturnTrueIfPackageFistInstallTimeIsEqualToPackageUpdateTime() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.firstInstallTime = 22L;
        packageInfo.lastUpdateTime = 22L;
        when(packageManager.getPackageInfo("packageName", 0)).thenReturn(packageInfo);
        expect(packageUtils.appIsInstalledForTheFirstTime()).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfPackageFistInstallTimeIsLessThanPackageUpdateTime() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.firstInstallTime = 21L;
        packageInfo.lastUpdateTime = 22L;
        when(packageManager.getPackageInfo("packageName", 0)).thenReturn(packageInfo);
        expect(packageUtils.appIsInstalledForTheFirstTime()).toBeFalse();
    }


}
