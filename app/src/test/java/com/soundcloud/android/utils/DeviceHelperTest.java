package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;

public class DeviceHelperTest extends AndroidUnitTest {

    private static final String PACKAGE_NAME = "package-name";
    private DeviceHelper deviceHelper;

    @Mock Context context;
    @Mock ContentResolver contentResolver;
    @Mock PackageManager packageManager;
    @Mock BuildHelper buildHelper;
    @Mock Resources resources;

    @Before
    public void setUp() throws Exception {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = 66;
        packageInfo.versionName = "1.2.3";

        deviceHelper = new DeviceHelper(context, buildHelper, resources);
        when(context.getContentResolver()).thenReturn(contentResolver);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_META_DATA)).thenReturn(packageInfo);
    }

    @Test
    public void getUdidIsDefaultsToUnknownIfCannotBeObtained() {
        deviceHelper = new DeviceHelper(context, buildHelper, resources);
        assertThat(deviceHelper.getUdid()).isEqualTo("unknown");
    }

    @Test
    public void shouldGetHashedDeviceFromAndroidId() throws Exception {
        Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, "foobar");

        deviceHelper = new DeviceHelper(context, buildHelper, resources);

        assertThat(deviceHelper.getUdid()).isEqualTo("3858f62230ac3c915f300c664312c63f");
    }

    @Test
    public void getDeviceNameReturnsManufacturerAndModelIfModelDoesNotContainsManufacturer() {
        when(buildHelper.getModel()).thenReturn("GT-I9082");
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsModelOnlyIfModelContainsManufacturer() {
        when(buildHelper.getModel()).thenReturn("Samsung GT-I9082");
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsManufacturerOnlyIfNoModel() {
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung");
    }

    @Test
    public void getDeviceNameReturnsModelOnlyIfNoManufacturer() {
        when(buildHelper.getModel()).thenReturn("Samsung GT-I9082");
        assertThat(deviceHelper.getDeviceName()).isEqualTo("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsDefaultNameWithNoManufacturerOrModel() {
        assertThat(deviceHelper.getDeviceName()).isEqualTo("unknown device");
    }

    @Test
    public void getPackageNameReturnsPackageNameFromContext() {
        assertThat(deviceHelper.getPackageName()).isEqualTo(PACKAGE_NAME);
    }

    @Test
    public void getAppBuildModelIdentifierReturnsCompleteAppBuildModel() throws Exception {
        when(buildHelper.getModel()).thenReturn("Samsung GT-I9082");
        when(buildHelper.getManufacturer()).thenReturn("Samsung");
        when(buildHelper.getAndroidReleaseVersion()).thenReturn("4.1.1");
        assertThat(deviceHelper.getUserAgent()).isEqualTo(
                "SoundCloud-Android/" + BuildConfig.VERSION_NAME + " (Android 4.1.1; Samsung GT-I9082)");
    }

    @Test
    public void getUserAgentSanitizesForAscii() {
        when(buildHelper.getModel()).thenReturn("öäü");
        when(buildHelper.getAndroidReleaseVersion()).thenReturn("4.1.1");
        assertThat(deviceHelper.getUserAgent()).isEqualTo(
                "SoundCloud-Android/" + BuildConfig.VERSION_NAME + " (Android 4.1.1; oau)");
    }

    @Test
    public void shouldReturnAndroidReleaseVersion() {
        when(buildHelper.getAndroidReleaseVersion()).thenReturn("7.0.0");
        assertThat(deviceHelper.getAndroidReleaseVersion()).isEqualTo("7.0.0");
    }


    @Test
    public void determineTabletBasedOnResources() {
        final boolean isTablet = false;

        when(resources.getBoolean(R.bool.is_tablet)).thenReturn(isTablet);

        assertThat(deviceHelper.isTablet()).isEqualTo(isTablet);
    }
}
