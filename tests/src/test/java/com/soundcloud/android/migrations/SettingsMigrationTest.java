package com.soundcloud.android.migrations;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;

    @Before
    public void setUp(){
        when(sharedPreferences.edit()).thenReturn(editor);
    }

    @Test
    public void shouldNotPerformMigrationIfPreviousAppIsInstalledForTheFirstTime() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(-1);
        new SettingsMigration(0, sharedPreferences).migrate();
        verify(editor, never()).putBoolean(anyString(), anyBoolean());
    }

    @Test
    public void shouldPerformMigrationIfPreviousAppVersionIsSmallerThanTheMigrationVersion() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(67);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(sharedPreferences.getBoolean("acra.enable", true)).thenReturn(true);
        when(sharedPreferences.getBoolean("crashlogs", true)).thenReturn(false);
        new SettingsMigration(0, sharedPreferences).migrate();
        verify(editor).putBoolean("analytics_enabled", true);
        verify(editor).putBoolean("acra.enable", false);
    }

    @Test
    public void shouldNotPerformMigrationIfPreviousAppVersionIsEqualToTheMigrationVersion() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(68);
        new SettingsMigration(0, sharedPreferences).migrate();
        verify(editor, never()).putBoolean(anyString(), anyBoolean());
    }

    @Test
    public void shouldNotPerformMigrationIfPreviousAppVersionIsLargerThanTheMigrationVersion() {
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(69);
        new SettingsMigration(0, sharedPreferences).migrate();
        verify(editor, never()).putBoolean(anyString(), anyBoolean());
    }

    @Test
    public void shouldUpdateVersionCodeToLatestVersionInSharedPreferencesWhenMigrationNotPerformed(){
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(-1);
        new SettingsMigration(11, sharedPreferences).migrate();
        verify(editor).putInt("changeLogVersionCode", 11);
        verify(editor).commit();
    }

    @Test
    public void shouldUpdateVersionCodeToLatestVersionInSharedPreferencesWhenMigrationPerformed(){
        when(sharedPreferences.getInt("changeLogVersionCode", -1)).thenReturn(66);
        new SettingsMigration(11, sharedPreferences).migrate();
        verify(editor).putInt("changeLogVersionCode", 11);
        verify(editor, times(2)).commit();
    }


}
