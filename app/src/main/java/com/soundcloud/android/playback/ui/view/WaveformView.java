package com.soundcloud.android.playback.ui.view;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.waveform.WaveformResult;
import rx.Observable;

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
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import javax.inject.Inject;

public class WaveformView extends LinearLayout implements ProgressAware {

    private static final int DEFAULT_BAR_WIDTH_DP = 2;
    private static final int DEFAULT_BAR_SPACE_DP = 1;
    private static final int DEFAULT_BASELINE_DP = 68;
    private static final float DEFAULT_WAVEFORM_WIDTH_RATIO = 1.5f;
    private static final int SCALE_UP_DURATION = 200;
    private static final int SCALE_DOWN_DURATION = 70;

    private final int barWidth;
    private final int spaceWidth;
    private final int baseline;
    private final Paint progressAbovePaint;
    private final Paint progressBelowPaint;
    private final Paint unplayedAbovePaint;
    private final Paint unplayedBelowPaint;
    private final int progressColor;
    private final int unplayedColor;

    private final ImageView leftWaveform;
    private final ImageView rightWaveform;
    private final ImageView leftLine;
    private final ImageView rightLine;

    private final WaveformViewController waveformViewController;

    private ObjectAnimator leftWaveformScaler;
    private ObjectAnimator rightWaveformScaler;

    @Inject WaveformViewControllerFactory waveformViewControllerFactory;

    private final Runnable layoutWaveformsRunnable = new Runnable() {
        @Override
        public void run() {
            leftWaveform.requestLayout();
            rightWaveform.requestLayout();
            leftLine.requestLayout();
            rightLine.requestLayout();
        }
    };

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);

        SoundCloudApplication.getObjectGraph().inject(this);

        final float density = getResources().getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaveformView);
        final int progressAboveStart = a.getColor(R.styleable.WaveformView_progressAboveStart, Color.WHITE);
        final int progressAboveEnd = a.getColor(R.styleable.WaveformView_progressAboveEnd, Color.WHITE);
        final int progressBelow = a.getColor(R.styleable.WaveformView_progressBelow, Color.WHITE);
        final int unplayedAbove = a.getColor(R.styleable.WaveformView_unplayedAbove, Color.WHITE);
        final int unplayedBelow = a.getColor(R.styleable.WaveformView_unplayedBelow, Color.WHITE);
        float waveformWidthRatio = a.getFloat(R.styleable.WaveformView_widthRatio, DEFAULT_WAVEFORM_WIDTH_RATIO);
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

        ViewHelper.setScaleY(leftWaveform, 0);
        ViewHelper.setScaleY(rightWaveform, 0);

        // pivot positions for scaling animations
        ViewHelper.setPivotY(leftWaveform, baseline);
        ViewHelper.setPivotY(rightWaveform, baseline);

        leftLine = (ImageView) findViewById(R.id.line_left);
        rightLine = (ImageView) findViewById(R.id.line_right);

        leftLine.setImageDrawable(createLoadingDrawable(progressColor));
        rightLine.setImageDrawable(createLoadingDrawable(unplayedColor));

        waveformViewController = waveformViewControllerFactory.create(this, waveformWidthRatio);
    }

    public void showPlayingState(PlaybackProgress progress) {
        waveformViewController.showPlayingState(progress);
    }

    public void setProgress(PlaybackProgress progress) {
        waveformViewController.setProgress(progress);
    }

    public void showIdleState() {
        waveformViewController.showIdleState();
    }

    ImageView getLeftWaveform() {
        return leftWaveform;
    }

    ImageView getRightWaveform() {
        return rightWaveform;
    }

    void setWaveformTranslations(int leftTranslation, int rightTranslation) {
        ViewHelper.setTranslationX(leftWaveform, leftTranslation);
        ViewHelper.setTranslationX(rightWaveform, rightTranslation);
        ViewHelper.setTranslationX(leftLine, leftTranslation);
        ViewHelper.setTranslationX(rightLine, rightTranslation);
    }

    void showIdleLinesAtWaveformPositions() {
        ViewHelper.setTranslationX(leftLine, ViewHelper.getTranslationX(leftWaveform));
        ViewHelper.setTranslationX(rightLine, ViewHelper.getTranslationX(rightWaveform));
        leftLine.setVisibility(View.VISIBLE);
        rightLine.setVisibility(View.VISIBLE);
    }

    void hideIdleLines() {
        leftLine.setVisibility(View.GONE);
        rightLine.setVisibility(View.GONE);
    }

    void scaleUpWaveforms() {
        clearScaleAnimations();

        leftWaveformScaler = createScaleUpAnimator(leftWaveform);
        leftWaveformScaler.start();

        rightWaveformScaler = createScaleUpAnimator(rightWaveform);
        rightWaveformScaler.start();
    }

    void scaleDownWaveforms() {
        clearScaleAnimations();

        leftWaveformScaler = createScaleDownAnimator(leftWaveform);
        leftWaveformScaler.start();

        rightWaveformScaler = createScaleDownAnimator(rightWaveform);
        rightWaveformScaler.start();
    }

    private void clearScaleAnimations() {
        if (leftWaveformScaler != null) {
            leftWaveformScaler.cancel();
        }
        if (rightWaveformScaler != null) {
            rightWaveformScaler.cancel();
        }
    }

    private ObjectAnimator createScaleUpAnimator(View animateView) {
        final ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(animateView, "scaleY", ViewHelper.getScaleY(leftWaveform), 1f);
        objectAnimator.setDuration(SCALE_UP_DURATION);
        objectAnimator.setInterpolator(new OvershootInterpolator());
        return objectAnimator;
    }

    private ObjectAnimator createScaleDownAnimator(View animateView) {
        final ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(animateView, "scaleY", ViewHelper.getScaleY(leftWaveform), 0f);
        objectAnimator.setDuration(SCALE_DOWN_DURATION);
        objectAnimator.setInterpolator(new DecelerateInterpolator());
        return objectAnimator;
    }

    void setWaveformWidths(int width) {
        leftWaveform.setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        leftLine.setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        rightWaveform.setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        rightLine.setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));

        post(layoutWaveformsRunnable);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        waveformViewController.onWaveformViewWidthChanged(w);
    }

    public void displayWaveform(Observable<WaveformResult> waveformResultObservable) {
        waveformViewController.displayWaveform(waveformResultObservable);
    }

    void showLoading() {
        leftWaveform.setImageDrawable(createLoadingDrawable(progressColor));
        rightWaveform.setImageDrawable(createLoadingDrawable(unplayedColor));
    }

    private Drawable createLoadingDrawable(int color) {
        return new ProgressLineDrawable(color, baseline - spaceWidth, spaceWidth);
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
