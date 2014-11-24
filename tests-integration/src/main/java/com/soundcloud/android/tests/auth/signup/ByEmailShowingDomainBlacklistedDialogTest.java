package com.soundcloud.android.tests.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.tests.auth.SignUpTest;
import com.soundcloud.android.screens.auth.signup.SignupDomainBlacklistedScreen;

public class ByEmailShowingDomainBlacklistedDialogTest extends SignUpTest {
    public ByEmailShowingDomainBlacklistedDialogTest() {
        super();
    }

    // Ignoring this test until failing because signups with the domain @0815.ru are not blocked on the server side now,
    // due to a Sven issue - https://soundcloud.atlassian.net/browse/TSS-520
    // When this issue is resolved we can re-activate this test and delete blacklistedEmail@0815.ru using Sonar
    public void ignore_testDomainBlacklistedSignup() throws Exception {
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
        return "blacklistedEmail3@0815.ru";
    }

}
