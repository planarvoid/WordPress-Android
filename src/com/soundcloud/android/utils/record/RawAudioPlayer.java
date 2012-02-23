package com.soundcloud.android.utils.record;

import com.soundcloud.android.utils.CloudUtils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RawAudioPlayer {

    private PlayRawAudioTask mPlayRawAudioTask;
    private float mCurrentProgress;

    public void play(File f){
        play(f,0);
    }

    public void play(File f, long position){
        mPlayRawAudioTask = new PlayRawAudioTask(f);
        mPlayRawAudioTask.execute(position);
    }

    public void stop() {
        if (mPlayRawAudioTask != null && !CloudUtils.isTaskFinished(mPlayRawAudioTask)){
            mPlayRawAudioTask.stop();
        }
    }

    public float getCurrentProgress(){
        return mCurrentProgress;
    }

    private class PlayRawAudioTask extends AsyncTask<Long, Long, Boolean> {
        private File mFile;
        private long mLength;
        private AudioTrack mAudioTrack;
        private boolean mPlaying;

        public PlayRawAudioTask(File f) {
            this(f, 44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        }

        public PlayRawAudioTask(File f, int sampleRate, int channelConfiguration, int encoding) {
            mFile = f;
            int minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfiguration, encoding);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfiguration, encoding,
                    minSize, AudioTrack.MODE_STREAM);
        }

        public void stop(){
            mPlaying = false;
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

                mLength = waveHeader.getNumBytes();

                DataInputStream dis = new DataInputStream(fin);
                dis.skip(headerLength + offset);

                long written = 0;
                while ((i = dis.read(s, 0, bufferSize)) > -1 && mPlaying) {
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
            mPlaying = true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }

        @Override
        protected void onProgressUpdate(Long... values){
            mCurrentProgress = Math.min(1, (float) values[0] / mLength);
        }
    }


}
