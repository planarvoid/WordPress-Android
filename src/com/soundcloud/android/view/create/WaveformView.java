package com.soundcloud.android.view.create;

import com.soundcloud.android.utils.record.WaveHeader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class WaveformView extends View {

    private static final int HEADER_LENGTH = 44;

    /**
     * Indicates PCM format.
     */
    public static final short FORMAT_PCM = 1;
    /**
     * Indicates ALAW format.
     */
    public static final short FORMAT_ALAW = 6;
    /**
     * Indicates ULAW format.
     */
    public static final short FORMAT_ULAW = 7;

    private short mFormat;
    private short mNumChannels;
    private int mSampleRate;
    private short mBitsPerSample;
    private int mNumBytes;
    private boolean mSized;
    private File mFile;
    private Bitmap mBitmap;

    private final Paint mPaint = new Paint();
    private static final int WAVEFORM_ORANGE = 0xffff8000;

    public WaveformView(Context context) {
        super(context);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

    protected void onDraw(android.graphics.Canvas canvas) {
        if (mBitmap != null) canvas.drawBitmap(mBitmap, new Matrix(), mPaint);
    }

    private void makeWave(){
        try {
            makeWave(new FileInputStream(mFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void makeWave(FileInputStream in) throws IOException {

        WaveHeader waveHeader = new WaveHeader();
        int headerLength = waveHeader.read(in);
        final int samples = waveHeader.getNumBytes() / 2;

        final int width = getWidth();
        final int height = getHeight();
        int chunkSize = samples / width;
        double sampleSum, sampleMean, sampleMax = 0;
        double amplitudes[] = new double[width];

        byte[] bytes = new byte[chunkSize * 2];

        for (int chunkIndex = 0; chunkIndex < width; chunkIndex++) {
             // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            // sum the samples (shorts)
            sampleSum = 0.0;
            int byteIndex = 0;
            while (byteIndex < offset){
                short s = (short) byteBuffer.getShort();
                sampleSum += Math.abs(s);
                byteIndex += 2;
            }

            sampleMean = sampleSum / (double) chunkSize;
            sampleMax = sampleMax > sampleMean ? sampleMax : sampleMean;
            amplitudes[chunkIndex] = sampleMean;
        }

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mPaint.setColor(WAVEFORM_ORANGE);
        Canvas c = new Canvas(mBitmap);
        int i = 0;
        for (double amplitude : amplitudes) {
            final int halfWaveHeight = (int) ((amplitude / sampleMax) * height / 2);
            c.drawLine(i, height / 2 - halfWaveHeight, i, height / 2 + halfWaveHeight, mPaint);
            i++;
        }

        invalidate();
    }

    public static short readShort(byte[] data, int offset) {
        return (short) (((data[offset] << 8)) | ((data[offset + 1] & 0xff)));
    }

    private static void readId(InputStream in, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) != in.read()) throw new IOException(id + " tag not present");
        }
    }

    private static int readInt(InputStream in) throws IOException {
        return in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    }

    private static short readShort(InputStream in) throws IOException {
        return (short) (in.read() | (in.read() << 8));
    }

    /**
     * http://code.google.com/p/moonblink/source/browse/trunk/HermitLibrary/src/org/hermit/dsp/SignalPower.java
     * <p/>
     * Calculate the bias and range of the given input signal.
     *
     * @param sdata   Buffer containing the input samples to process.
     * @param off     Offset in sdata of the data of interest.
     * @param samples Number of data samples to process.
     * @param out     A float array in which the results will be placed
     *                Must have space for two entries, which will be
     *                set to:
     *                <ul>
     *                <li>The bias, i.e. the offset of the average
     *                signal value from zero.
     *                <li>The range, i.e. the absolute value of the largest
     *                departure from the bias level.
     *                </ul>
     * @throws NullPointerException           Null output array reference.
     * @throws ArrayIndexOutOfBoundsException Output array too small.
     */
    public final static void biasAndRange(short[] sdata, int off, int samples,
                                          float[] out) {
        // Find the max and min signal values, and calculate the bias.
        short min = 32767;
        short max = -32768;
        int total = 0;
        for (int i = off; i < off + samples; ++i) {
            final short val = sdata[i];
            total += val;
            if (val < min)
                min = val;
            if (val > max)
                max = val;
        }
        final float bias = (float) total / (float) samples;
        final float bmin = min + bias;
        final float bmax = max - bias;
        final float range = Math.abs(bmax - bmin) / 2f;

        out[0] = bias;
        out[1] = range;
    }

}
