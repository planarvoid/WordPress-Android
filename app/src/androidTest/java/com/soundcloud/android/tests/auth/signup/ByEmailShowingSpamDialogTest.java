package com.soundcloud.android.tests.auth.signup;

import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailShowingSpamDialogTest extends SignUpTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testUserBlockedSpam() throws Exception {
        homeScreen
                .clickSignUpButton()
                .clickByEmailButton()
                .typeEmail("blocked-mail-test-sc@yopmail.com")
                .typePassword("password123")
                .typeAge(21)
                .chooseGender("Custom")
                .typeCustomGender("Genderqueer")
                .signup()
                .acceptTerms()
                .closeSpamDialog();
    }
}
