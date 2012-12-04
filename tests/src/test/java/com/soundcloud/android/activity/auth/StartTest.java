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
        expect(Start.writeLog(toWrite)).toBeTrue();

        final long[] signups = Start.readLog();
        expect(Arrays.equals(toWrite, signups)).toBeTrue();
    }

    @Test
    public void shouldWriteNewSignupToLog() throws Exception {
        long now = System.currentTimeMillis();
        expect(Start.writeNewSignupToLog(now)).toBeTrue();

        final long[] signups = Start.readLog();
        expect(Arrays.equals(new long[]{now}, signups)).toBeTrue();
    }

    @Test
    public void shouldNotThrottle() throws Exception {
        long now = System.currentTimeMillis();
        expect(Start.writeNewSignupToLog(now - Start.THROTTLE_WINDOW)).toBeTrue();
        expect(Start.shouldThrottleSignup()).toBeFalse();

        expect(Start.writeNewSignupToLog(now)).toBeTrue();
        expect(Start.shouldThrottleSignup()).toBeFalse();

        expect(Start.writeNewSignupToLog(now)).toBeTrue();
        expect(Start.shouldThrottleSignup()).toBeFalse();
    }

    @Test
    public void shouldThrottle() throws Exception {
        long now = System.currentTimeMillis();
        expect(Start.writeNewSignupToLog(now)).toBeTrue();
        expect(Start.writeNewSignupToLog(now)).toBeTrue();
        expect(Start.writeNewSignupToLog(now)).toBeTrue();
        expect(Start.shouldThrottleSignup()).toBeTrue();
    }
}
