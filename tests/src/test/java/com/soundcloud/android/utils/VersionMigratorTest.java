package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class VersionMigratorTest {

    @Mock
    SharedPreferences sharedPreferences;
    @Mock
    SharedPreferences.Editor editor;

    @Test
    public void shouldNotUpdateVersionCodeIfCurrentVersionIsZero() {
        expect(new VersionMigrator(0, sharedPreferences).migrate()).toBeFalse();
        verifyZeroInteractions(sharedPreferences);
    }

    @Test
    public void shouldNotUpdateVersionCodeIfCurrentVersionIsTheSameAsLast() {
        when(sharedPreferences.getInt(Consts.PrefKeys.VERSION_KEY, 0)).thenReturn(1);
        expect(new VersionMigrator(1, sharedPreferences).migrate()).toBeFalse();
        verify(sharedPreferences, never()).edit();
    }

    @Test
    public void shouldUpdateVersionCodeIfCurrentVersionIsGreaterThanLast() {
        when(sharedPreferences.getInt(Consts.PrefKeys.VERSION_KEY, 0)).thenReturn(1);
        when(sharedPreferences.edit()).thenReturn(editor);
        expect(new VersionMigrator(2, sharedPreferences).migrate()).toBeTrue();
        verify(editor).putInt(Consts.PrefKeys.VERSION_KEY, 2);
    }

    @Test
    public void shouldMigratePrefKeysInVersion68(){
        when(sharedPreferences.getInt(Consts.PrefKeys.VERSION_KEY, 0)).thenReturn(67);
        when(sharedPreferences.edit()).thenReturn(editor);

        when(sharedPreferences.getBoolean(Settings.ACRA_ENABLE, true)).thenReturn(false);
        when(sharedPreferences.getBoolean(Settings.CRASHLOGS, true)).thenReturn(false);

        expect(new VersionMigrator(68, sharedPreferences).migrate()).toBeTrue();
        verify(editor).putBoolean(Settings.ANALYTICS, false);
        verify(editor).putBoolean(Settings.ACRA_ENABLE, false);
        verify(editor).commit();
    }
}
