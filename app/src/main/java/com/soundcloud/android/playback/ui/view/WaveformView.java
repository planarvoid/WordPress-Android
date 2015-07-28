package com.soundcloud.android.playback.ui.view;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.soundcloud.android.R;
import com.soundcloud.android.view.FixedWidthView;
import com.soundcloud.android.view.ListenableHorizontalScrollView;
import com.soundcloud.android.waveform.WaveformData;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class WaveformView extends FrameLayout {

    private static final int DEFAULT_BAR_WIDTH_DP = 2;
    private static final int DEFAULT_BAR_SPACE_DP = 1;
    private static final int DEFAULT_BASELINE_DP = 68;
    private static final float DEFAULT_WAVEFORM_WIDTH_RATIO = 1.5f;
    private static final int SCALE_DOWN_DURATION = 50;

    private static final double SPRING_TENSION = 180.0D;
    private static final double SPRING_FRICTION = 10.0D;

    private final int barWidth;
    private final int spaceWidth;
    private final int baseline;
    private final Paint progressAbovePaint;
    private final Paint progressBelowPaint;
    private final Paint unplayedAbovePaint;
    private final Paint unplayedBelowPaint;
    private final int progressColor;
    private final int unplayedColor;
    private final float waveformWidthRatio;

    private final WaveformCanvas leftWaveform;
    private final WaveformCanvas rightWaveform;
    private final ImageView leftLine;
    private final ImageView rightLine;

    private final FixedWidthView dragView;
    private final ListenableHorizontalScrollView dragViewHolder;

    private final SpringSystem springSystem = SpringSystem.create();
    private Spring springY;
    private ObjectAnimator leftWaveformScaler;
    private ObjectAnimator rightWaveformScaler;

    private OnWidthChangedListener onWidthChangedListener;

    private final Runnable layoutWaveformsRunnable = new Runnable() {
        @Override
        public void run() {
            leftWaveform.requestLayout();
            rightWaveform.requestLayout();
            leftLine.requestLayout();
            rightLine.requestLayout();
            dragView.requestLayout();
        }
    };

    public interface OnWidthChangedListener {
        void onWaveformViewWidthChanged(int newWidth);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float density = getResources().getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaveformView);
        final int progressAboveStart = a.getColor(R.styleable.WaveformView_progressAboveStart, Color.WHITE);
        final int progressAboveEnd = a.getColor(R.styleable.WaveformView_progressAboveEnd, Color.WHITE);
        final int progressBelow = a.getColor(R.styleable.WaveformView_progressBelow, Color.WHITE);
        final int unplayedAbove = a.getColor(R.styleable.WaveformView_unplayedAbove, Color.WHITE);
        final int unplayedBelow = a.getColor(R.styleable.WaveformView_unplayedBelow, Color.WHITE);
        waveformWidthRatio = a.getFloat(R.styleable.WaveformView_widthRatio, DEFAULT_WAVEFORM_WIDTH_RATIO);
        barWidth = a.getDimensionPixelSize(R.styleable.WaveformView_barWidth, (int) (DEFAULT_BAR_WIDTH_DP * density));
        spaceWidth = a.getDimensionPixelSize(R.styleable.WaveformView_spaceWidth, (int) (DEFAULT_BAR_SPACE_DP * density));
        baseline = a.getDimensionPixelSize(R.styleable.WaveformView_baseline, (int) (DEFAULT_BASELINE_DP * density));
        a.recycle();

        progressColor = progressAboveEnd;
        unplayedColor = unplayedAbove;

        progressAbovePaint = new Paint();
        progressAbovePaint.setShader(new LinearGradient(0, 0, 0, baseline, progressAboveStart,
                progressAboveEnd, Shader.TileMode.MIRROR));

        progressBelowPaint = new Paint();
        progressBelowPaint.setColor(progressBelow);

        unplayedAbovePaint = new Paint();
        unplayedAbovePaint.setColor(unplayedAbove);

        unplayedBelowPaint = new Paint();
        unplayedBelowPaint.setColor(unplayedBelow);

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.player_progress_layout, this);

        leftWaveform = (WaveformCanvas) findViewById(R.id.waveform_left);
        rightWaveform = (WaveformCanvas) findViewById(R.id.waveform_right);

        dragView = (FixedWidthView) findViewById(R.id.drag_view);
        dragViewHolder = (ListenableHorizontalScrollView) findViewById(R.id.drag_view_holder);
        dragViewHolder.setOverScrollMode(View.OVER_SCROLL_NEVER);

        leftWaveform.setScaleY(0);
        rightWaveform.setScaleY(0);

        // pivot positions for scaling animations
        leftWaveform.setPivotY(baseline);
        rightWaveform.setPivotY(baseline);

        leftLine = (ImageView) findViewById(R.id.line_left);
        rightLine = (ImageView) findViewById(R.id.line_right);

        leftLine.setImageDrawable(createLoadingDrawable(progressColor));
        rightLine.setImageDrawable(createLoadingDrawable(unplayedColor));
    }

    public void setOnWidthChangedListener(OnWidthChangedListener onWidthChangedListener) {
        this.onWidthChangedListener = onWidthChangedListener;
    }

    public ListenableHorizontalScrollView getDragViewHolder() {
        return dragViewHolder;
    }

    public float getWidthRatio() {
        return waveformWidthRatio;
    }

    WaveformCanvas getLeftWaveform() {
        return leftWaveform;
    }

    WaveformCanvas getRightWaveform() {
        return rightWaveform;
    }

    ImageView getLeftLine() {
        return leftLine;
    }

    ImageView getRightLine() {
        return rightLine;
    }

    void setWaveformTranslations(int leftTranslation, int rightTranslation) {
        leftWaveform.setTranslationX(leftTranslation);
        rightWaveform.setTranslationX(rightTranslation);
        leftLine.setTranslationX(leftTranslation);
        rightLine.setTranslationX(rightTranslation);
    }

    void showExpandedWaveform() {
        clearScaleAnimations();

        springY = springSystem.createSpring();
        springY.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float value = (float) spring.getCurrentValue();
                rightWaveform.setScaleY(value);
                leftWaveform.setScaleY(value);
            }
        });
        springY.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(SPRING_TENSION, SPRING_FRICTION));
        springY.setCurrentValue(leftWaveform.getScaleY());
        springY.setEndValue(1);

        hideIdleLines();
    }

    private void hideIdleLines() {
        // clear animations do clear transformation logic from nine-old-androids on pre HC
        leftLine.clearAnimation();
        rightLine.clearAnimation();
        leftLine.setVisibility(View.GONE);
        rightLine.setVisibility(View.GONE);
    }

    void showCollapsedWaveform() {
        clearScaleAnimations();

        leftWaveformScaler = createScaleDownAnimator(leftWaveform);
        leftWaveformScaler.start();

        rightWaveformScaler = createScaleDownAnimator(rightWaveform);
        rightWaveformScaler.start();
    }

    void showIdleLinesAtWaveformPositions() {
        leftLine.setTranslationX(leftWaveform.getTranslationX());
        rightLine.setTranslationX(rightWaveform.getTranslationX());

        leftLine.setVisibility(View.VISIBLE);
        rightLine.setVisibility(View.VISIBLE);
    }

    private void clearScaleAnimations() {
        if (springY != null) {
            springY.removeAllListeners();
            springY.destroy();
        }

        if (leftWaveformScaler != null) {
            leftWaveformScaler.cancel();
        }
        if (rightWaveformScaler != null) {
            rightWaveformScaler.cancel();
        }
    }

    private ObjectAnimator createScaleDownAnimator(View animateView) {
        final ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(animateView, "scaleY", animateView.getScaleY(), 0f);
        objectAnimator.setDuration(SCALE_DOWN_DURATION);
        objectAnimator.setInterpolator(new DecelerateInterpolator());
        return objectAnimator;
    }

    void setWaveformWidths(int waveformWidth) {
        leftWaveform.setLayoutParams(new FrameLayout.LayoutParams(waveformWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        leftLine.setLayoutParams(new FrameLayout.LayoutParams(waveformWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        rightWaveform.setLayoutParams(new FrameLayout.LayoutParams(waveformWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        rightLine.setLayoutParams(new FrameLayout.LayoutParams(waveformWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        dragView.setWidth(waveformWidth + getWidth());

        post(layoutWaveformsRunnable);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (onWidthChangedListener != null) {
            onWidthChangedListener.onWaveformViewWidthChanged(w);
        }
    }

    private Drawable createLoadingDrawable(int color) {
        return new ProgressLineDrawable(color, baseline, spaceWidth);
    }


    public void setWaveformData(WaveformData waveformData, int adjustedWidth) {
        double totalSamples = adjustedWidth / (barWidth + spaceWidth);
        WaveformData scaled = waveformData.scale(totalSamples);

        leftWaveform.initialize(scaled, progressAbovePaint, progressBelowPaint, barWidth, spaceWidth, baseline);
        rightWaveform.initialize(scaled, unplayedAbovePaint, unplayedBelowPaint, barWidth, spaceWidth, baseline);

        leftWaveform.setScaleY(0);
        rightWaveform.setScaleY(0);
    }
}
