package com.soundcloud.android.introductoryoverlay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayOperations.OnIntroductoryOverlayStateChangedListener;
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
    public void setOverlayShownShouldForwardTheValueToThePreferencesGivenTheKey() {
        boolean shown = false;

        introductoryOverlayOperations.setOverlayShown(fakeOverlayKey, shown);

        verify(sharedPreferencesEditor).putBoolean(fakeOverlayKey, shown);
    }

    @Test
    public void registeredListenerIsNotifiedWhenStateChangesForKey() {
        OnIntroductoryOverlayStateChangedListener listener = mock(OnIntroductoryOverlayStateChangedListener.class);
        introductoryOverlayOperations.registerOnStateChangedListener(listener);

        introductoryOverlayOperations.onSharedPreferenceChanged(sharedPreferences, fakeOverlayKey);

        verify(listener).onIntroductoryOverlayStateChanged(fakeOverlayKey);
    }

    @Test
    public void unregisteredListenerIsNotNotifiedWhenStateChangesForKey() {
        OnIntroductoryOverlayStateChangedListener listener = mock(OnIntroductoryOverlayStateChangedListener.class);
        introductoryOverlayOperations.registerOnStateChangedListener(listener);
        introductoryOverlayOperations.unregisterOnStateChangedListener(listener);

        introductoryOverlayOperations.onSharedPreferenceChanged(sharedPreferences, fakeOverlayKey);

        verify(listener, never()).onIntroductoryOverlayStateChanged(fakeOverlayKey);
    }

    @Test
    public void wasOverlayShownShouldReturnFalseIfNothingWasSetBefore() {
        assertThat(introductoryOverlayOperations.wasOverlayShown(fakeOverlayKey)).isFalse();
    }
}