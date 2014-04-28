package com.soundcloud.android.creators.record;

import android.util.FloatMath;

import java.nio.ByteBuffer;

public class JavaAmplitudeAnalyzer implements AmplitudeAnalyzer {
    private static final float MAX_ADJUSTED_AMPLITUDE = (float) FloatMath.sqrt(FloatMath.sqrt(32768f));

    private final AudioConfig config;

    private int lastMax;
    private int lastValue;
    private int currentAdjustedMaxAmplitude;

    public JavaAmplitudeAnalyzer(AudioConfig config) {
        this.config = config;
    }

    public float frameAmplitude(ByteBuffer buffer, int length) {
        int max = getMax(buffer, length);

        lastValue = buffer.getShort(length-2);

        // max amplitude returns false 0's sometimes, so just
        // use the last value. It is usually only for a frame
        if (max == 0) {
            max = lastMax;
        } else {
            lastMax = max;
        }

        // Simple peak follower, cf. http://www.musicdsp.org/showone.php?id=19
        if (max >= currentAdjustedMaxAmplitude) {
            /* When we hit a peak, ride the peak to the top. */
            currentAdjustedMaxAmplitude = max;
        } else {
            /*  decay of output when signal is low. */
            currentAdjustedMaxAmplitude *= 0.6;
        }

        return Math.max(.1f,
                ((float) FloatMath.sqrt(FloatMath.sqrt(currentAdjustedMaxAmplitude))) / MAX_ADJUSTED_AMPLITUDE);
    }

    public int getLastValue() {
        return lastValue;
    }

    private int getMax(ByteBuffer buffer, int length) {
        buffer.rewind();
        int max = 0;
        while (buffer.position() < length) {
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
            if (value > max) max = value;
        }
        buffer.rewind();
        return max;
    }
}
