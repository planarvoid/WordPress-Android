
package com.soundcloud.utils.record;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class PowerGauge extends View {

    final int SAMPLES_PER_PIXEL = 2205;

    private int SAMPLE_CONCENTRATION = 1;

    final float offset = 0;

    final boolean useLastScale = true;

    /** the last scaling factor to normalize samples **/
    private float scalingFactor = 1;

    /**
     * wheter the plot was cleared, if true we have to recalculate the scaling
     * factor
     **/
    private boolean cleared = true;

    private Bitmap bitmap;

    private float nextBufferX;

    private Matrix m;

    public PowerGauge(Context context) {
        super(context);

        mPaint = new Paint();
        mPaint.setStyle(Style.FILL);
        mPaint.setColor(0xffe36711);

        m = new Matrix();
    }

    public void clear() {
        cleared = true;
        nextBufferX = 0;
        if (bitmap != null)
            bitmap.recycle();
        bitmap = null;
    }

    public final void addSample(float sample) {
        synchronized (this) {
            // mSamples.insert(sample);
        }
    }

    public final void addSamples(float[] samples) {
        synchronized (this) {
            // Log.i(TAG,"ADD SAMPLESSS")
            // if (mSamples == null){
            // return;
            // }
            //        	
            // for (int i = 0; i < samples.length; i++){
            // mSamples.add(samples[i]);
            // }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed && this.getWidth() > 0) {
            // mSamples = new SampleQueue(this.getWidth() * SAMPLES_PER_PIXEL);

            sqWidth = (this.getWidth() - GUTTER_WIDTH * METER_COLORS.length) / METER_COLORS.length;
        } else if (changed) {
            sqWidth = 0;
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            if (nextBufferX > getWidth())
                m.setTranslate(getWidth() - nextBufferX, 0);
            else
                m.setTranslate(0, 0);
            canvas.drawBitmap(bitmap, m, mPaint);
        }

        // drawMeter(canvas);

    }

    private void drawMeter(Canvas canvas) {
        if (sqWidth == 0)
            return;

        int nextBufferX = 0;
        for (int i = 0; i < METER_COLORS.length; i++) {
            // mPaint.setColor(i <= mLastSquareOn ? METER_COLORS_ON[i] :
            // METER_COLORS[i]);
            mPaint.setColor(METER_COLORS[i]);
            if (i > mLastSquareOn)
                mPaint.setAlpha(50);
            canvas.drawRect(nextBufferX, 0, nextBufferX + sqWidth, SQUARE_HEIGHT, mPaint);
            nextBufferX += sqWidth + GUTTER_WIDTH;
        }
    }

    private void calculatePowerFromBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length / 2; i++) { // 16bit sample size

            final long v = getShort(buffer[i * 2], buffer[i * 2 + 1]);
            sum += v;
            sqsum += v * v;
        }

        double power = (sqsum - sum * sum / (buffer.length / 2)) / (buffer.length / 2);

        // Scale to the range 0 - 1.
        power /= MAX_16_BIT * MAX_16_BIT;

        // Convert to dB, with 0 being max power. Add a fudge factor to make
        // a "real" fully saturated input come to 0 dB.
        currentPower = Math.log10(power) * 10f + FUDGE;
    }

    public void updateAmplitude(float maxAmplitude) {
        // Log.i(TAG,"MAX AMP " + maxAmplitude);
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getWidth() * 2, getHeight(), Bitmap.Config.ARGB_8888);
        } else if (nextBufferX + 1 > bitmap.getWidth()) {

            Bitmap old = bitmap;
            bitmap = Bitmap.createBitmap(getWidth() * 2, getHeight(), Bitmap.Config.ARGB_8888);

            Matrix mat = new Matrix();
            mat.setTranslate(-old.getWidth() / 2, 0);

            Canvas c = new Canvas(bitmap);
            c.drawBitmap(old, mat, mPaint);

            nextBufferX = nextBufferX - old.getWidth() / 2;
            old.recycle();
        }

        Canvas c = new Canvas(bitmap);

        // Log.i(TAG,"Current Amplitude: " + maxAmplitude);
        c.drawLine(nextBufferX, this.getHeight() / 2 - maxAmplitude * this.getHeight() / 2,
                nextBufferX, this.getHeight() / 2 + maxAmplitude * this.getHeight() / 2, mPaint);
        nextBufferX++;

    }

    /*
     * Converts a byte[2] to a short, in LITTLE_ENDIAN format
     */
    private short getShort(byte argB1, byte argB2) {
        return (short) (argB1 | (argB2 << 8));
    }

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "PowerGauge";

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    private float[] fSamples;

    private boolean flushedSamples = false;

    private double currentPower;

    double sum = 0;

    double sqsum = 0;

    private Paint mPaint;

    private int mLastSquareOn;

    // private SampleQueue mSamples;
    private int firstSample = 0;

    private int maxSamples = 10000;

    private static final Double POWER_BASE = -60.0;

    private static final Double POWER_PEAK = 0.0;

    private static final int SQUARE_HEIGHT = 20;

    private static final int GUTTER_WIDTH = 3;

    // Maximum signal amplitude for 16-bit data.
    private static final float MAX_16_BIT = 32768;

    // This fudge factor is added to the output to make a realistically
    // fully-saturated signal come to 0dB. Without it, the signal would
    // have to be solid samples of -32768 to read zero, which is not
    // realistic. This really is a fudge, because the best value depends
    // on the input frequency and sampling rate. We optimise here for
    // a 1kHz signal at 16,000 samples/sec.
    private static final float FUDGE = 0.6f;

    private int sqWidth = 0;

    private static final int[] METER_COLORS = {
            0xFF389717, 0xFF3B9617, 0xFF3D9416, 0xFF409316, 0xFF449116, 0xFF488E15, 0xFF4D8C15,
            0xFF518915, 0xFF568714, 0xFF5C8414, 0xFF618114, 0xFF677E13, 0xFF6C7C13, 0xFF717813,
            0xFF777513, 0xFF7E7213, 0xFF846F13, 0xFF8B6C13, 0xFF916813, 0xFF976513, 0xFFAD5B13,
            0xFFBE5214, 0xFFCE4A16, 0xFFE34017, 0xFFEE3919, 0xFFF63519
    };

    /*
     * private static final int[] METER_COLORS = { 0xFF389717 , 0xFF3D9416 ,
     * 0xFF449116 , 0xFF4D8C15 , 0xFF568714 , 0xFF618114 , 0xFF6C7C13 ,
     * 0xFF777513 , 0xFF846F13 , 0xFF916813 , 0xFFAD5B13 , 0xFFCE4A16 ,
     * 0xFFEE3919}; private static final int[] METER_COLORS_ON = { 0xFF3B9617 ,
     * 0xFF409316 , 0xFF488E15 , 0xFF518915 , 0xFF5C8414 , 0xFF677E13 ,
     * 0xFF717813 , 0xFF7E7213 , 0xFF8B6C13 , 0xFF976513 , 0xFFBE5214 ,
     * 0xFFE34017 , 0xFFF63519};
     */

    /*
     * Belose is a very accurate but extremely inneficient way of plotting
     * private void drawSamples(byte[] buffer){ while( i < buffer.length/2 ) {
     * float concentratedSample = 0; try { for (int k = 0; k <
     * SAMPLE_CONCENTRATION; k++){ float sample = 0; for( int j = 0; j <
     * ScCreate.REC_CHANNELS; j++ ) { int shortValue = getShort(buffer[i*2],
     * buffer[i*2+1]); sample += (shortValue * MAX_VALUE); i++; }
     * concentratedSample += sample/ScCreate.REC_CHANNELS; } sampleValue =
     * concentratedSample/SAMPLE_CONCENTRATION; if (sampleIndex == 0) lastValue
     * = createPlotValue(sampleValue); else { float value =
     * createPlotValue(sampleValue); c.drawLine( nextBufferX +
     * (int)((sampleIndex-1) / SAMPLES_PER_PIXEL), this.getHeight() -
     * (int)lastValue,nextBufferX + (int) (sampleIndex) / SAMPLES_PER_PIXEL,
     * this.getHeight() - (int)value, mPaint ); lastValue = value; }
     * sampleIndex++; } catch( Exception ex ) { break; } } nextBufferX =
     * nextBufferX + sampleIndex/SAMPLES_PER_PIXEL; } private float
     * createPlotValue(float sampleValue){ return (sampleValue / scalingFactor)
     * * this.getHeight() / 3 + this.getHeight() / 2 - offset * this.getHeight()
     * / 2; }
     */
}
