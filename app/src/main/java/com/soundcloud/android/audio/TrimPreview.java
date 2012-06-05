package com.soundcloud.android.audio;

import com.soundcloud.android.record.SoundRecorder;

import android.util.Log;

public class TrimPreview {

    public static long MAX_PREVIEW_DURATION = 500; // ms, max length of each preview chunk

    PlaybackStream mStream;
    long mStartPos;
    long mEndPos;
    public long duration;
    public long playbackRate;

    public TrimPreview(PlaybackStream stream, long startPosition, long endPosition, long moveTime) {
        mStream = stream;
        mStartPos = startPosition;
        mEndPos = endPosition;
        duration = moveTime;

        final AudioConfig config = stream.getConfig();
        final long byteRange = getByteRange(config);
        playbackRate = (int) (byteRange * (1000f / duration)) / config.sampleSize;

        if (playbackRate > SoundRecorder.MAX_PLAYBACK_RATE) {
            // we are bound by a maximum playback rate of audiotrack.
            // if this preview is too quick, we have to adjust it to fit the max sample rate, and Adjust the duration accordingly
            playbackRate = SoundRecorder.MAX_PLAYBACK_RATE;
            duration = (long) (1000f / (((float) (playbackRate * config.sampleSize)) / byteRange));
        }

        if (duration > MAX_PREVIEW_DURATION) {
            // we want a preview length that will not clog up the queue for too long, so if it is too long
            // just truncate it so it represents the last MAX_PREVIEW_DURATION of the users movement
            duration = MAX_PREVIEW_DURATION;
            if (isReverse()) {
                mStartPos = mEndPos + MAX_PREVIEW_DURATION;
            } else {
                mStartPos = mEndPos - MAX_PREVIEW_DURATION;
            }
        }
    }

    public long lowPos(AudioConfig config) {
        return config.validBytePosition(Math.min(mStartPos, mEndPos));
    }

    public long getByteRange(AudioConfig config) {
        return config.msToByte((int) Math.abs(mEndPos - mStartPos));
    }

    public boolean isReverse() {
        return mStartPos > mEndPos;
    }

    @Override
    public String toString() {
        return "TrimPreview{" +
                "mStream=" + mStream +
                ", mStartPos=" + mStartPos +
                ", mEndPos=" + mEndPos +
                ", duration=" + duration +
                ", playbackRate=" + playbackRate +
                '}';
    }
}
