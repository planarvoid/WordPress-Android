package com.soundcloud.android.playback.ui.view;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.view.FixedWidthView;
import com.soundcloud.android.view.ListenableHorizontalScrollView;
import com.soundcloud.android.waveform.WaveformData;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
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

    private final ImageView leftWaveform;
    private final ImageView rightWaveform;
    private final ImageView leftLine;
    private final ImageView rightLine;

    private final FixedWidthView dragView;
    private final ListenableHorizontalScrollView dragViewHolder;

    private SpringSystem springSystem = SpringSystem.create();
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

    public interface OnWidthChangedListener{
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

        leftWaveform = (ImageView) findViewById(R.id.waveform_left);
        rightWaveform = (ImageView) findViewById(R.id.waveform_right);

        dragView = (FixedWidthView) findViewById(R.id.drag_view);
        dragViewHolder = (ListenableHorizontalScrollView) findViewById(R.id.drag_view_holder);
        dragViewHolder.setOverScrollMode(View.OVER_SCROLL_NEVER);

        ViewHelper.setScaleY(leftWaveform, 0);
        ViewHelper.setScaleY(rightWaveform, 0);

        // pivot positions for scaling animations
        ViewHelper.setPivotY(leftWaveform, baseline);
        ViewHelper.setPivotY(rightWaveform, baseline);

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

    ImageView getLeftWaveform() {
        return leftWaveform;
    }

    ImageView getRightWaveform() {
        return rightWaveform;
    }

    ImageView getLeftLine() {
        return leftLine;
    }

    ImageView getRightLine() {
        return rightLine;
    }

    void setWaveformTranslations(int leftTranslation, int rightTranslation) {
        ViewHelper.setTranslationX(leftWaveform, leftTranslation);
        ViewHelper.setTranslationX(rightWaveform, rightTranslation);
        ViewHelper.setTranslationX(leftLine, leftTranslation);
        ViewHelper.setTranslationX(rightLine, rightTranslation);
    }

    void showExpandedWaveform() {
        clearScaleAnimations();

        springY = springSystem.createSpring();
        springY.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float value = (float) spring.getCurrentValue();
                ViewHelper.setScaleY(rightWaveform, value);
                ViewHelper.setScaleY(leftWaveform, value);
                invalidate(); // can we do anything cheaper than this?
            }
        });
        springY.setSpringConfig(SpringConfig.fromOrigamiTensionAndFriction(SPRING_TENSION, SPRING_FRICTION));
        springY.setCurrentValue(ViewHelper.getScaleY(leftWaveform));
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
        ViewHelper.setTranslationX(leftLine, ViewHelper.getTranslationX(leftWaveform));
        ViewHelper.setTranslationX(rightLine, ViewHelper.getTranslationX(rightWaveform));
        leftLine.setVisibility(View.VISIBLE);
        rightLine.setVisibility(View.VISIBLE);
    }

    private void clearScaleAnimations() {
        if (springY != null){
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
        final ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(animateView, "scaleY", ViewHelper.getScaleY(animateView), 0f);
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
        if (onWidthChangedListener != null){
            onWidthChangedListener.onWaveformViewWidthChanged(w);
        }
    }

    void showLoading() {
        leftWaveform.setImageDrawable(createLoadingDrawable(progressColor));
        rightWaveform.setImageDrawable(createLoadingDrawable(unplayedColor));
    }

    private Drawable createLoadingDrawable(int color) {
        return new ProgressLineDrawable(color, baseline, spaceWidth);
    }

    void setWaveformBitmaps(Pair<Bitmap, Bitmap> bitmaps) {
        leftWaveform.setImageBitmap(bitmaps.first);
        rightWaveform.setImageBitmap(bitmaps.second);

        // scale down as they will be scaled up when playback active
        ViewHelper.setScaleY(leftWaveform, 0);
        ViewHelper.setScaleY(rightWaveform, 0);
    }

    Pair<Bitmap, Bitmap> createWaveforms(WaveformData waveformData, int width) {
        return new Pair<Bitmap, Bitmap>(
                createWaveform(waveformData, width, progressAbovePaint, progressBelowPaint),
                createWaveform(waveformData, width, unplayedAbovePaint, unplayedBelowPaint)
        );
    }

    private Bitmap createWaveform(WaveformData waveformData, int width, Paint abovePaint, Paint belowPaint) {
        final int height = getHeight();
        Bitmap wave = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(wave);
        WaveformData scaled = waveformData.scale(width);

        int acc = 0;
        int groupIndex = 0;
        int dumpIndex = -1;
        for (int i = 0; i < scaled.samples.length; i++) {
            if (dumpIndex >= 0) {
                dumpIndex++;
                if (dumpIndex == spaceWidth) {
                    dumpIndex = -1;
                }
            } else {
                acc += scaled.samples[i];
                groupIndex++;
                if (groupIndex == barWidth || i == scaled.samples.length - 1) {
                    final int sample = acc / groupIndex;
                    for (int j = i - groupIndex + 1; j <= i; j++) {
                        drawBarAbove(canvas, j, sample * baseline / waveformData.maxAmplitude, abovePaint);
                        drawBarBelow(canvas, j, sample * (height - baseline) / waveformData.maxAmplitude, belowPaint);
                    }
                    acc = groupIndex = dumpIndex = 0;
                }
            }
        }
        return wave;
    }

    private void drawBarAbove(Canvas canvas, int x, int height, Paint abovePaint) {
        canvas.drawLine(x, baseline - height, x, baseline, abovePaint);
    }

    private void drawBarBelow(Canvas canvas, int x, int height, Paint belowPaint) {
        canvas.drawLine(x, baseline + spaceWidth, x, baseline + height - spaceWidth, belowPaint);
    }
}
