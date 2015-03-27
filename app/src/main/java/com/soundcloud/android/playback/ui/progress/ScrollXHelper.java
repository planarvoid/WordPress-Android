package com.soundcloud.android.playback.ui.progress;

import org.jetbrains.annotations.Nullable;

import android.view.View;

public class ScrollXHelper extends ProgressHelper {

    public ScrollXHelper(int startPosition, int endPosition) {
        super(startPosition, endPosition);
    }

    @Override
    public void setValue(View progressView, float value) {
        progressView.setScrollX((int) value);
    }

    @Nullable
    @Override
    public ProgressAnimator createAnimator(View progressView, float startProportion) {
        return new ScrollXAnimator(progressView, getValueFromProportion(startProportion), getEndPosition());
    }
}
