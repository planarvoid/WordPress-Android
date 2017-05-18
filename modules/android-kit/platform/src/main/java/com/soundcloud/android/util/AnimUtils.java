package com.soundcloud.android.util;

import com.soundcloud.androidkit.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public static Animation runFadeInAnimationOn(View view) {
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_in);
        view.startAnimation(animation);
        return animation;
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

    public static void removeItemFromList(ListView listView, int position, ItemRemovalCallback callback) {
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

    private static void removeItemAnimated(final View removeView, final int position,
                                           final ItemRemovalCallback callback) {
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

    public interface ItemRemovalCallback {
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
        } catch (NoSuchMethodException e) {
            logFailureToClearAnimations();
        } catch (InvocationTargetException e) {
            logFailureToClearAnimations();
        } catch (IllegalAccessException e) {
            logFailureToClearAnimations();
        }
    }

    private static void logFailureToClearAnimations() {
        Log.e(TAG, "Failed to call ValueAnimator.clearAllAnimations() - maybe this method is changed/removed");
    }

}
