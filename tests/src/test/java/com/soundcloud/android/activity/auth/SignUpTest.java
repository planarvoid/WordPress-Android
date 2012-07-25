package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class SignUpTest {

    @Test
    public void shouldCheckpasswordLength() throws Exception {
        expect(SignUp.checkPassword("1234")).toBeTrue();
        expect(SignUp.checkPassword("123")).toBeFalse();
        expect(SignUp.checkPassword(null)).toBeFalse();
    }

    @Test
    public void shouldWriteSignupLog() throws Exception {
        long[] toWrite = new long[]{1L, 2L};
        expect(SignUp.writeLog(toWrite)).toBeTrue();

        final long[] signups = SignUp.readLog();
        expect(Arrays.equals(toWrite, signups)).toBeTrue();
    }

    @Test
    public void shouldWriteNewSignupToLog() throws Exception {
        long now = System.currentTimeMillis();
        expect(SignUp.writeNewSignupToLog(now)).toBeTrue();

        final long[] signups = SignUp.readLog();
        expect(Arrays.equals(new long[]{now}, signups)).toBeTrue();
    }

    @Test
    public void shouldNotThrottle() throws Exception {
        long now = System.currentTimeMillis();
        expect(SignUp.writeNewSignupToLog(now - SignUp.THROTTLE_WINDOW)).toBeTrue();
        expect(SignUp.shouldThrottle(Robolectric.application)).toBeFalse();

        expect(SignUp.writeNewSignupToLog(now)).toBeTrue();
        expect(SignUp.shouldThrottle(Robolectric.application)).toBeFalse();

        expect(SignUp.writeNewSignupToLog(now)).toBeTrue();
        expect(SignUp.shouldThrottle(Robolectric.application)).toBeFalse();
    }

    @Test
    public void shouldThrottle() throws Exception {
        long now = System.currentTimeMillis();
        expect(SignUp.writeNewSignupToLog(now)).toBeTrue();
        expect(SignUp.writeNewSignupToLog(now)).toBeTrue();
        expect(SignUp.writeNewSignupToLog(now)).toBeTrue();
        expect(SignUp.shouldThrottle(Robolectric.application)).toBeTrue();
    }
}
