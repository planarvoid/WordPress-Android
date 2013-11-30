package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;

public class SmartFields extends SignUpTestCase {

    public void testDoneButtonBehavior() throws Exception {
        signUpScreen = homeScreen.clickSignUpButton();

        assertFalse(signUpScreen.getDoneButton().isEnabled());

        signUpScreen.typeEmail("slawomir@aol.com");
        signUpScreen.typePassword("password123");

        assert(signUpScreen.getDoneButton().isEnabled());
    }
}
