package com.soundcloud.android.tests.auth.signup;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.auth.signup.SignupEmailTakenScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ByEmailShowingEmailTakenDialogTest extends SignUpTest {

    public ByEmailShowingEmailTakenDialogTest() {
        super();
    }

    public void testEmailTakenSignup() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen.typeEmail(generateEmail());
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.chooseBirthMonth("April");
        signUpBasicsScreen.typeBirthYear("1984");

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();

        SignupEmailTakenScreen dialog = new SignupEmailTakenScreen(solo);
        assertThat(dialog.isVisible(), is(true));
    }

    // Use an email which was already registered
    // this is, a 422 response with the response body { "error": 101 }
    protected String generateEmail() {
        return TestUser.testUser.getEmail();
    }

}
