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
    private File mFile;
    private long mCurrentProgress, mTotalBytes;
    private boolean mPlaying;

    public void setFile(File f){
        if (mPlaying) stop();
        try {
            FileInputStream fin = new FileInputStream(f);
            WaveHeader waveHeader = new WaveHeader();
            waveHeader.read(fin);
            mTotalBytes = waveHeader.getNumBytes();
            mFile = f;

        } catch (IOException e) {
            e.printStackTrace();
            mFile = null;
        }
    }

    public boolean isPlaying(){
        return mPlaying;
    }

    public void togglePlayback() {
        if (mPlaying) {
            stopPlayback();
        } else {
            play();
        }
    }

    public void play(){
        if (!mPlaying){
            mPlaying = true;
            mPlayRawAudioTask = new PlayRawAudioTask(this, mFile);
            mPlayRawAudioTask.execute(mCurrentProgress);
        }
    }

    public void stop(){
        stopPlayback();
        mCurrentProgress = 0;
    }

    public float getCurrentProgressPercent(){
        return Math.min(1,((float) mCurrentProgress)/mTotalBytes);
    }

    private void stopPlayback(){
        mPlaying = false;
        if (mPlayRawAudioTask != null && mPlayRawAudioTask.isPlaying){
            mPlayRawAudioTask.stop();
        }
    }

    private void setCurrentProgress(long progress) {
        mCurrentProgress = progress;
    }

    public void seekTo(float percentage) {
        mCurrentProgress = (long) (percentage * mTotalBytes);
        if (mPlaying){
            // TODO, stop and start just to seek. this is lazy
            stopPlayback();
            play();
        }
    }

    private static class PlayRawAudioTask extends AsyncTask<Long, Long, Boolean> {
        private File mFile;
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
        protected Boolean doInBackground(Long... params) {
            Long offset = params[0];
            int bufferSize = 1024;
            int i = 0;
            byte[] s = new byte[bufferSize];
            try {
                FileInputStream fin = new FileInputStream(mFile);
                WaveHeader waveHeader = new WaveHeader();
                int headerLength = waveHeader.read(fin);

                // round to the nearest buffer size to ensure valid audio data (TODO can this be more precise?)
                offset = (offset / minSize) * minSize;

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
                rawAudioPlayerWeakReference.get().setCurrentProgress(values[0]);
            }

        }
    }

}
