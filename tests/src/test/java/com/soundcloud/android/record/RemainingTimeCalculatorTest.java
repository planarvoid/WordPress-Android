package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowStatFs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Environment;

@RunWith(DefaultTestRunner.class)
public class RemainingTimeCalculatorTest {

    @Test
    public void remainingTimeShouldBeZeroIfNoExternalStorage() throws Exception {
        RemainingTimeCalculator c = new RemainingTimeCalculator(44100*2);
        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 1024, 1024, 1024);
        expect(c.timeRemaining()).toEqual(0L);
    }

    @Test
    public void shouldCalculateRemainingTime() throws Exception {
        TestHelper.enableSDCard();

        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 1024, 0, 0);
        RemainingTimeCalculator c = new RemainingTimeCalculator(44100*2);

        expect(c.timeRemaining()).toEqual(0L);

        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 1024, 1024, 1024);
        expect(c.timeRemaining()).toEqual(42L);
    }

    @Test
    public void shouldDetermineIfDiskspaceIsLeft() throws Exception {
        TestHelper.enableSDCard();

        RemainingTimeCalculator c = new RemainingTimeCalculator(44100*2);
        expect(c.isDiskSpaceAvailable()).toBeFalse();
        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 1024, 1024, 1024);
        expect(c.isDiskSpaceAvailable()).toBeTrue();
    }
}
