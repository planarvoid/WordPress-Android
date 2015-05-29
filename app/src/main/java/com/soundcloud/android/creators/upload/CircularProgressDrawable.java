package com.soundcloud.android.creators.upload;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * Originally from https://gist.github.com/dmide/7506c7d9614eed90805d
 */

public class CircularProgressDrawable extends Drawable
        implements Animatable {

    private static final Interpolator ANGLE_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator SWEEP_INTERPOLATOR = new DecelerateInterpolator();
    private static final int ANGLE_ANIMATOR_DURATION = 2000;
    private static final int SWEEP_ANIMATOR_DURATION = 600;
    private static final int MIN_SWEEP_ANGLE = 30;
    private static final int START_ANGLE = 270;
    private final RectF fBounds = new RectF();

    private ObjectAnimator mObjectAnimatorSweep;
    private ObjectAnimator mObjectAnimatorAngle;
    private ObjectAnimator mObjectAnimatorProgress;
    private boolean mModeAppearing;
    private Paint mPaint;
    private float mCurrentGlobalAngleOffset;
    private float mCurrentGlobalAngle;
    private float mCurrentSweepAngle;
    private float mBorderWidth;
    private boolean mRunning;
    private int progress = 0;

    public CircularProgressDrawable(float borderWidth, int color) {
        mBorderWidth = borderWidth;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(borderWidth);
        mPaint.setColor(color);

        setupAnimations();
    }

    @Override
    public void draw(Canvas canvas) {
        float startAngle, sweepAngle;

        if (mRunning) {
            startAngle = mCurrentGlobalAngle - mCurrentGlobalAngleOffset;
            sweepAngle = mCurrentSweepAngle;
            if (!mModeAppearing) {
                startAngle = startAngle + sweepAngle;
                sweepAngle = 360 - sweepAngle - MIN_SWEEP_ANGLE;
            } else {
                sweepAngle += MIN_SWEEP_ANGLE;
            }
        } else {
            startAngle = START_ANGLE;
            sweepAngle = mCurrentSweepAngle;
        }

        canvas.drawArc(fBounds, startAngle, sweepAngle, false, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        mRunning = true;
        cancelProgressAnimation();
        mObjectAnimatorAngle.start();
        mObjectAnimatorSweep.start();
        invalidateSelf();
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }

        mRunning = false;
        progress = 0;
        mCurrentSweepAngle = 0;
        mObjectAnimatorAngle.cancel();
        mObjectAnimatorSweep.cancel();
        cancelProgressAnimation();
        invalidateSelf();
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        startProgress(progress);
        invalidateSelf();
    }

    private void cancelProgressAnimation() {
        if(mObjectAnimatorProgress != null) {
            mObjectAnimatorProgress.cancel();
        }
    }

    private void startProgress(int progress) {
        mObjectAnimatorProgress = ObjectAnimator.ofFloat(this, mSweepProperty, 360f * progress / 100f);
        mObjectAnimatorProgress.setInterpolator(SWEEP_INTERPOLATOR);
        mObjectAnimatorProgress.setDuration(SWEEP_ANIMATOR_DURATION);
        mObjectAnimatorProgress.setRepeatMode(ValueAnimator.RESTART);
        mObjectAnimatorProgress.setRepeatCount(0);
        mObjectAnimatorProgress.start();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        fBounds.left = bounds.left + mBorderWidth / 2f + .5f;
        fBounds.right = bounds.right - mBorderWidth / 2f - .5f;
        fBounds.top = bounds.top + mBorderWidth / 2f + .5f;
        fBounds.bottom = bounds.bottom - mBorderWidth / 2f - .5f;
    }

    public void setCurrentGlobalAngle(float currentGlobalAngle) {
        mCurrentGlobalAngle = currentGlobalAngle;
        invalidateSelf();
    }

    public float getCurrentGlobalAngle() {
        return mCurrentGlobalAngle;
    }

    public void setCurrentSweepAngle(float currentSweepAngle) {
        mCurrentSweepAngle = currentSweepAngle;
        invalidateSelf();
    }

    public float getCurrentSweepAngle() {
        return mCurrentSweepAngle;
    }

    private void toggleAppearingMode() {
        mModeAppearing = !mModeAppearing;
        if (mModeAppearing) {
            mCurrentGlobalAngleOffset = (mCurrentGlobalAngleOffset + MIN_SWEEP_ANGLE * 2) % 360;
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    ////////////////            Animation

    private Property<CircularProgressDrawable, Float> mAngleProperty
            = new Property<CircularProgressDrawable, Float>(Float.class, "angle") {
        @Override
        public Float get(CircularProgressDrawable object) {
            return object.getCurrentGlobalAngle();
        }

        @Override
        public void set(CircularProgressDrawable object, Float value) {
            object.setCurrentGlobalAngle(value);
        }
    };

    private Property<CircularProgressDrawable, Float> mSweepProperty
            = new Property<CircularProgressDrawable, Float>(Float.class, "arc") {
        @Override
        public Float get(CircularProgressDrawable object) {
            return object.getCurrentSweepAngle();
        }

        @Override
        public void set(CircularProgressDrawable object, Float value) {
            object.setCurrentSweepAngle(value);
        }
    };

    private void setupAnimations() {
        mObjectAnimatorAngle = ObjectAnimator.ofFloat(this, mAngleProperty, 360f);
        mObjectAnimatorAngle.setInterpolator(ANGLE_INTERPOLATOR);
        mObjectAnimatorAngle.setDuration(ANGLE_ANIMATOR_DURATION);
        mObjectAnimatorAngle.setRepeatMode(ValueAnimator.RESTART);
        mObjectAnimatorAngle.setRepeatCount(ValueAnimator.INFINITE);

        mObjectAnimatorSweep = ObjectAnimator.ofFloat(this, mSweepProperty, 360f - MIN_SWEEP_ANGLE * 2);
        mObjectAnimatorSweep.setInterpolator(SWEEP_INTERPOLATOR);
        mObjectAnimatorSweep.setDuration(SWEEP_ANIMATOR_DURATION);
        mObjectAnimatorSweep.setRepeatMode(ValueAnimator.RESTART);
        mObjectAnimatorSweep.setRepeatCount(ValueAnimator.INFINITE);
        mObjectAnimatorSweep.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                toggleAppearingMode();
            }
        });
    }
}