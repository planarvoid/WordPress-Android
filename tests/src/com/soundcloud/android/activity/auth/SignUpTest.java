package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.utils.CloudUtils.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.CloudUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


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
}
