package com.soundcloud.android.utils;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import javax.inject.Inject;

public class KeyboardHelper {

    private final InputMethodManager inputMethodManager;

    @Inject
    public KeyboardHelper(Context context) {
        this.inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void show(Window currentWindow, View view) {
        currentWindow.setSoftInputMode(SOFT_INPUT_ADJUST_NOTHING | SOFT_INPUT_STATE_VISIBLE);
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public void hide(Window currentWindow, View view) {
        currentWindow.setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
