package com.soundcloud.android.task.create;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.fail;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class CalculateAmplitudesTaskTest  {

    @Test
    public void taskShouldCalculateAmplitudesShort() throws Exception {
        long start = System.currentTimeMillis();

        // short (~.5mb)
        CalculateAmplitudesTask cat = new CalculateAmplitudesTask(new File(getClass().getResource("short_test.wav").getFile()),800);

        final List<double[]> results = new ArrayList<double[]>();
        final CalculateAmplitudesTask.CalculateAmplitudesListener listener = new CalculateAmplitudesTask.CalculateAmplitudesListener() {
            @Override
            public void onSuccess(File f, double[] amplitudes, double sampleMax) {
                results.add(amplitudes);
                results.add(new double[]{sampleMax});
            }

            @Override
            public void onError(File f) {
                fail("onError");
            }
        };
        cat.addListener(listener);
        cat.execute();
        expect(results.size()).toEqual(2);
        expect(results.get(1)[0]).toEqual(3363.483922829582d);
    }

    @Test
    public void taskShouldCalculateAmplitudesMedium() throws Exception {
        long start = System.currentTimeMillis();

        // med (~3.6mb)
        CalculateAmplitudesTask cat = new CalculateAmplitudesTask(new File(getClass().getResource("med_test.wav").getFile()),800);

        final List<double[]> results = new ArrayList<double[]>();
        final CalculateAmplitudesTask.CalculateAmplitudesListener listener = new CalculateAmplitudesTask.CalculateAmplitudesListener() {
            @Override
            public void onSuccess(File f, double[] amplitudes, double sampleMax) {
                results.add(amplitudes);
                results.add(new double[]{sampleMax});
            }

            @Override
            public void onError(File f) {
                fail("onError");
            }
        };
        cat.addListener(listener);
        cat.execute();
        expect(results.size()).toEqual(2);
        expect(results.get(1)[0]).toEqual(5132.169459071326d);
    }
}
