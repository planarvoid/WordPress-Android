package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

@RunWith(SoundCloudTestRunner.class)
public class DeviceHelperTest {

    public static final String PACKAGE_NAME = "package-name";
    private DeviceHelper deviceHelper;

    @Mock
    Context context;
    @Mock
    ContentResolver contentResolver;
    @Mock
    TelephonyManager telephonyManager;
    @Mock
    PackageManager packageManager;

    PackageInfo packageInfo;

    @Before
    public void setUp() throws Exception {
        packageInfo = new PackageInfo();
        packageInfo.versionCode = 66;
        packageInfo.versionName = "1.2.3";

        deviceHelper = new DeviceHelper(context);
        when(context.getContentResolver()).thenReturn(contentResolver);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_META_DATA)).thenReturn(packageInfo);
    }

    @After
    public void tearDown() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.class, "MANUFACTURER", null);
        Robolectric.Reflection.setFinalStaticField(Build.class, "MODEL", null);
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "RELEASE", null);
    }

    @Test
    public void getUniqueDeviceIdReturnsDeviceIdFromTelephonyManager() throws Exception {
        when(context.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(telephonyManager);
        when(telephonyManager.getDeviceId()).thenReturn("MYID");
        expect(deviceHelper.getUniqueDeviceID()).toEqual("04ddf8a23b64c654b938b95a50a486f0");
    }

    @Test
    public void shouldGetUniqueDeviceIdWithoutTelephonyManager() throws Exception {
        Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, "foobar");
        expect(deviceHelper.getUniqueDeviceID()).toEqual("3858f62230ac3c915f300c664312c63f");
    }

    @Test
    public void getDeviceNameReturnsManufacturerAndModelIfModelDoesNotContainsManufacturer(){
        Robolectric.Reflection.setFinalStaticField(Build.class, "MODEL", "GT-I9082");
        Robolectric.Reflection.setFinalStaticField(Build.class, "MANUFACTURER", "Samsung");
        expect(deviceHelper.getDeviceName()).toEqual("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsModelOnlyIfModelContainsManufacturer(){
        Robolectric.Reflection.setFinalStaticField(Build.class, "MODEL", "Samsung GT-I9082");
        Robolectric.Reflection.setFinalStaticField(Build.class, "MANUFACTURER", "Samsung");
        expect(deviceHelper.getDeviceName()).toEqual("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsManufacturerOnlyIfNoModel(){
        Robolectric.Reflection.setFinalStaticField(Build.class, "MANUFACTURER", "Samsung");
        expect(deviceHelper.getDeviceName()).toEqual("Samsung");
    }

    @Test
    public void getDeviceNameReturnsModelOnlyIfNoManufacturer(){
        Robolectric.Reflection.setFinalStaticField(Build.class, "MODEL", "Samsung GT-I9082");
        expect(deviceHelper.getDeviceName()).toEqual("Samsung GT-I9082");
    }

    @Test
    public void getDeviceNameReturnsDefaultNameWithNoManufacturerOrModel(){
        expect(deviceHelper.getDeviceName()).toEqual("unknown device");
    }

    @Test
    public void getPackageNameReturnsPackageNameFromContext(){
        expect(deviceHelper.getPackageName()).toEqual(PACKAGE_NAME);
    }

    @Test
    public void getAppVersionReturnsAppVersionFromPackageManager() throws Exception {
        expect(deviceHelper.getAppVersion()).toEqual("1.2.3");
    }

    @Test
    public void getAppVersionCodeReturnsAppVersionCodeFromPackageManager() throws Exception {
        expect(deviceHelper.getAppVersionCode()).toEqual(66);
    }

    @Test
    public void getAppBuildModelIdentifierReturnsCompleteAppBuildModel() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "RELEASE", "4.1.1");
        Robolectric.Reflection.setFinalStaticField(Build.class, "MODEL", "Samsung GT-I9082");
        Robolectric.Reflection.setFinalStaticField(Build.class, "MANUFACTURER", "Samsung");
        expect(deviceHelper.getUserAgent()).toEqual("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
    }

}