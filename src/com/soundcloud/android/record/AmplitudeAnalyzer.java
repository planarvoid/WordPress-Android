package com.soundcloud.android.record;

import java.nio.ByteBuffer;

public class AmplitudeAnalyzer {
    private static final float MAX_ADJUSTED_AMPLITUDE = (float) Math.sqrt(Math.sqrt(32768.0));

    private final AudioConfig config;

    private int mLastMax;
    private int mCurrentMaxAmplitude;
    private int mCurrentAdjustedMaxAmplitude;

    public AmplitudeAnalyzer(AudioConfig config) {
        this.config = config;
    }

    public float frameAmplitude() {
        int max = mCurrentMaxAmplitude;
        mCurrentMaxAmplitude = 0;

        // max amplitude returns false 0's sometimes, so just
        // use the last value. It is usually only for a frame
        if (max == 0) {
            max = mLastMax;
        } else {
            mLastMax = max;
        }

        // Simple peak follower, cf. http://www.musicdsp.org/showone.php?id=19
        if (max >= mCurrentAdjustedMaxAmplitude) {
            /* When we hit a peak, ride the peak to the top. */
            mCurrentAdjustedMaxAmplitude = max;
        } else {
            /*  decay of output when signal is low. */
            mCurrentAdjustedMaxAmplitude *= 0.4;
        }

        return Math.max(.1f,
                ((float) Math.sqrt(Math.sqrt(mCurrentAdjustedMaxAmplitude))) / MAX_ADJUSTED_AMPLITUDE);
    }

     public void updateCurrentMax(ByteBuffer buffer, int length) {
        buffer.rewind();
        buffer.limit(length);
        while (buffer.position() < buffer.limit()) {
            int value;
            switch (config.bitsPerSample) {
                case 16:
                    value = buffer.getShort();
                    break;
                case 8:
                    value = buffer.get();
                    break;
                default:
                    value = 0;
            }
            if (value > mCurrentMaxAmplitude) mCurrentMaxAmplitude = value;
        }
    }
}
