package com.soundcloud.android.tests.auth.signup;

import com.soundcloud.android.screens.auth.signup.SignupSpamScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ByEmailShowingCaptchaTest extends SignUpTest {

    public ByEmailShowingCaptchaTest() {
        super();
    }

    public void testSignupTriggersCaptcha() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen.typeEmail(generateEmail());
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.chooseBirthMonth("April");
        signUpBasicsScreen.typeBirthYear("1984");

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();

        SignupSpamScreen dialog = new SignupSpamScreen(solo);
        assertThat(dialog.isVisible(), is(true));

        // To complete the test we could press the "try again" button
        // and check if the activity would loose focus.
        // This does not work if the user is asked to choose which application will open the captcha URL
    }

    // All the emails with the domain "@siliconninjas.net" or "@test.allthecaptchas.io"
    // will have responses which will trigger a captcha
    // this is, a 422 response with the response body { "error": 103 }
    protected String generateEmail() {
        return "someemail-which-triggers-captcha@siliconninjas.net";
    }

}
