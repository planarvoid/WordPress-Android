package com.soundcloud.android.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.auth.SignUpTestCase;

public class SmartFields extends SignUpTestCase {

    public void testDoneButtonBehavior() throws Exception {
        signUpScreen = homeScreen.clickSignUpButton();

        assertThat(signUpScreen.isDoneButtonEnabled(), is(false));

        signUpScreen.typeEmail("slawomir@aol.com");
        signUpScreen.typePassword("password123");

        assertThat(signUpScreen.isDoneButtonEnabled(), is(true));
    }
}
