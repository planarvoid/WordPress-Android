package com.soundcloud.android.utils;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

@RunWith(MockitoJUnitRunner.class)
public class KeyboardHelperTest {

    private KeyboardHelper keyboardHelper;

    @Mock private Context context;
    @Mock private InputMethodManager inputMethodManager;
    @Mock private Window window;
    @Mock private View view;

    @Before
    public void setUp() {
        when(context.getSystemService(Context.INPUT_METHOD_SERVICE)).thenReturn(inputMethodManager);
        keyboardHelper = new KeyboardHelper(context);
    }

    @Test
    public void shouldShowKeyboard() {
        keyboardHelper.show(window, view);

        verify(window).setSoftInputMode(SOFT_INPUT_ADJUST_NOTHING | SOFT_INPUT_STATE_VISIBLE);
        verify(inputMethodManager).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    @Test
    public void shouldHideKeyboard() {
        final IBinder windowToken = mock(IBinder.class);
        when(view.getWindowToken()).thenReturn(windowToken);

        keyboardHelper.hide(window, view);

        verify(window).setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        verify(inputMethodManager).hideSoftInputFromWindow(windowToken, 0);
    }
}
