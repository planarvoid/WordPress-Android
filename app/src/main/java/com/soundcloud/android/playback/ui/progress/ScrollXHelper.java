package com.soundcloud.android.playback.ui.progress;

import com.nineoldandroids.view.ViewHelper;
import org.jetbrains.annotations.Nullable;

import android.view.View;

public class ScrollXHelper extends ProgressHelper {

    public ScrollXHelper(int startPosition, int endPosition) {
        super(startPosition, endPosition);
    }

    @Override
    public void setValue(View progressView, float value) {
        ViewHelper.setScrollX(progressView, (int) value);
    }

    @Nullable
    @Override
    public ProgressAnimator createAnimator(View progressView, float startProportion) {
        return new ScrollXAnimator(progressView, (int) getValueFromProportion(startProportion), getEndPosition());
    }
}
