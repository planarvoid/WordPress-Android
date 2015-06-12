package com.soundcloud.android.tracks;

import com.soundcloud.android.R;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Collection;

public class LoadingRelatedTracksView extends FrameLayout {

    private static final int DELAY_BETWEEN_ANIMATIONS = 50;
    private static final int DELAY_BEFORE_REPEATING_ANIMATION = 700;
    private static final int ANIMATION_DURATION_PER_LOADING_BAR = 100;
    private final float translationYOffset;
    private final View[] loadingBars;

    private AnimatorSet animators;

    public LoadingRelatedTracksView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.related_tracks_loading_icon, this, true);
        animators = new AnimatorSet();
        loadingBars = new View[]{
                findViewById(R.id.loading_bar_1),
                findViewById(R.id.loading_bar_2),
                findViewById(R.id.loading_bar_3),
                findViewById(R.id.loading_bar_4),
                findViewById(R.id.loading_bar_5)
        };
        translationYOffset = -getResources().getDimension(R.dimen.play_related_loading_transition_y_offset);
    }

    public void start() {
        animators = new AnimatorSet();
        animators.playTogether(createAnimators(loadingBars));
        animators.addListener(new InfiniteAnimationListener());
        animators.setInterpolator(new LinearInterpolator());
        animators.start();
    }

    private Collection<Animator> createAnimators(View... bars) {
        final ArrayList<Animator> animators = new ArrayList<>(bars.length);
        for (int i = 0; i < bars.length; i++) {
            animators.add(createAnimator(bars[i], i * DELAY_BETWEEN_ANIMATIONS, translationYOffset));
        }
        return animators;
    }

    private ObjectAnimator createAnimator(View view, long delayTimeMilliSeconds, float yOffset) {
        final ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 0, yOffset, 0);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(ANIMATION_DURATION_PER_LOADING_BAR);
        animator.setStartDelay(delayTimeMilliSeconds);
        return animator;
    }

    public void stop() {
        animators.cancel();
        clearAnimations(loadingBars);
    }

    private void clearAnimations(View... views) {
        for (View view : views) {
            view.clearAnimation();
        }
    }

    private static class InfiniteAnimationListener implements Animator.AnimatorListener {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            animation.setStartDelay(DELAY_BEFORE_REPEATING_ANIMATION);
            animation.start();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }
}
