package com.soundcloud.android.record;

import com.soundcloud.android.utils.IOUtils;

import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RawAudioPlayer {
    private static final String TAG = RawAudioPlayer.class.getSimpleName();

    private static final long SEEK_INTERVAL = 200;
    public static final int TRIM_PREVIEW_LENGTH = 500;

    private PlayRawAudioTask mPlayRawAudioTask;
    private File mFile;
    private long mCurrentProgress, mTotalBytes, mStartPos, mEndPos, mNextSeek, mLastSeekAt, mDuration;
    private boolean mPlaying;
    private final AudioConfig mConfig;

    private PlaybackListener mListener;

    public RawAudioPlayer(AudioConfig config) {
        mConfig = config;
    }

    public long getDuration() {
        return mDuration;
    }

    public long getCurrentPlaybackPosition() {
        return mCurrentProgress == -1 ? -1 :  mConfig.bytesToMs(mCurrentProgress);
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
        if (!f.exists()) throw new IOException("file "+f+" does not exist");

        mFile = f;
        FileInputStream fin = new FileInputStream(f);
        WaveHeader waveHeader = new WaveHeader(fin);
        if (mTotalBytes != waveHeader.getNumBytes() ) {
            mTotalBytes = waveHeader.getNumBytes();
            resetPlaybackBounds();
        }
        mDuration =  mConfig.bytesToMs(mEndPos);
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
            mPlayRawAudioTask = new PlayRawAudioTask(this, mFile, mConfig);
            mPlayRawAudioTask.setBounds(mStartPos, mEndPos);
            mPlayRawAudioTask.execute(mCurrentProgress);
        }
    }

    public void stop(){
        stopPlayback();
        mCurrentProgress = -1;
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
        seekTo(mConfig.startPosition((long) (percentage * mTotalBytes)));
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

    public void onNewStartPosition(double percent) {
        mStartPos = mConfig.startPosition((long) (percent * mTotalBytes));
        if (mPlaying) {
            seekTo(mStartPos);
        }
    }

    public void onNewEndPosition(double percent) {
        mStartPos = mConfig.startPosition((long) (percent * mTotalBytes));
        if (mPlaying) {
            seekTo(Math.max(mStartPos, mEndPos - mConfig.msToByte(TRIM_PREVIEW_LENGTH)));
        }
    }


    private static class PlayRawAudioTask extends AsyncTask<Long, Long, Boolean> {
        private File mFileToPlay;
        private boolean isPlaying;
        private AudioTrack mAudioTrack;
        private long mEndPos, mStartPos, mLastPlayedPos;
        private RawAudioPlayer player;
        private AudioConfig mConfig;

        public PlayRawAudioTask(RawAudioPlayer rawAudioPlayer, File f, AudioConfig config) {
            if (f == null) throw new IllegalArgumentException("file is null");

            mFileToPlay = f;
            mAudioTrack = config.createAudioTrack();
            player = rawAudioPlayer;
            mConfig = config;
        }

        public void stop() {
            isPlaying = false;
        }

        @Override
        protected Boolean doInBackground(Long... params) {
            final long offset = mConfig.startPosition(Math.max(params[0], mStartPos));
            Log.d("RawAudioPlayer", "playing "+mFileToPlay+ " at offset "+offset);

            final int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            RandomAccessFile file = null;
            try {
                file = new RandomAccessFile(mFileToPlay, "r");
                final long dataStart = offset + WaveHeader.LENGTH;
                if (dataStart < file.length()) {
                    file.seek(dataStart);
                    long written = 0;
                    int n;
                    while (isPlaying && (n = file.read(buffer, 0, bufferSize)) > -1 && (written + offset < mEndPos)) {
                        written += mAudioTrack.write(buffer, 0, n);
                        publishProgress(written+offset);
                    }
                    mLastPlayedPos = written + offset;
                    return true;
                } else {
                    Log.w(TAG, "dataStart > length: "+dataStart+">"+file.length());
                    return false;
                }
            } catch (IOException e) {
                Log.w(TAG, "error during playback", e);
            } finally {
                IOUtils.close(file);
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            if (player != null){
                player.onPlaybackStart(this);
            }
            mAudioTrack.play();
            isPlaying = true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mAudioTrack.stop();
            mAudioTrack.release();

            if (player != null) {
                if (mLastPlayedPos + WaveHeader.LENGTH >= mEndPos){
                    player.onPlaybackComplete(this);
                } else {
                    player.onPlaybackStopped(this);
                }
            }
        }
        @Override
        protected void onProgressUpdate(Long... values){
            if (isPlaying && player != null){
                player.setCurrentProgress(values[0]);
            }
        }

        public void setBounds(long startPos, long endPos) {
            mStartPos = startPos;
            mEndPos = endPos;
        }
    }
}
