package com.soundcloud.android.utils;

import com.soundcloud.android.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class AnimUtils {

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

    public static Animation runFadeInAnimationOn(Context ctx, View target) {
        Animation animation = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in);
        target.startAnimation(animation);
        return animation;
    }

    public static Animation runFadeOutAnimationOn(Context ctx, View target) {
        Animation animation = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_out);
        target.startAnimation(animation);
        return animation;
    }

    public static Animation runSpinClockwiseAnimationOn(Context ctx, View target) {
        Animation animation = AnimationUtils.loadAnimation(ctx, R.anim.spin_clockwise);
        target.startAnimation(animation);
        return animation;
    }

    public static void hideViews(Iterable<View> views) {
        for (View view : views){
            hideView(view.getContext(), view, true);
        }
    }

    public static void showViews(Iterable<View> views) {
        for (View view : views) {
            showView(view.getContext(), view, true);
        }
    }

    public static void hideView(Context context, final View view, boolean animated) {
        hideView(context, view, View.GONE, animated);
    }

    public static void hideView(Context context, final View view, final int hiddenVisibility, boolean animated) {
        view.clearAnimation();

        if (view.getVisibility() == hiddenVisibility) {
            return;
        }

        if (!animated) {
            view.setVisibility(hiddenVisibility);
        } else {
            hideView(context, view, new SimpleAnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (animation == view.getAnimation()) {
                        view.setVisibility(hiddenVisibility);
                    }
                }
            });
        }
    }

    public static void hideView(Context context, final View view, Animation.AnimationListener listener) {
        view.clearAnimation();
        if (view.getVisibility() == View.GONE) {
            return;
        }
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        animation.setAnimationListener(listener);
        view.startAnimation(animation);
    }

    public static void showView(Context context, final View view, boolean animated) {
        view.clearAnimation();
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
            if (animated) {
                view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
            }
        }
    }

    public static void removeItemFromList(ListView listView, final int position, final ItemRemovalCallback callback) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (position >= firstListItemPosition && position <= lastListItemPosition) {
            final int childIndex = position - listView.getFirstVisiblePosition() + listView.getHeaderViewsCount();
            final View removeView = listView.getChildAt(childIndex);
            removeItemAnimated(removeView, position, callback);
        } else {
            callback.onAnimationComplete(position);
        }
    }

    private static void removeItemAnimated(final View removeView, final int position, final ItemRemovalCallback callback) {
        final ViewGroup.LayoutParams removeParams = removeView.getLayoutParams();
        final int startHeight = removeView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(startHeight, 0)
                .setDuration(removeView.getResources().getInteger(android.R.integer.config_shortAnimTime));
        ViewCompat.setHasTransientState(removeView, true);
        removeView.setAlpha(0);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                callback.onAnimationComplete(position);
                removeParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                removeView.setLayoutParams(removeParams);
                removeView.setAlpha(1);
                ViewCompat.setHasTransientState(removeView, false);
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                removeParams.height = (Integer) valueAnimator.getAnimatedValue();
                removeView.setLayoutParams(removeParams);
            }
        });
        animator.start();
    }

    public static interface ItemRemovalCallback {
        void onAnimationComplete(int position);
    }

    /*
     * Really, really do not use this! It's only here to avoid changing behaviour that was added accidentally when
     * we were using NineOldAndroids. :'(
     */
    public static void clearAllAnimations() {
        try {
            Class clazz = ValueAnimator.class;
            Method method = clazz.getMethod("clearAllAnimations");
            method.invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // Nothing
        }
    }

}
