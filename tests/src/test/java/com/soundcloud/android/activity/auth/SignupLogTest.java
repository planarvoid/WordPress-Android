package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(DefaultTestRunner.class)
public class SignupLogTest {
    @Test
    public void shouldWriteSignupLog() throws Exception {
        long[] toWrite = new long[]{1L, 2L};
        expect(SignupLog.writeLog(toWrite)).toBeTrue();

        final long[] signups = SignupLog.readLog();
        expect(Arrays.equals(toWrite, signups)).toBeTrue();
    }

    @Test
    public void shouldWriteNewSignupToLog() throws Exception {
        long now = System.currentTimeMillis();
        expect(SignupLog.writeNewSignup(now)).toBeTrue();

        final long[] signups = SignupLog.readLog();
        expect(Arrays.equals(new long[]{now}, signups)).toBeTrue();
    }

    @Test
    public void shouldNotThrottleSignupForAFewAttempts() throws Exception {
        long now = System.currentTimeMillis();
        expect(SignupLog.writeNewSignup(now - SignupLog.THROTTLE_WINDOW)).toBeTrue();
        expect(SignupLog.shouldThrottleSignup()).toBeFalse();

        expect(SignupLog.writeNewSignup(now)).toBeTrue();
        expect(SignupLog.shouldThrottleSignup()).toBeFalse();

        expect(SignupLog.writeNewSignup(now)).toBeTrue();
        expect(SignupLog.shouldThrottleSignup()).toBeFalse();
    }

    @Test
    public void shouldThrottleSignupAfterTooManyAttempts() throws Exception {
        long now = System.currentTimeMillis();

        for (int i = 0; i < SignupLog.THROTTLE_AFTER_ATTEMPT - 1; i++) {
            expect(SignupLog.writeNewSignup(now)).toBeTrue();
            expect(SignupLog.shouldThrottleSignup()).toBeFalse();
        }
        expect(SignupLog.writeNewSignup(now)).toBeTrue();
        expect(SignupLog.shouldThrottleSignup()).toBeTrue();
    }
}
