package com.soundcloud.android.playback.ui.progress;

import com.nineoldandroids.view.animation.AnimatorProxy;
import org.jetbrains.annotations.Nullable;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

public class ScrollXHelper extends ProgressHelper {

    public ScrollXHelper(int startPosition, int endPosition) {
        super(startPosition, endPosition);
    }

    @Override
    @TargetApi(14)
    public void setValue(View progressView, float value) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2) {
            setScrollICS(progressView, (int) value);
        } else {
            AnimatorProxy.wrap(progressView).setScrollX((int) value);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setScrollICS(View progressView, int value) {
        progressView.setScrollX(value);
    }

    @Nullable
    @Override
    public ProgressAnimator createAnimator(View progressView, float startProportion) {
        return new ScrollXAnimator(progressView, (int) getValueFromProportion(startProportion), getEndPosition());
    }
}
