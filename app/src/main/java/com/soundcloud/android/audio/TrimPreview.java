package com.soundcloud.android.audio;

import com.soundcloud.android.record.SoundRecorder;

public class TrimPreview {
    public static long MAX_PREVIEW_DURATION = 500; // ms, max length of each preview chunk
    public static long PREVIEW_FADE_LENGTH  = 30; // ms

    PlaybackStream mStream;
    long startPos;
    long endPos;
    public long duration;
    public int playbackRate;

    public TrimPreview(PlaybackStream stream, long startPosition, long endPosition, long moveTime) {
        this(stream,startPosition, endPosition, moveTime, SoundRecorder.MAX_PLAYBACK_RATE);
    }
    public TrimPreview(PlaybackStream stream, long startPosition, long endPosition, long moveTime, int maxPlaybackRate) {
        mStream = stream;
        startPos = startPosition;
        endPos = endPosition;
        duration = moveTime;

        final AudioConfig config = stream.getConfig();
        final long byteRange = getByteRange(config);
        playbackRate = (int) (byteRange * (1000f / duration)) / config.sampleSize;

        if (playbackRate > maxPlaybackRate) {
            // we are bound by a maximum playback rate of audiotrack.
            // if this preview is too quick, we have to adjust it to fit the max sample rate, and Adjust the duration accordingly
            playbackRate = maxPlaybackRate;
            duration = (long) (1000f / (((float) (playbackRate * config.sampleSize)) / byteRange));
        }

        if (duration > MAX_PREVIEW_DURATION) {
            // we want a preview length that will not clog up the queue for too long, so if it is too long
            // just truncate it so it represents the last MAX_PREVIEW_DURATION of the users movement
            duration = MAX_PREVIEW_DURATION;

            final long newRange = config.bytesToMs((long) ((playbackRate * config.sampleSize)/(1000f / duration)));

            if (isReverse()) {
                startPos = endPos + newRange;
            } else {
                startPos = endPos - newRange;
            }
        }
    }

    public long lowPos(AudioConfig config) {
        return config.validBytePosition(Math.min(startPos, endPos));
    }

    public long getByteRange(AudioConfig config) {
        return config.msToByte((int) Math.abs(endPos - startPos));
    }

    public boolean isReverse() {
        return startPos > endPos;
    }

    @Override
    public String toString() {
        return "TrimPreview{" +
                "mStream=" + mStream +
                ", startPos=" + startPos +
                ", endPos=" + endPos +
                ", duration=" + duration +
                ", playbackRate=" + playbackRate +
                '}';
    }

    /**
     * get a fade out filter adjusted for this preview's playback rate
     */
    public FadeFilter getFadeFilter() {
        return new FadeFilter(
                FadeFilter.FADE_TYPE_END,
                AudioConfig.msToByte(
                        PREVIEW_FADE_LENGTH,
                        playbackRate,
                        mStream.getConfig().sampleSize)
        );
    }
}
