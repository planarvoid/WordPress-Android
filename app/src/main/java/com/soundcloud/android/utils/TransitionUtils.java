package com.soundcloud.android.utils;

import com.soundcloud.android.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.transition.Transition;
import android.transition.TransitionInflater;

@TargetApi(19)
@SuppressLint("NewApi")
public class TransitionUtils {

    public static Transition createAutoTransition(Context context) {
        return TransitionInflater.from(context).inflateTransition(R.transition.auto_transition);
    }

    public static boolean transitionsSupported(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
}
