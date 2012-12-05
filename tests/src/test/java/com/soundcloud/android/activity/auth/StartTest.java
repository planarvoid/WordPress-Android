package com.soundcloud.android.activity.auth;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class StartTest {
    @Test
    public void shouldWriteSignupLog() throws Exception {
        long[] toWrite = new long[]{1L, 2L};
        expect(Onboard.writeLog(toWrite)).toBeTrue();

        final long[] signups = Onboard.readLog();
        expect(Arrays.equals(toWrite, signups)).toBeTrue();
    }

    @Test
    public void shouldWriteNewSignupToLog() throws Exception {
        long now = System.currentTimeMillis();
        expect(Onboard.writeNewSignupToLog(now)).toBeTrue();

        final long[] signups = Onboard.readLog();
        expect(Arrays.equals(new long[]{now}, signups)).toBeTrue();
    }

    @Test
    public void shouldNotThrottle() throws Exception {
        long now = System.currentTimeMillis();
        expect(Onboard.writeNewSignupToLog(now - Onboard.THROTTLE_WINDOW)).toBeTrue();
        expect(Onboard.shouldThrottleSignup()).toBeFalse();

        expect(Onboard.writeNewSignupToLog(now)).toBeTrue();
        expect(Onboard.shouldThrottleSignup()).toBeFalse();

        expect(Onboard.writeNewSignupToLog(now)).toBeTrue();
        expect(Onboard.shouldThrottleSignup()).toBeFalse();
    }

    @Test
    public void shouldThrottle() throws Exception {
        long now = System.currentTimeMillis();
        expect(Onboard.writeNewSignupToLog(now)).toBeTrue();
        expect(Onboard.writeNewSignupToLog(now)).toBeTrue();
        expect(Onboard.writeNewSignupToLog(now)).toBeTrue();
        expect(Onboard.shouldThrottleSignup()).toBeTrue();
    }
}
