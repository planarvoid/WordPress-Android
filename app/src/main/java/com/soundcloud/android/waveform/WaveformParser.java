package com.soundcloud.android.waveform;

import com.soundcloud.android.utils.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

class WaveformParser {

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SAMPLES = "samples";

    @Inject
    public WaveformParser() {}

    public WaveformData parse(InputStream data) throws JSONException, IOException {
        final JSONObject obj = new JSONObject(IOUtils.readInputStream(data));
        final int width = obj.getInt(WIDTH);
        final int height = obj.getInt(HEIGHT);
        final int[] samples = new int[width];
        final JSONArray sampleArray = obj.getJSONArray(SAMPLES);

        if (sampleArray == null || sampleArray.length() == 0) {
            throw new IOException("no samples provided");
        }

        if (sampleArray.length() != width) {
            throw new IOException("incomplete sample data");
        }

        for (int i = 0; i < width; i++) {
            double value =  Math.pow(sampleArray.getDouble(i) / height, 1.5);
            samples[i] = (int) (height * value);
        }

        return new WaveformData(height, samples);
    }
}
