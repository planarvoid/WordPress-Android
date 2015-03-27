package com.soundcloud.android.playback.ui.progress;

import android.view.View;

public class TranslateXHelper extends ProgressHelper {

    public TranslateXHelper(int startX, int endX) {
        super(startX, endX);
    }

    @Override
    public void setValue(View progressView, float value) {
        progressView.setTranslationX(value);
    }

    @Override
    public ProgressAnimator createAnimator(View progressView, float startProportion) {
        return new TranslateXAnimator(progressView, getValueFromProportion(startProportion), getEndPosition());
    }
}
