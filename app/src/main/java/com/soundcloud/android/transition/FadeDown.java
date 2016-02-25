package com.soundcloud.android.transition;

import com.soundcloud.android.utils.ViewUtils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FadeDown extends Visibility {

    private static final int OFFSET_DP = 48;

    private static final String ALPHA = "fadeDown:alpha";
    private static final String TRANSLATION_Y = "fadeDown:translationY";

    private static final String[] transitionProperties = {
            ALPHA,
            TRANSLATION_Y
    };

    private final int offset;

    public FadeDown(Context context, AttributeSet attrs) {
        super(context, attrs);
        offset = -ViewUtils.dpToPx(context, OFFSET_DP);
    }

    @Override
    public String[] getTransitionProperties() {
        return transitionProperties;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        transitionValues.values.put(ALPHA, 0f);
        transitionValues.values.put(TRANSLATION_Y, offset);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        transitionValues.values.put(ALPHA, 1f);
        transitionValues.values.put(TRANSLATION_Y, 0f);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues, TransitionValues endValues) {
        return new NoPauseAnimator(ObjectAnimator.ofPropertyValuesHolder(
                endValues.view,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, offset, 0f)));
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                                TransitionValues endValues) {
        return new NoPauseAnimator(ObjectAnimator.ofPropertyValuesHolder(
                endValues.view,
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 0f)));
    }

}
