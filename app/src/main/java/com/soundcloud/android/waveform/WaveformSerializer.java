package com.soundcloud.android.waveform;

import javax.inject.Inject;

public class WaveformSerializer {

    @Inject
    public WaveformSerializer() {
        //no-op required by dagger
    }


    public int[] deserialize(String serializedSamples) throws NumberFormatException {
        final String[] array = serializedSamples.split(",");
        int[] samples = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            samples[i] = Integer.parseInt(array[i]);
        }
        return samples;
    }

    public String serialize(int[] samples) {
        final StringBuilder sb = new StringBuilder();
        boolean firstTime = true;

        for (int token : samples) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(',');
            }
            sb.append(token);
        }
        return sb.toString();
    }
}
