package com.soundcloud.android.waveform;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.java.objects.MoreObjects;

import java.util.Arrays;

/**
 * Waveform sample data.
 * <p/>
 * <pre>
 *  $ curl 'http://wis.sndcdn.com/H9uGzKOYK5Ph_m.png'
 *  {
 *      "width": 1800,
 *      "height": 140,
 *      "samples": [3,6,7,8,24,63,130,...]
 *  }
 * </pre>
 *
 * @see <a href="https://github.com/soundcloud/waveform-image-samples">Waveform Image Samples</a>
 */
public class WaveformData {
    public final static WaveformData EMPTY = new WaveformData(-1, new int[0]);

    public final int maxAmplitude;
    public final int[] samples;

    public WaveformData(int height, int[] samples) {
        this.maxAmplitude = height;
        this.samples = samples;
    }

    /**
     * @param requiredWidth the new width
     * @return the waveform data downsampled to the required width
     */
    public WaveformData scale(double requiredWidth) {
        checkArgument(requiredWidth >= 0, "invalid width");

        double samplesPerWidth = samples.length / requiredWidth;
        int totalSamples = (int) Math.ceil(requiredWidth);

        if (requiredWidth == samples.length) {
            return this;
        } else {
            int[] newSamples = new int[totalSamples];
            int newMax = 0, j, sampleIndex;
            double acc, first, last, numberOfSamples, samplePercentage;

            for (int i = 0; i < totalSamples; i++) {
                first = samplesPerWidth * i;
                last = samplesPerWidth * (i + 1);

                sampleIndex = (int) first;
                samplePercentage = (1 - (first - sampleIndex));
                numberOfSamples = samplePercentage;

                // Add partial start
                acc = samples[sampleIndex] * samplePercentage;

                // Add full values
                for (j = sampleIndex + 1; j < (int) last && j < samples.length; j++) {
                    acc += samples[j];
                    numberOfSamples++;
                }

                // add partial end
                if (last < samples.length) {
                    sampleIndex = (int) last;
                    samplePercentage = last - sampleIndex;
                    acc += samples[sampleIndex] * samplePercentage;
                    numberOfSamples += samplePercentage;
                }

                // set average value
                newSamples[i] = (int) Math.round(acc / numberOfSamples);

                if (newSamples[i] > newMax) {
                    newMax = newSamples[i];
                }
            }
            return new WaveformData(newMax, newSamples);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WaveformData that = (WaveformData) o;

        return MoreObjects.equal(maxAmplitude, that.maxAmplitude)
                && Arrays.equals(samples, that.samples);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(maxAmplitude) + Arrays.hashCode(samples);
    }

}
