package com.soundcloud.android.configuration;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class DeviceManagementStorageTest {
    
    private DeviceManagementStorage deviceManagementStorage;
    
    @Mock SharedPreferences sharedPreferences;
    @Mock SharedPreferences.Editor sharedPreferencesEditor;

    @Before
    public void setUp() throws Exception {
        deviceManagementStorage = new DeviceManagementStorage(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putBoolean(anyString(), anyBoolean())).thenReturn(sharedPreferencesEditor);
    }

    @Test
    public void setDeviceConflictStoresDeviceConflictInPrefs() throws Exception {
        deviceManagementStorage.setDeviceConflict();
        verify(sharedPreferencesEditor).putBoolean(DeviceManagementStorage.DEVICE_CONFLICT, true);
    }

    @Test
    public void clearDeviceConflictClearsDeviceConflictInPrefs() throws Exception {
        deviceManagementStorage.clearDeviceConflict();
        verify(sharedPreferencesEditor).putBoolean(DeviceManagementStorage.DEVICE_CONFLICT, false);
    }

    @Test
    public void hadDeviceConflictReturnsDeviceConflictFromPrefs() throws Exception {
        when(sharedPreferences.getBoolean(DeviceManagementStorage.DEVICE_CONFLICT, false)).thenReturn(true);
        expect(deviceManagementStorage.hadDeviceConflict()).toBeTrue();
    }
}