package com.soundcloud.android.tests.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.tests.auth.SignUpTest;
import com.soundcloud.android.screens.auth.signup.SignupEmailTakenScreen;
import com.soundcloud.android.framework.TestUser;

public class ByEmailShowingEmailTakenDialogTest extends SignUpTest {
    public ByEmailShowingEmailTakenDialogTest() {
        super();
    }

    public void testEmailTakenSignup() throws Exception {
        signUpScreen = homeScreen.clickSignUpButton();

        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");
        signUpScreen.signup();
        signUpScreen.acceptTerms();

        SignupEmailTakenScreen dialog = new SignupEmailTakenScreen(solo);
        assertThat(dialog.isVisible(), is(true));
    }

    // Use an email which was already registered
    // this is, a 422 response with the response body { "error": 101 }
    protected String generateEmail() {
        return TestUser.testUser.getEmail();
    }

}
