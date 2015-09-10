package com.soundcloud.android.facebookinvites;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SharedPreferences;

public class FacebookInvitesStorageTest extends AndroidUnitTest {

    public static final String TIMES_APP_OPENED = "times_app_opened";
    public static final String TIMES_DISMISSED = "times_dismissed";
    public static final String LAST_CLICK = "last_click";
    public static final String LAST_DISMISS = "last_dismiss";

    @Mock private DateProvider dateProvider;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;

    private FacebookInvitesStorage storage;
    private long currentTime = System.currentTimeMillis();

    @Before
    public void setUp() throws Exception {
        storage = new FacebookInvitesStorage(sharedPreferences, dateProvider);
        when(dateProvider.getCurrentTime()).thenReturn(currentTime);
    }

    @Test
    public void testIncrementTimesAppOpened() throws Exception {
        when(sharedPreferences.getInt(TIMES_APP_OPENED, 0)).thenReturn(1000);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putInt(any(String.class), any(Integer.class))).thenReturn(editor);

        storage.setAppOpened();

        verify(editor).putInt(TIMES_APP_OPENED, 1001);
        verify(editor).apply();
    }

    @Test
    public void testSetClicked() throws Exception {
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putLong(any(String.class), any(Long.class))).thenReturn(editor);
        when(editor.putInt(any(String.class), any(Integer.class))).thenReturn(editor);

        storage.setClicked();

        verify(editor).putLong(LAST_CLICK, currentTime);
        verify(editor).putLong(LAST_DISMISS, 0l);
        verify(editor).putInt(TIMES_DISMISSED, 0);
        verify(editor, times(3)).apply();
    }

    @Test
    public void testSetDismissed() throws Exception {
        when(sharedPreferences.getInt(TIMES_DISMISSED, 0)).thenReturn(10);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putLong(any(String.class), any(Long.class))).thenReturn(editor);
        when(editor.putInt(any(String.class), any(Integer.class))).thenReturn(editor);

        storage.setDismissed();

        verify(editor).putLong(LAST_DISMISS, currentTime);
        verify(editor).putInt(TIMES_DISMISSED, 11);
        verify(editor, times(2)).apply();
    }

}