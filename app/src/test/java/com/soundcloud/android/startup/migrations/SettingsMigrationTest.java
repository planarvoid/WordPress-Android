package com.soundcloud.android.startup.migrations;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class SettingsMigrationTest {

    private SettingsMigration settingsMigration;

    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;

    @Before
    public void setUp() throws Exception {
        settingsMigration = new SettingsMigration(sharedPreferences);
    }

    @Test
    public void shouldPerformMigrationIfPreviousAppVersionIsSmallerThanTheMigrationVersion() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(67);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(sharedPreferences.getBoolean("acra.enable", true)).thenReturn(true);
        when(sharedPreferences.getBoolean("crashlogs", true)).thenReturn(false);

        settingsMigration.applyMigration();

        verify(editor).putBoolean("analytics_enabled", true);
        verify(editor).putBoolean("acra.enable", false);
        verify(editor).apply();
    }

    @Test
    public void shouldReturnCorrectAppVersionCodeMigrationIsApplicableTo(){
        expect(settingsMigration.getApplicableAppVersionCode()).toEqual(68);
    }

}
