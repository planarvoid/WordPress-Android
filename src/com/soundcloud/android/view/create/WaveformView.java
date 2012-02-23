package com.soundcloud.android.view.create;

import com.soundcloud.android.task.create.CalculateAmplitudesTask;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;

public class WaveformView extends View implements CalculateAmplitudesTask.CalculateAmplitudesListener {

    private final String TAG = getClass().getSimpleName();

    private static final int WAVEFORM_ORANGE = 0xffff8000;
    private static final int WAVEFORM_LIGHT = 0xffffffff;

    private boolean mSized;
    private File mFile;
    private Bitmap mBitmap;
    private Paint mOrangePaint;
    private Paint mWhitePaint;
    private ProgressDialog mProgressDialog;
    private double[] mAmplitudes;

    private CalculateAmplitudesTask mCalculateAmplitudesTask;
    private float mCurrentProgress;
    private double mSampleMax;

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mOrangePaint = new Paint();
        mOrangePaint.setColor(WAVEFORM_ORANGE);

        mWhitePaint = new Paint();
        mWhitePaint.setColor(WAVEFORM_LIGHT);
    }

    public void setFromFile(File f) {
        mFile = f;
        if (mSized) makeWave();
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        mSized = true;
        if (mFile != null) makeWave();
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
                canvas.drawLine(i, height / 2 - halfWaveHeight, i, height / 2 + halfWaveHeight,
                        i >= currentProgressIndex ? mWhitePaint : mOrangePaint);
                i++;
            }
        }
    }

    private void makeWave(){
        if (mCalculateAmplitudesTask != null){
            mCalculateAmplitudesTask.cancel(true);
        }

        mCalculateAmplitudesTask = new CalculateAmplitudesTask(mFile,getWidth());
        mCalculateAmplitudesTask.addListener(this);
        mCalculateAmplitudesTask.execute();

        mProgressDialog = ProgressDialog.show(getContext(),"Please wait", "Analyzing the hell out of your file", true,false);
    }


    @Override
    public void onSuccess(File f, double[] amplitudes, double sampleMax) {
        mProgressDialog.cancel();
        mAmplitudes = amplitudes;
        mSampleMax = sampleMax;
        invalidate();
    }

    @Override
    public void onError(File f) {
        mProgressDialog.cancel();
        Log.e(TAG, "Error making waveform, file: " + f.getAbsolutePath());
    }

    public Object saveConfigurationInstance() {
        return new Configuration() {
            {
                calculateAmplitudesTask = mCalculateAmplitudesTask;
                bitmap = mBitmap;
            }
        };
    }

    public void restoreConfigurationInstance(Object state) {
        mCalculateAmplitudesTask = ((Configuration) state).calculateAmplitudesTask;
        mBitmap = ((Configuration) state).bitmap;

        if (mCalculateAmplitudesTask != null){
            mCalculateAmplitudesTask.addListener(this);
        }
    }

    public void setCurrentProgress(float currentProgress) {
        mCurrentProgress = currentProgress;
        invalidate();
    }

    public float getCurrentProgress() {
        return mCurrentProgress;
    }

    private static class Configuration {
        CalculateAmplitudesTask calculateAmplitudesTask;
        Bitmap bitmap;
    }
}
