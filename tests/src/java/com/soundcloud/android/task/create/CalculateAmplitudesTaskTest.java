package com.soundcloud.android.task.create;

import android.location.Location;
import com.soundcloud.android.model.FoursquareVenue;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(DefaultTestRunner.class)
public class CalculateAmplitudesTaskTest extends ApiTests {

    @Test
    public void taskShouldCalculateAmplitudesShort() throws Exception {
        long start = System.currentTimeMillis();

        // short (~.5mb)
        CalculateAmplitudesTask cat = new CalculateAmplitudesTask(new File(getClass().getResource("short_test.wav").getFile()),800);

        final List<double[]> results = new ArrayList<double[]>();
        cat.addListener(new CalculateAmplitudesTask.CalculateAmplitudesListener() {
            @Override
            public void onSuccess(File f, double[] amplitudes, double sampleMax) {
                results.add(amplitudes);
                results.add(new double[]{sampleMax});
            }

            @Override
            public void onError(File f) {
            }
        });
        cat.execute();
        expect(results.size()).toBe(2);
        expect(results.get(1)[0]).toEqual(3363.483922829582);
        System.out.println("Short Analyze took " + (System.currentTimeMillis() - start) + " ms");
    }

    @Test
    public void taskShouldCalculateAmplitudesMedium() throws Exception {
        long start = System.currentTimeMillis();

        // med (~3.6mb)
        CalculateAmplitudesTask cat = new CalculateAmplitudesTask(new File(getClass().getResource("med_test.wav").getFile()),800);

        final List<double[]> results = new ArrayList<double[]>();
        cat.addListener(new CalculateAmplitudesTask.CalculateAmplitudesListener() {
            @Override
            public void onSuccess(File f, double[] amplitudes, double sampleMax) {
                results.add(amplitudes);
                results.add(new double[]{sampleMax});
            }

            @Override
            public void onError(File f) {
            }
        });
        cat.execute();
        expect(results.size()).toBe(2);
        expect(results.get(1)[0]).toEqual(4985.451978657181);
        System.out.println("Medium Analyze took " + (System.currentTimeMillis() - start) + " ms");
    }
}
