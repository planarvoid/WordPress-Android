package com.soundcloud.android.migrations;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SharedPreferences;

public class SettingsMigrationTest {

    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;

    @Before
    public void setUp(){
        initMocks(this);
    }

    @Test
    public void shouldPerformMigrationIfPreviousAppVersionIsSmallerThanTheMigrationVersion() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(67);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(sharedPreferences.getBoolean("acra.enable", true)).thenReturn(true);
        when(sharedPreferences.getBoolean("crashlogs", true)).thenReturn(false);
        new SettingsMigration(sharedPreferences).applyMigration();
        verify(editor).putBoolean("analytics_enabled", true);
        verify(editor).putBoolean("acra.enable", false);
        verify(editor).commit();
    }

    @Test
    public void shouldReturnCorrectAppVersionCodeMigrationIsApplicableTo(){
        expect(new SettingsMigration(sharedPreferences).getApplicableAppVersionCode()).toEqual(68);
    }


}
