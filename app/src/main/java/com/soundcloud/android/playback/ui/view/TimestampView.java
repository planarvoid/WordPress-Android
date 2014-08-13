package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.ui.progress.ScrubController.OnScrubListener;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.google.common.annotations.VisibleForTesting;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.utils.ScTextUtils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class TimestampView extends LinearLayout implements ProgressAware, OnScrubListener {

    private static final long SCRUB_TRANSITION_DURATION = 120l;
    private static final float SCRUB_SCALE = 2.2f;
    private static final double SPRING_TENSION = 110.0;
    private static final double SPRING_FRICTION = 10.0;

    private final View timestampLayout;
    private final View timestampHolderHolder;
    private final TextView progressText;
    private final TextView durationText;
    private final View background;
    private final SpringSystem springSystem;
    private final float waveformBaseline;
    private final float timestampOriginalHeight;

    private boolean isScrubbing;
    private long duration;
    private int animatePercentage;
    private AnimatorSet timestampAnimator;
    private Spring springY;

    private SimpleSpringListener springListener = new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
            float value = (float) spring.getCurrentValue();
            ViewHelper.setTranslationY(timestampLayout, value * getTimestampScrubY());
            ViewHelper.setScaleX(timestampLayout, value * SCRUB_SCALE);
            ViewHelper.setScaleY(timestampLayout, value * SCRUB_SCALE);
            invalidate();
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
        timestampHolderHolder = findViewById(R.id.timestamp_holder);

        animatePercentage = getResources().getInteger(R.integer.timestamp_animate_percentage);
        waveformBaseline = getResources().getDimension(R.dimen.waveform_baseline);
        timestampOriginalHeight = getResources().getDimension(R.dimen.timestamp_height);
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
        invalidate(); // TODO: Selectively invalidate the final animated position
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        isScrubbing = newScrubState == SCRUB_STATE_SCRUBBING;
        clearAnimations();

        if (isScrubbing) {
            animateToScrubMode();
        } else if (ViewHelper.getTranslationY(timestampLayout) != 0) {
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
        springY.setCurrentValue(ViewHelper.getTranslationY(timestampLayout) / getTimestampScrubY());
        springY.setEndValue(1);
        ViewHelper.setAlpha(background, 0);
    }

    private int getTimestampScrubY() {
        final double holderTopToTarget = timestampHolderHolder.getTop() - getHeight() * (animatePercentage / 100f);
        return (int) -(holderTopToTarget + waveformBaseline - timestampOriginalHeight);
    }

    private void animateFromScrubMode() {
        timestampAnimator = new AnimatorSet();

        final ObjectAnimator translationY = ObjectAnimator.ofFloat(timestampLayout, "translationY", ViewHelper.getTranslationY(timestampLayout), 0);
        timestampAnimator.playTogether(
                translationY,
                ObjectAnimator.ofFloat(timestampLayout, "scaleX", ViewHelper.getScaleX(timestampLayout), 1),
                ObjectAnimator.ofFloat(timestampLayout, "scaleY", ViewHelper.getScaleY(timestampLayout), 1),
                ObjectAnimator.ofFloat(background, "alpha", ViewHelper.getAlpha(background), 1)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            configureHardwareAnimation(timestampAnimator);
        }
        timestampAnimator.setDuration(SCRUB_TRANSITION_DURATION);
        timestampAnimator.start();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
