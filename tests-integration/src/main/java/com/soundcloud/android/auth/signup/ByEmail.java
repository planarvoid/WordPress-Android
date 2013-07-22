package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;

public class ByEmail extends SignUpTestCase {
    public ByEmail() {
        super();
    }

    public void testSomething() throws Exception {
        onboardScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();

    }
}
