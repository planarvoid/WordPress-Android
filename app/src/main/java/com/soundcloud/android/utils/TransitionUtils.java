package com.soundcloud.android.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.transition.ChangeBounds;
import android.view.Window;
import android.view.animation.Interpolator;

@TargetApi(21)
@SuppressLint("NewApi")
public class TransitionUtils {

    public static int ENTER_DURATION = 500;
    public static int EXIT_DURATION = 200;

    public static void setChangeBoundsEnterTransition(Window window, int duration, Interpolator interpolator) {
        if (transitionsSupported()) {
            window.setSharedElementEnterTransition(createChangeBoundsTransition(duration, interpolator));
        }
    }

    public static void setChangeBoundsExitTransition(Window window, int duration, Interpolator interpolator) {
        if (transitionsSupported()) {
            window.setSharedElementEnterTransition(createChangeBoundsTransition(duration, interpolator));
        }
    }

    @NonNull
    private static ChangeBounds createChangeBoundsTransition(int duration, Interpolator interpolator) {
        ChangeBounds bounds = new ChangeBounds();
        bounds.setInterpolator(interpolator);
        bounds.setDuration(duration);
        return bounds;
    }

    public static boolean transitionsSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
