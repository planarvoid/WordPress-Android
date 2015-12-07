package com.soundcloud.android.tests.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.screens.elements.SignUpSpamDialogElement;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailShowingSpamDialogTest extends SignUpTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testUserBlockedSpam() throws Exception {
        signUpBasicsScreen = homeScreen
                .clickSignUpButton()
                .clickByEmailButton()
                .typeEmail("blocked-mail-test-sc@yopmail.com")
                .typePassword("password123")
                .typeAge(21)
                .chooseGender("Custom")
                .typeCustomGender("Genderqueer");

        assertTrue(signUpBasicsScreen.isDoneButtonEnabled());
        signUpBasicsScreen.signup();
        assertTrue(signUpBasicsScreen.acceptTermsButton().isVisible());
        SignUpSpamDialogElement signUpSpamDialogElement = signUpBasicsScreen.clickAcceptTermsOpensSpamDialog();
        assertTrue(signUpSpamDialogElement.isVisible());
        assertTrue(signUpSpamDialogElement.clickCancelButton().isVisible());
    }
}
