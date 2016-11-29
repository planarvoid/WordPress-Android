package com.soundcloud.android.introductoryoverlay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

public class IntroductoryOverlayOperationsTest extends AndroidUnitTest {

    private final String fakeOverlayKey = IntroductoryOverlayKey.PLAY_QUEUE;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;

    private IntroductoryOverlayOperations introductoryOverlayOperations;

    @SuppressLint("CommitPrefEdits")
    @Before
    public void setUp() {
        introductoryOverlayOperations = new IntroductoryOverlayOperations(sharedPreferences);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putBoolean(anyString(), anyBoolean())).thenReturn(sharedPreferencesEditor);
    }

    @Test
    public void setOverlayShownShouldSetTheTrueValueToThePreferencesGivenTheKey() {
        introductoryOverlayOperations.setOverlayShown(fakeOverlayKey);

        verify(sharedPreferencesEditor).putBoolean(fakeOverlayKey, true);
    }

    @Test
    public void wasOverlayShownShouldReturnFalseIfNothingWasSetBefore() {
        assertThat(introductoryOverlayOperations.wasOverlayShown(fakeOverlayKey)).isFalse();
    }
}