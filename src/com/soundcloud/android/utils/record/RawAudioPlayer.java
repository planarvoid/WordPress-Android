package com.soundcloud.android.utils.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class RawAudioPlayer {

    private PlayRawAudioTask mPlayRawAudioTask;
    private float mCurrentProgress;
    private File mFile;
    private boolean mPlaying;

    public void setFile(File f){
        mFile = f;
        if (mPlaying) stop();
    }

    public boolean isPlaying(){
        return mPlaying;
    }

    public void togglePlayback(float currentProgress) {
        if (mPlaying) {
            stopPlayback();
        } else {
            play(currentProgress);
        }
    }

    public void play(){
        play(0);
    }

    public void play(float offsetPercent){
        if (!mPlaying){
            mPlaying = true;
            mPlayRawAudioTask = new PlayRawAudioTask(this, mFile);
            mPlayRawAudioTask.execute(offsetPercent);
        }
    }

    public void stop(){
        stopPlayback();
        mCurrentProgress = 0;
    }

    public float getCurrentProgress(){
        return mCurrentProgress;
    }

    private void stopPlayback(){
        mPlaying = false;
        if (mPlayRawAudioTask != null && mPlayRawAudioTask.isPlaying){
            mPlayRawAudioTask.stop();
        }
    }

    private void setCurrentProgress(float progressPercent) {
        mCurrentProgress = progressPercent;
    }

    private static class PlayRawAudioTask extends AsyncTask<Float, Long, Boolean> {
        private File mFile;
        private long mLength;
        private AudioTrack mAudioTrack;
        private int minSize;
        private boolean isPlaying;
        private WeakReference<RawAudioPlayer> rawAudioPlayerWeakReference;


        public PlayRawAudioTask(RawAudioPlayer rawAudioPlayer, File f) {
            this(f, 44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            setRawAudioPlayer(rawAudioPlayer);
        }

        public PlayRawAudioTask(File f, int sampleRate, int channelConfiguration, int encoding) {
            mFile = f;
            minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfiguration, encoding);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfiguration, encoding,
                    minSize, AudioTrack.MODE_STREAM);
        }



        public void setRawAudioPlayer(RawAudioPlayer rawAudioPlayer){
            rawAudioPlayerWeakReference = new WeakReference<RawAudioPlayer>(rawAudioPlayer);
        }

        public void stop(){
            isPlaying = false;
        }

        @Override
        protected Boolean doInBackground(Float... params) {
            Float offsetPercent = params[0];
            int bufferSize = 1024;
            int i = 0;
            byte[] s = new byte[bufferSize];
            try {
                FileInputStream fin = new FileInputStream(mFile);
                WaveHeader waveHeader = new WaveHeader();
                int headerLength = waveHeader.read(fin);

                mLength = waveHeader.getNumBytes();
                // round to the nearest buffer size to ensure valid audio data (TODO can this be more precise?)
                long offset = ((long) (offsetPercent * mLength) / minSize) * minSize;

                DataInputStream dis = new DataInputStream(fin);
                dis.skip(headerLength + offset);

                long written = 0;
                while ((i = dis.read(s, 0, bufferSize)) > -1 && isPlaying) {
                    written += mAudioTrack.write(s, 0, i);
                    publishProgress(written+offset);
                }
                dis.close();
                fin.close();
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            mAudioTrack.play();
            isPlaying = true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }

        @Override
        protected void onProgressUpdate(Long... values){
            if (isPlaying && rawAudioPlayerWeakReference != null && rawAudioPlayerWeakReference.get() != null){
                rawAudioPlayerWeakReference.get().setCurrentProgress(Math.min(1, (float) values[0] / mLength));
            }

        }
    }

}
