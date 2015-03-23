package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class SignupMethodBasicsTest {

    @Test
    public void shouldNotAcceptPasswordShorterThan6Characters() throws Exception {
        expect(SignupBasicsLayout.checkPassword("12345")).toBeFalse();
    }

    @Test
    public void shouldNotAcceptEmptyPassword() throws Exception {
        expect(SignupBasicsLayout.checkPassword(null)).toBeFalse();
    }

    @Test
    public void shouldNotAcceptEmptyStringAsPassword() throws Exception {
        expect(SignupBasicsLayout.checkPassword("")).toBeFalse();
    }

    @Test
    public void shouldAccept6CharactersPassword() throws Exception {
        expect(SignupBasicsLayout.checkPassword("123456")).toBeTrue();
    }

}
