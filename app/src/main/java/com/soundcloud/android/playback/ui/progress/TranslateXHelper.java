package com.soundcloud.android.playback.ui.progress;

import com.nineoldandroids.view.ViewHelper;

import android.os.Build;
import android.view.View;

public class TranslateXHelper extends ProgressHelper {

    public TranslateXHelper(int startX, int endX) {
        super(startX, endX);
    }

    @Override
    public void setValueFromProportion(View progressView, float proportion) {
        ViewHelper.setTranslationX(progressView, getValueFromProportion(proportion));
    }

    @Override
    public ProgressAnimator createAnimator(View progressView, float startProportion) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new TranslateXAnimatorHC(progressView, getValueFromProportion(startProportion), getEndPosition());
        } else {
            return new TranslateXAnimator(progressView, getValueFromProportion(startProportion), getEndPosition());
        }
    }
}
