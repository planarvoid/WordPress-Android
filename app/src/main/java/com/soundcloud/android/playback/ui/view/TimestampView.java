package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.utils.ScTextUtils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class TimestampView extends LinearLayout implements ProgressAware, OnScrubListener {

    private static final long SCRUB_TRANSITION_DURATION = 120l;
    private static final float SCRUB_SCALE = 2.2f;
    private static final double SPRING_TENSION = 110.0;
    private static final double SPRING_FRICTION = 10.0;
    private static final long BUFFERING_ANIMATION_DURATION = 1800;
    private static final long BUFFERING_RESET_ANIMATION_DURATION = 300;

    private final View timestampLayout;
    private final View timestampHolder;
    private final TextView progressText;
    private final TextView durationText;
    private final View background;
    private final SpringSystem springSystem;
    private final float waveformBaseline;
    private final float timestampOriginalHeight;
    private final View dividerView;

    private boolean inBufferingMode;
    private boolean isScrubbing;
    private long duration;
    private int animatePercentage;
    private Spring springY;
    private AnimatorSet timestampAnimator;
    private AnimatorSet bufferingAnimationSet;

    private final SimpleSpringListener springListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            float value = (float) spring.getCurrentValue();
            timestampLayout.setTranslationY(value * getTimestampScrubY());
            timestampLayout.setScaleX(value * SCRUB_SCALE);
            timestampLayout.setScaleY(value * SCRUB_SCALE);
            selectiveInvalidate(true);
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    public TimestampView(Context context, AttributeSet attrs) {
        this(context, attrs, SpringSystem.create());
    }

    public TimestampView(Context context, AttributeSet attrs, SpringSystem springSystem) {
        super(context, attrs);
        this.springSystem = springSystem;

        LayoutInflater.from(context).inflate(R.layout.timestamp_layout, this, true);
        setOrientation(VERTICAL);
        setClipChildren(false);

        progressText = (TextView) findViewById(R.id.timestamp_progress);
        durationText = (TextView) findViewById(R.id.timestamp_duration);
        background = findViewById(R.id.timestamp_background);
        timestampLayout = findViewById(R.id.timestamp_layout);
        timestampHolder = findViewById(R.id.timestamp_holder);
        dividerView = findViewById(R.id.timestamp_divider);

        animatePercentage = getResources().getInteger(R.integer.timestamp_animate_percentage);
        waveformBaseline = getResources().getDimension(R.dimen.waveform_baseline);
        timestampOriginalHeight = getResources().getDimension(R.dimen.timestamp_height);

        bufferingAnimationSet = createBufferingAnimationSet();
    }

    private AnimatorSet createBufferingAnimationSet() {
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                createBufferingAnimation(progressText),
                createBufferingAnimation(durationText),
                createBufferingAnimation(dividerView)
        );
        return animatorSet;
    }

    private ValueAnimator createBufferingAnimation(View view) {
        ValueAnimator bufferingAnimation = ObjectAnimator.ofFloat(view, "alpha", 1f, .2f, 1f);
        bufferingAnimation.setDuration(BUFFERING_ANIMATION_DURATION);
        bufferingAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        bufferingAnimation.setRepeatMode(ValueAnimator.RESTART);
        bufferingAnimation.setRepeatCount(ValueAnimator.INFINITE);
        return bufferingAnimation;
    }

    public void setInitialProgress(long duration) {
        this.duration = duration;
        progressText.setText(format(0));
        durationText.setText(format(duration));
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        if (progress.isDurationValid() && progress.getDuration() != duration) {
            duration = progress.getDuration();
        }
        if (!isScrubbing) {
            progressText.setText(format(progress.getPosition()));
            durationText.setText(format(duration));
        }
    }

    public void setBufferingMode(boolean isBuffering) {
        if (isBuffering != inBufferingMode) {
            this.inBufferingMode = isBuffering;
            if (inBufferingMode) {
                bufferingAnimationSet.start();
            } else {
                bufferingAnimationSet.cancel();
                resetBufferingStates(progressText);
                resetBufferingStates(durationText);
                resetBufferingStates(dividerView);
            }
        }
    }

    private void resetBufferingStates(View view){
        ValueAnimator toOpaque = ObjectAnimator.ofFloat(view, "alpha", view.getAlpha(), 1);
        toOpaque.setDuration(BUFFERING_RESET_ANIMATION_DURATION);
        toOpaque.start();
    }

    private String format(long millis) {
        return ScTextUtils.formatTimestamp(millis, TimeUnit.MILLISECONDS);
    }

    public void showBackground(boolean visible) {
        background.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @VisibleForTesting
    public boolean isShowingBackground() {
        return background.getVisibility() == View.VISIBLE;
    }

    @Override
    public void displayScrubPosition(float scrubPosition) {
        long scrubTime = (long) (scrubPosition * duration);
        progressText.setText(format(scrubTime));
        selectiveInvalidate(false);
    }

    private void selectiveInvalidate(boolean isTransition) {
        int left = background.getLeft();
        int top = timestampLayout.getTop() + background.getTop();
        int right = background.getRight();

        int bottom;
        if (isTransition) {
            bottom = getBottom();
        } else {
            bottom = (int) (timestampLayout.getTop() + background.getBottom() + background.getHeight() * SCRUB_SCALE);
        }

        float adjustWidth = background.getWidth() * SCRUB_SCALE / 2;
        right += adjustWidth;
        left -= adjustWidth;

        invalidate(left, top, right, bottom);
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        isScrubbing = newScrubState == SCRUB_STATE_SCRUBBING;
        clearAnimations();

        if (isScrubbing) {
            animateToScrubMode();
        } else if (timestampLayout.getTranslationY() != 0 || newScrubState == SCRUB_STATE_CANCELLED) {
            animateFromScrubMode();
        }
    }

    private void clearAnimations() {
        if (timestampAnimator != null) {
            timestampAnimator.cancel();
        }
        if (springY != null) {
            springY.removeAllListeners();
            springY.destroy();
        }
    }

    private void animateToScrubMode() {
        springY = springSystem.createSpring();
        springY.addListener(springListener);
        springY.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(SPRING_TENSION, SPRING_FRICTION));
        springY.setCurrentValue(timestampLayout.getTranslationY() / getTimestampScrubY());
        springY.setEndValue(1);
        background.setAlpha(0);
    }

    private int getTimestampScrubY() {
        final double holderTopToTarget = timestampHolder.getTop() - getHeight() * (animatePercentage / 100f);
        return (int) -(holderTopToTarget + waveformBaseline - timestampOriginalHeight);
    }

    private void animateFromScrubMode() {
        timestampAnimator = new AnimatorSet();

        final ObjectAnimator translationY = ObjectAnimator.ofFloat(timestampLayout, "translationY", timestampLayout.getTranslationY(), 0);
        timestampAnimator.playTogether(
                translationY,
                ObjectAnimator.ofFloat(timestampLayout, "scaleX", timestampLayout.getScaleX(), 1),
                ObjectAnimator.ofFloat(timestampLayout, "scaleY", timestampLayout.getScaleY(), 1),
                ObjectAnimator.ofFloat(background, "alpha", background.getAlpha(), 1)
        );

        configureHardwareAnimation(timestampAnimator);
        timestampAnimator.setDuration(SCRUB_TRANSITION_DURATION);
        timestampAnimator.start();
    }

    private void configureHardwareAnimation(AnimatorSet set) {
        timestampLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                timestampLayout.setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                timestampLayout.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }
}
