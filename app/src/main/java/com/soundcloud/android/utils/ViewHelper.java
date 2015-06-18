package com.soundcloud.android.utils;

import com.soundcloud.android.util.AnimUtils;

import android.annotation.TargetApi;
import android.graphics.Outline;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

import javax.inject.Inject;

public class ViewHelper {

    @Inject
    public ViewHelper() {
        // for injection
    }

    public void hideView(View view, int hiddenVisibility, boolean animated){
        AnimUtils.hideView(view, hiddenVisibility, animated);
    }

    public void showView(View view, boolean animated){
        AnimUtils.showView(view, animated);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setCircularButtonOutline(View view, final int dimension) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            view.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, dimension, dimension);
                }
            });
            view.setClipToOutline(true);
        }
    }
}
