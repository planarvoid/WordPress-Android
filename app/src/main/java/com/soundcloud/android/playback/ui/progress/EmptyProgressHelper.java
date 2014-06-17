package com.soundcloud.android.playback.ui.progress;

import org.jetbrains.annotations.Nullable;

import android.view.View;

public class EmptyProgressHelper extends ProgressHelper {

    protected EmptyProgressHelper() {
        super(0, 0);
    }

    @Override
    public void setValueFromProportion(View progressView, float value) {
        // no-op
    }

    @Nullable
    @Override
    public ProgressAnimator createAnimator(View progressView, float startProportion) {
        return null;
    }
}
