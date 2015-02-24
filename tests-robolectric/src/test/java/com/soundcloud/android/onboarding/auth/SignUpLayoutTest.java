package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class SignUpLayoutTest {

    @Test
    public void shouldNotAcceptPasswordShorterThan6Characters() throws Exception {
        expect(SignUpLayout.checkPassword("12345")).toBeFalse();
    }

    @Test
    public void shouldNotAcceptEmptyPassword() throws Exception {
        expect(SignUpLayout.checkPassword(null)).toBeFalse();
    }

    @Test
    public void shouldNotAcceptEmptyStringAsPassword() throws Exception {
        expect(SignUpLayout.checkPassword("")).toBeFalse();
    }

    @Test
    public void shouldAccept6CharactersPassword() throws Exception {
        expect(SignUpLayout.checkPassword("123456")).toBeTrue();
    }

}