package com.soundcloud.android.util;

import com.soundcloud.androidkit.R;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public final class AnimUtils {

    public static final String TAG = AnimUtils.class.getSimpleName();

    private AnimUtils() {}

    public static class SimpleAnimationListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    public static Animation runSpinClockwiseAnimationOn(View view) {
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.ak_spin_clockwise);
        view.startAnimation(animation);
        return animation;
    }

    public static void hideViews(Iterable<View> views) {
        for (View view : views){
            hideView(view, true);
        }
    }

    public static void showViews(Iterable<View> views) {
        for (View view : views) {
            showView(view, true);
        }
    }

    public static void hideView(View view, boolean animated) {
        hideView(view, View.GONE, animated);
    }

    public static void hideView(final View view, final int hiddenVisibility, boolean animated) {
        view.clearAnimation();

        if (view.getVisibility() == hiddenVisibility) {
            return;
        }

        view.clearAnimation();

        if (!animated) {
            view.setVisibility(hiddenVisibility);
        } else {
            hideView(view, new SimpleAnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (animation == view.getAnimation()) {
                        view.setVisibility(hiddenVisibility);
                    }
                }
            });
        }
    }

    public static void hideView(View view, Animation.AnimationListener listener) {
        if (view.getVisibility() == View.GONE) {
            return;
        }
        view.clearAnimation();
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.ak_fade_out);
        animation.setAnimationListener(listener);
        view.startAnimation(animation);
    }

    public static void showView(View view, boolean animated) {
        if (view.getVisibility() != View.VISIBLE) {
            view.clearAnimation();
            view.setVisibility(View.VISIBLE);
            if (animated) {
                view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.ak_fade_in));
            }
        }
    }

}
