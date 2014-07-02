package com.soundcloud.android.playback.ui.progress;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TranslateXAnimatorHC extends TranslateXAnimator {

    public TranslateXAnimatorHC(final View progressView, float startX, float endX) {
        super(progressView, startX, endX);

        addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                disableHardwareLayer(progressView);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                disableHardwareLayer(progressView);
            }
        });
    }

    @Override
    public void start() {
        enableHardwareLayer(progressView);
        super.start();
    }

    private void enableHardwareLayer(View progressView) {
        progressView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void disableHardwareLayer(View progressView) {
        progressView.setLayerType(View.LAYER_TYPE_NONE, null);
    }
}
