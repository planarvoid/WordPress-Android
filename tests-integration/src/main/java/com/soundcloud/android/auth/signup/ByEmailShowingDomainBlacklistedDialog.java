package com.soundcloud.android.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.auth.signup.SignupDomainBlacklistedScreen;

public class ByEmailShowingDomainBlacklistedDialog extends SignUpTestCase {
    public ByEmailShowingDomainBlacklistedDialog() {
        super();
    }

    public void testDomainBlacklistedSignup() throws Exception {
        signUpScreen = homeScreen.clickSignUpButton();

        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");
        signUpScreen.signup();
        signUpScreen.acceptTerms();

        SignupDomainBlacklistedScreen dialog = new SignupDomainBlacklistedScreen(solo);
        assertThat(dialog.isVisible(), is(true));
    }

    // All the emails with the domain "@0815.ru" will have responses which will trigger a denied signup dialog,
    // this is, a 422 response with the response body { "error": 102 }
    protected String generateEmail() {
        return "blacklistedEmail@0815.ru";
    }

}
