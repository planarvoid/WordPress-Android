package com.soundcloud.android.audio;

import com.soundcloud.android.utils.IOUtils;


import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlaybackStream {
    private static final int TRIM_PREVIEW_LENGTH = 500;

    private long currentPosition;
    private long startPosition;
    private long endPosition;
    public PlaybackFilter filter;

    private AudioConfig mConfig;
    private AudioFile mPlaybackFile;


    public PlaybackStream(AudioFile audioFile, AudioConfig config) throws IOException {
        mPlaybackFile = audioFile;
        resetBounds();
        currentPosition = -1;

        mConfig = config;
    }

    public void reset() {
        resetBounds();
        filter = null;
    }

    public void resetBounds() {
        startPosition = 0;
        endPosition = mPlaybackFile.getDuration();
    }

    public long getDuration(){
        return endPosition - startPosition;
    }

    public long getPosition() {
        return currentPosition - startPosition;
    }

    public long setStartPositionByPercent(double percent) {
        startPosition = (long) (percent * mPlaybackFile.getDuration());
        return startPosition;
    }

    public long setEndPositionByPercent(double percent) {
        endPosition = (long) (percent * mPlaybackFile.getDuration());
        return Math.max(startPosition, endPosition - TRIM_PREVIEW_LENGTH);
    }

    public int read(ByteBuffer buffer, int bufferSize) throws IOException {
        if (currentPosition < endPosition) {

            final int n = mPlaybackFile.read(buffer, bufferSize);
            buffer.flip();
            if (filter != null) {
                filter.apply(buffer, mConfig.msToByte(currentPosition - startPosition), mConfig.msToByte(endPosition - startPosition));
            }

            currentPosition = mPlaybackFile.getPosition();
            return n;
        }
        return -1;
    }

    public boolean isFinished() {
        return currentPosition >= endPosition;
    }

    public void resetPlayback() {
        currentPosition = -1;
    }

    public void initializePlayback() throws IOException {
        currentPosition = getValidPosition(currentPosition);
        mPlaybackFile.seek(currentPosition);

    }

    public long getValidPosition(long currentPosition) {
            return (currentPosition < startPosition || currentPosition >= endPosition) ? startPosition : currentPosition;
        }

    public void close() {
        IOUtils.close(mPlaybackFile);
        mPlaybackFile = null;
    }

    public void setCurrentPosition(long pos) {
        currentPosition = pos;
    }

    public void reopen() {
        try {
            mPlaybackFile.reopen();
            mPlaybackFile.seek(currentPosition);
        } catch (IOException e) {
            Log.w("asdf", e);
        }
    }
}
