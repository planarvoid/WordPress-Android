package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class AmplitudeDataTest {
    private AmplitudeData data;

    @Before
    public void before() {
        data = new AmplitudeData();
    }

    @Test
    public void testAdd() throws Exception {
        expect(data.size()).toEqual(0);
        data.add(12f);
        expect(data.size()).toEqual(1);
    }

    @Test
    public void testAddArray() throws Exception {
        data.add(1, 2, 3);
        expect(data.size()).toEqual(3);
        expect(data.get(0)).toEqual(1f);
        expect(data.get(1)).toEqual(2f);
        expect(data.get(2)).toEqual(3f);
    }

    @Test
    public void testSize() throws Exception {
        expect(data.size()).toEqual(0);
    }

    @Test
    public void testIsEmpty() throws Exception {
        expect(data.isEmpty()).toBeTrue();
        data.add(1f);
        expect(data.isEmpty()).toBeFalse();
    }


    @Test
    public void testGet() throws Exception {
        data.add(1f);
        data.add(2f);
        data.add(3f);
        expect(data.get(0)).toEqual(1f);
        expect(data.get(1)).toEqual(2f);
        expect(data.get(2)).toEqual(3f);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowIndexOutOfBoundsExceptionIfIndexOutOfBounds() throws Exception {
        data.get(0);
    }

    @Test
    public void shouldClear() throws Exception {
        data.add(1f);
        data.add(2f);
        data.add(3f);
        expect(data.size()).toEqual(3);
        data.clear();
        expect(data.size()).toEqual(0);
    }

    @Test
    public void shouldSlice() throws Exception {
        data.add(1, 2, 3, 4, 5);
        AmplitudeData slice = data.slice(1, 2);
        expect(slice.size()).toEqual(2);
        expect(slice.get(0)).toEqual(2f);
        expect(slice.get(1)).toEqual(3f);
    }

    @Test
    public void shouldBeIterable() throws Exception {
        final float[] samples = {1, 2, 3, 4, 5};
        data.add(samples);

        int i = 0;
        for (float f : data) {
            expect(f).toEqual(samples[i++]);
        }
    }

    @Test
    public void shouldGrowArrayAutomatically() throws Exception {
        AmplitudeData d = new AmplitudeData(2);
        d.add(0f);
        d.add(1f);
        d.add(2f);
        d.add(3f);
        expect(d.size()).toEqual(4);
    }

    @Test
    public void shouldCutRight() throws Exception {
        AmplitudeData d = new AmplitudeData(2);
        d.add(0, 1, 2, 3, 4);
        expect(d.size()).toEqual(5);
        d.truncate(2);

        expect(d.size()).toEqual(2);
        expect(Arrays.equals(d.get(), new float[] { 0, 1 } )).toBeTrue();
        d.add(2f, 3f);
        expect(Arrays.equals(d.get(), new float[] { 0, 1, 2, 3  } )).toBeTrue();

        d.truncate(1);
        expect(Arrays.equals(d.get(), new float[] { 0 } )).toBeTrue();
    }

    @Test
    @Ignore
    public void shouldParcelAndUnparcelAmplitudeData() throws Exception {
        data.add(1, 2, 3, 4, 5);
        Parcel parcel = Parcel.obtain();

        data.writeToParcel(parcel, 0);

        AmplitudeData unparceled = new AmplitudeData(parcel);
        expect(unparceled).toEqual(data);
    }
}
