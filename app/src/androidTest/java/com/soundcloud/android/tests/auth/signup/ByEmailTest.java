package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.generateEmail;

import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailTest extends SignUpTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();
    }

    public void testUserFollowSingleSuccess() throws Exception {
        // TODO : Re-use the same user
        fillForm(generateEmail());

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();
        signUpBasicsScreen.skipSignUpDetails();
        suggestedUsersScreen = signUpBasicsScreen.waitForSuggestedUsers();

        assertTrue(suggestedUsersScreen.hasContent());
        assertTrue(suggestedUsersScreen.hasMusicSection());
        assertTrue(suggestedUsersScreen.hasAudioSection());
        assertFalse(suggestedUsersScreen.hasFacebookSection());

        suggestedUsersCategoryScreen = suggestedUsersScreen.rockOut();

        suggestedUsersCategoryScreen.followRandomUser();
        solo.goBack();

        suggestedUsersScreen.finish();
        //TODO: This is taking awfuly long time to finish.
        // Find a way to wait properly.
    }

    public void testUserBlockedSpam() throws Exception {
        fillForm("blocked-mail-test-sc@yopmail.com");

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();
        signUpBasicsScreen.closeSpamDialog();
    }

    private void fillForm(String email) {
        signUpBasicsScreen.typeEmail(email);
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.chooseBirthMonth("May");
        signUpBasicsScreen.typeBirthYear("1995");
        signUpBasicsScreen.chooseGender("Custom");
        signUpBasicsScreen.typeCustomGender("Genderqueer");
    }
}
