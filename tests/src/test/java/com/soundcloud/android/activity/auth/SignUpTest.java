package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.CloudUtils.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.Expect;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;


@RunWith(DefaultTestRunner.class)
public class SignUpTest {

    @Test
    public void shouldCheckEmail() throws Exception {
        assertTrue(checkEmail("foo@bar.com"));
        assertTrue(checkEmail("Foo+special@bar.com"));
        assertFalse(checkEmail("foo@barcom"));
        assertFalse(checkEmail("foobar.com"));
    }

    @Test
    public void shouldCheckpasswordLength() throws Exception {
        assertTrue(SignUp.checkPassword("1234"));
        assertFalse(SignUp.checkPassword("123"));
        assertFalse(SignUp.checkPassword(null));
    }

    @Test
    public void shouldWriteSignupLog() throws Exception {
        TestHelper.enableSDCard();
        Robolectric.application.onCreate();

        long[] toWrite = new long[]{1L, 2L};
        long now = System.currentTimeMillis();
        SignUp.writeLog(toWrite);

        final long[] signups = SignUp.readLog();
        expect(Arrays.equals(toWrite, signups)).toBeTrue();
    }

    @Test
    public void shouldWriteNewSignupToLog() throws Exception {
        TestHelper.enableSDCard();
        Robolectric.application.onCreate();

        long now = System.currentTimeMillis();
        SignUp.writeNewSignupToLog(now);

        final long[] signups = SignUp.readLog();
        expect(Arrays.equals(new long[]{now}, signups)).toBeTrue();
    }

    @Test
    public void shouldNotThrottle() throws Exception {
        TestHelper.enableSDCard();
        Robolectric.application.onCreate();

        long now = System.currentTimeMillis();
        SignUp.writeNewSignupToLog(now - SignUp.THROTTLE_WINDOW);
        expect(SignUp.shouldThrottle()).toBeFalse();

        SignUp.writeNewSignupToLog(now);
        expect(SignUp.shouldThrottle()).toBeFalse();

        SignUp.writeNewSignupToLog(now);
        SignUp.writeNewSignupToLog(now);
        expect(SignUp.shouldThrottle()).toBeFalse();
    }

    @Test
    public void shouldThrottle() throws Exception {
        TestHelper.enableSDCard();
        Robolectric.application.onCreate();

        long now = System.currentTimeMillis();
        SignUp.writeNewSignupToLog(now);
        SignUp.writeNewSignupToLog(now);
        SignUp.writeNewSignupToLog(now);
        expect(SignUp.shouldThrottle()).toBeTrue();
    }
}
