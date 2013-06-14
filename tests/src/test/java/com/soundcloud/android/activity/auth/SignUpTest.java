package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class SignUpTest {

    @Test
    public void shouldCheckpasswordLength() throws Exception {
        expect(SignUp.checkPassword("123456")).toBeTrue();
        expect(SignUp.checkPassword("12345")).toBeFalse();
        expect(SignUp.checkPassword(null)).toBeFalse();
    }
}