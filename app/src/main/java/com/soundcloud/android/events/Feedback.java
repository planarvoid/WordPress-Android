package com.soundcloud.android.feedback;

import com.google.auto.value.AutoValue;

import android.support.annotation.Nullable;
import android.view.View;

import java.lang.ref.WeakReference;

@AutoValue
public abstract class Feedback {

    public static int LENGTH_SHORT = 0;

    public static int LENGTH_LONG = 1;

    public static Feedback create(int message) {
        return create(message, LENGTH_SHORT);
    }

    public static Feedback create(int message, int length) {
        return new AutoValue_Feedback(message, length, 0, null);
    }

    public static Feedback create(int message, int actionResId, View.OnClickListener onClickListener) {
        return new AutoValue_Feedback(message, LENGTH_LONG, actionResId, new WeakReference<>(onClickListener));
    }

    public abstract int getMessage();

    public abstract int getLength();

    public abstract int getActionResId();

    @Nullable
    public abstract WeakReference<View.OnClickListener> getActionListener();
}
