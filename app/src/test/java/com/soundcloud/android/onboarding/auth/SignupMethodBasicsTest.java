package com.soundcloud.android.onboarding.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SignupMethodBasicsTest {

    @Test
    public void shouldNotAcceptPasswordShorterThan6Characters() throws Exception {
        assertThat(SignupBasicsLayout.checkPassword("12345")).isFalse();
    }

    @Test
    public void shouldNotAcceptEmptyPassword() throws Exception {
        assertThat(SignupBasicsLayout.checkPassword(null)).isFalse();
    }

    @Test
    public void shouldNotAcceptEmptyStringAsPassword() throws Exception {
        assertThat(SignupBasicsLayout.checkPassword("")).isFalse();
    }

    @Test
    public void shouldAccept6CharactersPassword() throws Exception {
        assertThat(SignupBasicsLayout.checkPassword("123456")).isTrue();
    }

}
