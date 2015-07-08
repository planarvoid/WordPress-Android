package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class DeviceHelperTest extends AndroidUnitTest {

    public static final String PACKAGE_NAME = "package-name";
    private DeviceHelper deviceHelper;

    @Mock Context context;
    @Mock ContentResolver contentResolver;
    @Mock TelephonyManager telephonyManager;
    @Mock PackageManager packageManager;
    @Mock BuildHelper buildHelper;

    PackageInfo packageInfo;

    @Before
    public void setUp() throws Exception {
        packageInfo = new PackageInfo();
        packageInfo.versionCode = 66;
        packageInfo.versionName = "1.2.3";

        deviceHelper = new DeviceHelper(context, buildHelper);
        when(context.getContentResolver()).thenReturn(contentResolver);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_META_DATA)).thenReturn(packageInfo);
    }

    @Test
    public void getUniqueDeviceIdReturnsDeviceIdFromTelephonyManager() throws Exception {
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(telephonyManager.getDeviceId()).thenReturn("MYID");

        deviceHelper = new DeviceHelper(context, buildHelper);

        assertThat(deviceHelper.getUdid()).isEqualTo("04ddf8a23b64c654b938b95a50a486f0");
    }

    @Test
    public void shouldGetUniqueDeviceIdWithoutTelephonyManager() throws Exception {
        Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, "foobar");

        deviceHelper = new DeviceHelper(context, buildHelper);

        assertThat(deviceHelper.getUdid()).isEqualTo("3858f62230ac3c915f300c664312c63f");
    }

    @Test
    public void inSplitTestGroupReturnsTrueBasedOnDeviceId() throws Exception {
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(telephonyManager.getDeviceId()).thenReturn("MYID");
        assertThat(deviceHelper.inSplitTestGroup()).isTrue();
    }

    @Test
    public void inSplitTestGroupReturnsFalseBasedOnDeviceId() throws Exception {
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(telephonyManager.getDeviceId()).thenReturn("0000000");
        assertThat(deviceHelper.inSplitTestGroup()).isFalse();
    }

    @Test
    public void getDeviceNameReturnsManufacturerAndModelIfModelDoesNotContainsManufacturer(){
        when(buildHelper.getModel()).thenReturn("GT-I9082");
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsModelOnlyIfModelContainsManufacturer(){
        when(buildHelper.getModel()).thenReturn("Samsung GT-I9082");
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsManufacturerOnlyIfNoModel(){
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung");
    }

    @Test
    public void getDeviceNameReturnsModelOnlyIfNoManufacturer(){
        when(buildHelper.getModel()).thenReturn("Samsung GT-I9082");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsDefaultNameWithNoManufacturerOrModel(){
        assertThat(deviceHelper.getDeviceName()).isEqualTo("unknown device");
    }

    @Test
    public void getPackageNameReturnsPackageNameFromContext(){
        assertThat(deviceHelper.getPackageName()).isEqualTo(PACKAGE_NAME);
    }

    @Test
    public void getAppVersionReturnsAppVersionFromPackageManager() throws Exception {
        assertThat(deviceHelper.getAppVersion()).isEqualTo("1.2.3");
    }

    @Test
    public void getAppVersionCodeReturnsAppVersionCodeFromPackageManager() throws Exception {
        assertThat(deviceHelper.getAppVersionCode()).isEqualTo(66);
    }

    @Test
    public void getAppBuildModelIdentifierReturnsCompleteAppBuildModel() throws Exception {
        when(buildHelper.getModel()).thenReturn("Samsung GT-I9082");
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        when(buildHelper.getAndroidReleaseVersion()).thenReturn("4.1.1");
        assertThat(deviceHelper.getUserAgent()).isEqualTo("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
    }

}