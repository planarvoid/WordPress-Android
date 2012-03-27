package com.soundcloud.android.task.create;

import android.os.AsyncTask;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.record.WaveHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class CalculateAmplitudesTask extends AsyncTask<Void,Integer,Boolean>
{
    final private String TAG = SoundCloudApplication.TAG;
    final private int mChunks;
    final private File mFile;
    private long onStart;

    private ArrayList<WeakReference<CalculateAmplitudesListener>> mListenerWeakReferences;

    private double[] mAmplitudes;
    private double mSampleMax;

    public CalculateAmplitudesTask(File f, int chunks){
        mFile = f;
        mChunks = chunks;
    }

    public void addListener(CalculateAmplitudesListener listener){
        if (mListenerWeakReferences == null){
            mListenerWeakReferences = new ArrayList<WeakReference<CalculateAmplitudesListener>>();
        }
        mListenerWeakReferences.add(new WeakReference<CalculateAmplitudesListener>(listener));
    }

    protected void onPreExecute() {
        onStart = System.currentTimeMillis();
        Log.d(TAG, "Starting analyzing play at " + onStart);
    }


    protected void onPostExecute(Boolean result) {
        Log.d(TAG,"Finished analyzing play: [duration:" + (System.currentTimeMillis() - onStart) + ", success: " + result + "]");
        if (mListenerWeakReferences != null && !isCancelled()) {
            for (WeakReference<CalculateAmplitudesListener> listenerRef : mListenerWeakReferences) {
                CalculateAmplitudesListener listener = listenerRef.get();
                if (listener != null) {
                    if (result) {
                        listener.onSuccess(mFile, mAmplitudes, mSampleMax);
                    } else {
                        listener.onError(mFile);
                    }
                }
            }
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            FileInputStream in = new FileInputStream(mFile);

            WaveHeader waveHeader = new WaveHeader();
            int headerLength = waveHeader.read(in);

            final int samples = waveHeader.getNumBytes() / 2;

            int chunkSize = samples / mChunks;
            double sampleSum, sampleMean;
            mAmplitudes = new double[mChunks];

            byte[] bytes = new byte[chunkSize * 2];
            for (int chunkIndex = 0; chunkIndex < mChunks; chunkIndex++) {

                // Read in the bytes
                int offset = 0, numRead = 0;
                while (offset < bytes.length
                        && (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0) {
                    offset += numRead;
                }

                // sum the samples (shorts), so far this is the fastest method I have found
                sampleSum = 0.0;
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < offset/2; i++){
                    sampleSum += Math.abs(byteBuffer.getShort());
                }

                sampleMean = sampleSum / offset;
                mSampleMax = mSampleMax > sampleMean ? mSampleMax : sampleMean;
                mAmplitudes[chunkIndex] = sampleMean;
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public interface CalculateAmplitudesListener {
        void onSuccess(File f, double[] amplitudes, double sampleMax);
        void onError(File f);
    }
}
