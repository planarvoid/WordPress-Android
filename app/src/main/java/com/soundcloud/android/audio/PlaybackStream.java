package com.soundcloud.android.audio;

import com.soundcloud.android.utils.IOUtils;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlaybackStream {
    private static final int TRIM_PREVIEW_LENGTH = 500;

    private long mCurrentPos;
    private long mStartPos;
    private long mEndPos;

    private AudioConfig mConfig;
    private AudioFile mPlaybackFile;

    private PlaybackFilter mFilter;
    private boolean mOptimize;

    public PlaybackStream(AudioFile audioFile) throws IOException {
        mPlaybackFile = audioFile;
        mConfig = audioFile.getConfig();
        resetBounds();
        mCurrentPos = -1;
    }

    public void reset() {
        resetBounds();
        mFilter = null;
        mOptimize = false;
    }

    public void resetBounds() {
        mStartPos = 0;
        mEndPos   = mPlaybackFile.getDuration();
    }

    public long getDuration(){
        return mEndPos - mStartPos;
    }

    public long getPosition() {
        return Math.max(0,mCurrentPos - mStartPos);
    }

    public AudioConfig getConfig() {
        return mConfig;
    }

    public TrimPreview setStartPositionByPercent(double newPos, long moveTime) {
        if (newPos < 0d || newPos > 1d) throw new IllegalArgumentException("Illegal start percent " + newPos);

        final long old = mStartPos;
        mStartPos = (long) (newPos * mPlaybackFile.getDuration());
        return new TrimPreview(this,old,mStartPos, moveTime);
    }

    public TrimPreview setEndPositionByPercent(double newPos, long moveTime) {
        if (newPos < 0d || newPos > 1d) throw new IllegalArgumentException("Illegal end percent " + newPos);

        final long old = mEndPos;
        mEndPos = (long) (newPos * mPlaybackFile.getDuration());
        return new TrimPreview(this, old, mEndPos, moveTime);
    }

    public int read(ByteBuffer buffer, int bufferSize) throws IOException {
        final int n = mPlaybackFile.read(buffer, bufferSize);
        buffer.flip();
        return n;
    }

    public int readForPlayback(ByteBuffer buffer, int bufferSize) throws IOException {
        if (mCurrentPos < mEndPos) {
            final int n = mPlaybackFile.read(buffer, bufferSize);
            buffer.flip();
            if (mFilter != null) {
                mFilter.apply(buffer, mConfig.msToByte(mCurrentPos - mStartPos), mConfig.msToByte(mEndPos - mStartPos));
            }
            mCurrentPos = mPlaybackFile.getPosition();
            return n;
        }
        return -1;
    }

    public boolean isFinished() {
        return mCurrentPos >= mEndPos;
    }

    public void resetPlayback() {
        mCurrentPos = -1;
    }

    public void initializePlayback() throws IOException {
        mCurrentPos = getValidPosition(mCurrentPos);
        initializePlayback(mCurrentPos);
    }

    public void initializePlayback(long atPosition) throws IOException {
        mPlaybackFile.seek(atPosition);
    }


    public long getValidPosition(long currentPosition) {
        return (currentPosition < mStartPos || currentPosition >= mEndPos) ? mStartPos : currentPosition;
    }

    public void close() {
        IOUtils.close(mPlaybackFile);
        mPlaybackFile = null;
    }

    public void setCurrentPosition(long pos) {
        mCurrentPos = pos;
    }

    public void reopen() {
        try {
            mPlaybackFile.reopen();
            mPlaybackFile.seek(mCurrentPos);
        } catch (IOException e) {
            Log.w(PlaybackStream.class.getSimpleName(), e);
        }
    }

    public long getStartPos() {
        return mStartPos;
    }

    public long getEndPos() {
        return mEndPos;
    }

    public boolean isOptimized() {
        return mOptimize;
    }

    public boolean isFading() {
        return mFilter instanceof FadeFilter;
    }

    public void setFading(boolean enabled) {
        mFilter = enabled ? new FadeFilter(mPlaybackFile.getConfig()) : null;
    }

    public void setOptimize(boolean enabled) {
        mOptimize = enabled;
    }

    public void setTrim(long start, long end) {
        mStartPos = start;
        mEndPos = end;
    }

}
