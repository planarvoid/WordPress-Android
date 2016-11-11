package com.soundcloud.android.playback.ui;

import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;

class ScaleAnimationSubscriber extends DefaultSubscriber<PlayQueueEvent> {

    private static final long ANIMATION_DURATION = 600;

    private final View view;

    public ScaleAnimationSubscriber(View view) {
        this.view = view;
    }

    @Override
    public void onNext(PlayQueueEvent event) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        float pivotY = rect.bottom - view.getTop();
        view.setPivotY(pivotY);
        Animator scaleXAnimator = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 1.1f, 1.0f);
        Animator scaleYAnimator = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 1.1f, 1.0f);
        AnimatorSet scaleAnimator = new AnimatorSet();
        scaleAnimator.setDuration(ANIMATION_DURATION);
        scaleAnimator.setInterpolator(new FastOutSlowInInterpolator());
        scaleAnimator.playTogether(scaleXAnimator, scaleYAnimator);
        scaleAnimator.start();
    }

}
