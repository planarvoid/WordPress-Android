package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class SignUpTest {

    @Test
    public void shouldNotAcceptPasswordShorterThan6Characters() throws Exception {
        expect(SignUp.checkPassword("12345")).toBeFalse();
    }

    @Test
    public void shouldNotAcceptEmptyPassword() throws Exception {
        expect(SignUp.checkPassword(null)).toBeFalse();
    }

    @Test
    public void shouldNotAcceptEmptyStringAsPassword() throws Exception {
        expect(SignUp.checkPassword("")).toBeFalse();
    }

    @Test
    public void shouldAccept6CharactersPassword() throws Exception {
        expect(SignUp.checkPassword("123456")).toBeTrue();
    }

}