package com.soundcloud.android.activity.auth;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(RobolectricTestRunner.class)
public class SignUpTests {
    @Test
    public void shouldCheckEmail() throws Exception {
        assertTrue(SignUp.checkEmail("foo@bar.com"));
        assertTrue(SignUp.checkEmail("Foo+special@bar.com"));
        assertFalse(SignUp.checkEmail("foo@barcom"));
        assertFalse(SignUp.checkEmail("foobar.com"));
    }

    @Test
    public void shouldCheckpasswordLength() throws Exception {
        assertTrue(SignUp.checkPassword("1234"));
        assertFalse(SignUp.checkPassword("123"));
        assertFalse(SignUp.checkPassword(null));
    }
}
