package com.soundcloud.android.utils.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class RawAudioPlayer {

    private static final long SEEK_INTERVAL = 200;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private PlayRawAudioTask mPlayRawAudioTask;
    private File mFile;
    private long mCurrentProgress, mTotalBytes, mStartPos, mEndPos, mNextSeek, mLastSeekAt;
    private int mMinBufferSize;
    private boolean mPlaying;
    private PlaybackListener mListener;

    public RawAudioPlayer() {
        mMinBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
    }

    public interface PlaybackListener {
        void onPlaybackStart();
        void onPlaybackStopped();
        void onPlaybackComplete();
    }

    public void setListener(PlaybackListener listener){
        mListener = listener;
    }
    
    public void setFile(File f){
        if (mPlaying) stop();
        try {
            FileInputStream fin = new FileInputStream(f);
            WaveHeader waveHeader = new WaveHeader();
            waveHeader.read(fin);
            mEndPos = mTotalBytes = waveHeader.getNumBytes();
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
            mPlayRawAudioTask = new PlayRawAudioTask(this, mFile, SAMPLE_RATE,CHANNEL_CONFIG,ENCODING);
            mPlayRawAudioTask.setBounds(mStartPos,mEndPos);
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
        seekTo(getValidBytePos((long) (percentage * mTotalBytes)));
    }

    public void seekTo(long position) {
        mNextSeek = position;
        if (!mSeekHandler.hasMessages(1)){
            if (System.currentTimeMillis() - mLastSeekAt > SEEK_INTERVAL){
                doSeek();
            } else {
                mSeekHandler.sendEmptyMessageDelayed(1,SEEK_INTERVAL - (System.currentTimeMillis() - mLastSeekAt));
            }
        }
    }

    Handler mSeekHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            doSeek();
        }
    };

    private void doSeek(){
        mCurrentProgress = mNextSeek;
            mLastSeekAt = System.currentTimeMillis();
            if (mPlaying){
                // TODO, stop and start just to seek. this is lazy
                stopPlayback();
                play();
            }
    }

    private void onPlaybackStart() {
        mPlaying = true;
        if (mListener != null){
            mListener.onPlaybackStart();
        }
    }

    private void onPlaybackStopped() {
        mPlaying = false;
        if (mListener != null){
            mListener.onPlaybackStopped();
        }
    }

    private void onPlaybackComplete() {
        mPlaying = false;
        mCurrentProgress = 0;
        if (mListener != null){
            mListener.onPlaybackComplete();
        }
    }

    public void onNewStartPosition(float percent) {
        mStartPos = getValidBytePos((long) (percent * mTotalBytes));
        if (mCurrentProgress < mStartPos && mPlaying) seekTo(mStartPos);
    }

    public void onNewEndPosition(float percent) {
        mEndPos = getValidBytePos((long) (percent * mTotalBytes));
        if (mCurrentProgress > mEndPos && mPlaying) stop();
    }

    private long getValidBytePos(long in){
        return (in / mMinBufferSize) * mMinBufferSize;
    }


    private static class PlayRawAudioTask extends AsyncTask<Long, Long, Boolean> {
        private File mFile;
        private AudioTrack mAudioTrack;
        private int minSize, headerLength;
        private boolean isPlaying;
        private long mEndPos, mStartPos, mLastPlayedPos;
        private WeakReference<RawAudioPlayer> rawAudioPlayerWeakReference;

        public PlayRawAudioTask(RawAudioPlayer rawAudioPlayer, File f, int sampleRate, int channelConfiguration, int encoding) {
            mFile = f;
            minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfiguration, encoding);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfiguration, encoding,
                    minSize, AudioTrack.MODE_STREAM);
            setRawAudioPlayer(rawAudioPlayer);
        }

        public void setRawAudioPlayer(RawAudioPlayer rawAudioPlayer){
            rawAudioPlayerWeakReference = new WeakReference<RawAudioPlayer>(rawAudioPlayer);
        }

        public void stop(){
            isPlaying = false;
        }

        @Override
        protected Boolean doInBackground(Long... params) {
            Long offset = Math.max(params[0],mStartPos);
            int bufferSize = 1024;
            int i = 0;
            byte[] s = new byte[bufferSize];
            try {
                FileInputStream fin = new FileInputStream(mFile);
                WaveHeader waveHeader = new WaveHeader();
                headerLength = waveHeader.read(fin);

                // round to the nearest buffer size to ensure valid audio data (TODO can this be more precise?)
                offset = (offset / minSize) * minSize;

                DataInputStream dis = new DataInputStream(fin);
                dis.skip(headerLength + offset);

                long written = 0;
                while ((i = dis.read(s, 0, bufferSize)) > -1 && (written + offset < mEndPos) && isPlaying) {
                    written += mAudioTrack.write(s, 0, i);
                    publishProgress(written+offset);
                }
                mLastPlayedPos = written + offset;
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
            if (rawAudioPlayerWeakReference != null && rawAudioPlayerWeakReference.get() != null){
                rawAudioPlayerWeakReference.get().onPlaybackStart();
            }
            mAudioTrack.play();
            isPlaying = true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mAudioTrack.stop();
            mAudioTrack.release();

            if (rawAudioPlayerWeakReference != null && rawAudioPlayerWeakReference.get() != null){
                if (mLastPlayedPos + headerLength >= mEndPos){
                    rawAudioPlayerWeakReference.get().onPlaybackComplete();
                } else {
                    rawAudioPlayerWeakReference.get().onPlaybackStopped();
                }

            }
        }

        @Override
        protected void onProgressUpdate(Long... values){
            if (isPlaying && rawAudioPlayerWeakReference != null && rawAudioPlayerWeakReference.get() != null){
                rawAudioPlayerWeakReference.get().setCurrentProgress(values[0]);
            }

        }

        public void setBounds(long startPos, long endPos) {
            mStartPos = startPos;
            mEndPos = endPos;
        }
    }


}
