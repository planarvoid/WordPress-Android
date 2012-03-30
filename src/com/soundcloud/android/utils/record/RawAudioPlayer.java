package com.soundcloud.android.utils.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
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
    public static final int TRIM_PREVIEW_LENGTH = 500;

    private PlayRawAudioTask mPlayRawAudioTask;
    private File mFile;
    private long mCurrentProgress, mTotalBytes, mStartPos, mEndPos, mNextSeek, mLastSeekAt, mDuration;
    private int mMinBufferSize;
    private boolean mPlaying;
    private PlaybackListener mListener;

    public RawAudioPlayer() {
        mMinBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
    }

    public long getDuration() {
        return mDuration;
    }

    public long getCurrentPlaybackPosition() {
        return mCurrentProgress == -1 ? -1 : PcmUtils.byteToMs(mCurrentProgress);
    }

    public void resetPlaybackBounds() {
        mStartPos = 0;
        mEndPos = mTotalBytes;
    }

    public interface PlaybackListener {
        void onPlaybackStart();
        void onPlaybackStopped();
        void onPlaybackComplete();
        void onPlaybackError();
    }

    public void setListener(PlaybackListener listener){
        mListener = listener;
    }

    public void setFile(File f) throws IOException {
        if (mPlaying) stop();

        mFile = f;
        FileInputStream fin = new FileInputStream(f);
        WaveHeader waveHeader = new WaveHeader(fin);
        if (mTotalBytes != waveHeader.getNumBytes() ) {
            mTotalBytes = waveHeader.getNumBytes();
            resetPlaybackBounds();
        }
        mDuration = PcmUtils.byteToMs(mEndPos); //ms, sample rate * 2 bytes per sample * 2 channels
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

    public void play() {
        if (!mPlaying) {
            mPlaying = true;
            mPlayRawAudioTask = new PlayRawAudioTask(this, mFile, SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
            mPlayRawAudioTask.setBounds(mStartPos,mEndPos);
            mPlayRawAudioTask.execute(mCurrentProgress);
        }
    }

    public void stop(){
        stopPlayback();
        mCurrentProgress = -1;
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

    private void doSeek() {
        mCurrentProgress = mNextSeek;
        mLastSeekAt = System.currentTimeMillis();
        if (mPlaying) {
            // TODO, stop and start just to seek. this is lazy
            stopPlayback();
            play();
        }
    }

    private void onPlaybackStart(PlayRawAudioTask task) {
        if (mPlayRawAudioTask == task) {
            mPlaying = true;
            if (mListener != null) {
                mListener.onPlaybackStart();
            }
        }

    }

    private void onPlaybackStopped(PlayRawAudioTask task) {
        if (mPlayRawAudioTask == task) {
            mPlaying = false;
            if (mListener != null) {
                mListener.onPlaybackStopped();
            }
        }
    }

    private void onPlaybackComplete(PlayRawAudioTask task) {
        if (mPlayRawAudioTask == task) {
            mPlaying = false;
            mCurrentProgress = -1;
            if (mListener != null) {
                mListener.onPlaybackComplete();
            }
        }
    }

    public void onNewStartPosition(float percent) {
        mStartPos = getValidBytePos((long) (percent * mTotalBytes));
        if (mPlaying){
            seekTo(mStartPos);
        }
    }

    public void onNewEndPosition(float percent) {
        mEndPos = getValidBytePos((long) (percent * mTotalBytes));
        if (mPlaying){
            seekTo(Math.max(mStartPos, mEndPos - PcmUtils.msToByte(TRIM_PREVIEW_LENGTH)));
        }
    }

    private long getValidBytePos(long in){
        return (in / mMinBufferSize) * mMinBufferSize;
    }

    private static class PlayRawAudioTask extends AsyncTask<Long, Long, Boolean> {
        private File mFileToPlay;
        private AudioTrack mAudioTrack;
        private int minSize;
        private boolean isPlaying;
        private long mEndPos, mStartPos, mLastPlayedPos;
        private WeakReference<RawAudioPlayer> rawAudioPlayerWeakReference;

        public PlayRawAudioTask(RawAudioPlayer rawAudioPlayer, File f, int sampleRate, int channelConfiguration, int encoding) {
            if (f == null) throw new IllegalArgumentException("file is null");

            mFileToPlay = f;
            minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfiguration, encoding);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfiguration, encoding, minSize, AudioTrack.MODE_STREAM);
            rawAudioPlayerWeakReference = new WeakReference<RawAudioPlayer>(rawAudioPlayer);
        }

        public void stop(){
            isPlaying = false;
        }

        @Override
        protected Boolean doInBackground(Long... params) {
            long offset = Math.max(params[0], mStartPos);
            Log.d("RawAudioPlayer", "playing "+mFileToPlay+ " at offset "+offset);

            int bufferSize = 1024;
            int i;
            byte[] s = new byte[bufferSize];
            try {
                FileInputStream fin = new FileInputStream(mFileToPlay);
                // round to the nearest buffer size to ensure valid audio data (TODO can this be more precise?)
                offset = (offset / minSize) * minSize;

                DataInputStream dis = new DataInputStream(fin);
                dis.skip(WaveHeader.HEADER_LENGTH + offset);

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
                rawAudioPlayerWeakReference.get().onPlaybackStart(this);
            }
            mAudioTrack.play();
            isPlaying = true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mAudioTrack.stop();
            mAudioTrack.release();

            if (rawAudioPlayerWeakReference != null && rawAudioPlayerWeakReference.get() != null){
                if (mLastPlayedPos + WaveHeader.HEADER_LENGTH >= mEndPos){
                    rawAudioPlayerWeakReference.get().onPlaybackComplete(this);
                } else {
                    rawAudioPlayerWeakReference.get().onPlaybackStopped(this);
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
