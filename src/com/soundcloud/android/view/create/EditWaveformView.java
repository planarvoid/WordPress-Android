package com.soundcloud.android.view.create;

import com.soundcloud.android.task.create.CalculateAmplitudesTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class EditWaveformView extends View {

    private final String TAG = getClass().getSimpleName();

    private static final int WAVEFORM_DARK_UNPLAYED = 0xff666666;
    private static final int WAVEFORM_UNPLAYED = 0xffffffff;
    private static final int WAVEFORM_DARK_PLAYED = 0xff662000;
    private static final int WAVEFORM_PLAYED = 0xffff8000;


    private boolean mSized;
    private Paint mPlayedPaint, mUnplayedPaint,mDarkUnplayedPaint,mDarkPlayedPaint;
    private double[] mAmplitudes;

    private float mCurrentProgress;
    private double mSampleMax;
    private int mTrimLeft, mTrimRight;

    public EditWaveformView(Context context) {
        super(context);
        init();
    }

    public EditWaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditWaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mPlayedPaint = new Paint();
        mPlayedPaint.setColor(WAVEFORM_PLAYED);

        mUnplayedPaint = new Paint();
        mUnplayedPaint.setColor(WAVEFORM_UNPLAYED);

        mDarkUnplayedPaint = new Paint();
        mDarkUnplayedPaint.setColor(WAVEFORM_DARK_UNPLAYED);

        mDarkPlayedPaint = new Paint();
        mDarkPlayedPaint.setColor(WAVEFORM_DARK_PLAYED);
    }


    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        if (mAmplitudes != null){
            final int width = getWidth();
            final int height = getHeight();
            final int currentProgressIndex = (int) (width*mCurrentProgress);

            int i = 0;
            for (double amplitude : mAmplitudes) {
                final int halfWaveHeight = (int) ((amplitude / mSampleMax) * height / 2);
                if (i == mTrimLeft || i == mTrimRight -1) {
                    canvas.drawLine(i, 0, i, height, mUnplayedPaint);
                } else {
                    final Paint p = (i < mTrimLeft) ? mDarkPlayedPaint :
                                    ((i > mTrimRight) ? mDarkUnplayedPaint :
                                     (i >= currentProgressIndex) ? mUnplayedPaint : mPlayedPaint);

                    canvas.drawLine(i, height / 2 - halfWaveHeight, i, height / 2 + halfWaveHeight, p);
                }

                i++;
            }
        }
    }



    public void setCurrentProgress(float currentProgress) {
        mCurrentProgress = currentProgress;
        invalidate();
    }

    public float getCurrentProgress() {
        return mCurrentProgress;
    }

    public void setWave(double[] amplitudes, double sampleMax) {
        mAmplitudes = amplitudes;
        mSampleMax = sampleMax;
        invalidate();
        mTrimLeft = 0;
        mTrimRight = getWidth();
    }

    public void setTrimLeft(int trimLeft) {
        mTrimLeft = trimLeft;
        invalidate();
    }

    public void setTrimRight(int trimRight) {
        mTrimRight = trimRight;
        invalidate();
    }

    private static class Configuration {
        CalculateAmplitudesTask calculateAmplitudesTask;
        Bitmap bitmap;
    }
}
