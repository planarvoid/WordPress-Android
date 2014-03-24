package com.soundcloud.android.view;

import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.ViewDelegate;

import android.view.View;

public class EmptyListDelegate implements ViewDelegate {

    @Override
    public boolean isReadyForPull(View view, float x, float y) {
        return Boolean.TRUE;
    }

}
