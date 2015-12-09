package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.generateEmail;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailTest extends SignUpTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testUserSuccess() throws Exception {
        signUpBasicsScreen = homeScreen
                .clickSignUpButton()
                .clickByEmailButton()
                .typeEmail(generateEmail())
                .typePassword("password123")
                .typeAge(21)
                .chooseGender("Custom")
                .typeCustomGender("Genderqueer");

        assertTrue(signUpBasicsScreen.isDoneButtonEnabled());
        signUpBasicsScreen.signup();
        assertTrue(signUpBasicsScreen.acceptTermsButton().isVisible());
        signUpBasicsScreen.acceptTerms();
        assertTrue(signUpBasicsScreen.skipButton().isVisible());
        EmailOptInScreen optInScreen = signUpBasicsScreen.skipSignUpDetails();

        assertThat(optInScreen, is(visible()));

        final StreamScreen streamScreen = optInScreen.clickNo();
        assertThat(streamScreen, is(visible()));
    }

}
