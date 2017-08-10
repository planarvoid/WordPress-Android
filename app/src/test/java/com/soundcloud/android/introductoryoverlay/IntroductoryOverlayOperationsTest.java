package com.soundcloud.android.introductoryoverlay;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayOperations.OnIntroductoryOverlayStateChangedListener;
import com.soundcloud.android.utils.CurrentDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class IntroductoryOverlayOperationsTest {

    private static final String OVERLAY_KEY = IntroductoryOverlayKey.PLAY_QUEUE;
    private static final long CURRENT_TIME = 123L;

    @Mock private SharedPreferences sharedPreferences;
    @Mock private CurrentDateProvider dateProvider;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;

    private IntroductoryOverlayOperations introductoryOverlayOperations;

    @SuppressLint("CommitPrefEdits")
    @Before
    public void setUp() {
        introductoryOverlayOperations = new IntroductoryOverlayOperations(sharedPreferences, dateProvider);

        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putBoolean(anyString(), anyBoolean())).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putLong(anyString(), anyLong())).thenReturn(sharedPreferencesEditor);
        when(dateProvider.getCurrentTime()).thenReturn(CURRENT_TIME);
    }

    @Test
    public void setOverlayShownShouldSetTheTrueValueToThePreferencesGivenTheKey() {
        introductoryOverlayOperations.setOverlayShown(OVERLAY_KEY);

        verify(sharedPreferencesEditor).putBoolean(OVERLAY_KEY, true);
        verify(sharedPreferencesEditor).putLong("overlay_shown_time", CURRENT_TIME);
    }

    @Test
    public void setOverlayShownShouldForwardTheValueToThePreferencesGivenTheKey() {
        boolean shown = false;

        introductoryOverlayOperations.setOverlayShown(OVERLAY_KEY, shown);

        verify(sharedPreferencesEditor).putBoolean(OVERLAY_KEY, shown);
        verify(sharedPreferencesEditor).putLong("overlay_shown_time", CURRENT_TIME);
    }

    @Test
    public void hasDelayDurationPassedReturnsFalseIfDelayDurationHasNotPassed() {
        when(sharedPreferences.getLong(eq("overlay_shown_time"), anyLong())).thenReturn(CURRENT_TIME);

        assertThat(introductoryOverlayOperations.hasDelayDurationPassed()).isFalse();
    }

    @Test
    public void hasDelayDurationPassedReturnsTrueIfDelayDurationHasPassed() {
        when(sharedPreferences.getLong(eq("overlay_shown_time"), anyLong())).thenReturn(CURRENT_TIME);
        when(dateProvider.getCurrentTime()).thenReturn(CURRENT_TIME + TimeUnit.SECONDS.toMillis(10L));

        assertThat(introductoryOverlayOperations.hasDelayDurationPassed()).isTrue();
    }

    @Test
    public void registeredListenerIsNotifiedWhenStateChangesForKey() {
        OnIntroductoryOverlayStateChangedListener listener = mock(OnIntroductoryOverlayStateChangedListener.class);
        introductoryOverlayOperations.registerOnStateChangedListener(listener);

        introductoryOverlayOperations.onSharedPreferenceChanged(sharedPreferences, OVERLAY_KEY);

        verify(listener).onIntroductoryOverlayStateChanged(OVERLAY_KEY);
    }

    @Test
    public void unregisteredListenerIsNotNotifiedWhenStateChangesForKey() {
        OnIntroductoryOverlayStateChangedListener listener = mock(OnIntroductoryOverlayStateChangedListener.class);
        introductoryOverlayOperations.registerOnStateChangedListener(listener);
        introductoryOverlayOperations.unregisterOnStateChangedListener(listener);

        introductoryOverlayOperations.onSharedPreferenceChanged(sharedPreferences, OVERLAY_KEY);

        verify(listener, never()).onIntroductoryOverlayStateChanged(OVERLAY_KEY);
    }

    @Test
    public void wasOverlayShownShouldReturnFalseIfNothingWasSetBefore() {
        assertThat(introductoryOverlayOperations.wasOverlayShown(OVERLAY_KEY)).isFalse();
    }
}
