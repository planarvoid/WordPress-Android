package com.soundcloud.android.tests.auth.signup;

import com.soundcloud.android.tests.auth.SignUpTest;
import com.soundcloud.android.screens.ProfileScreen;

public class ByEmailTest extends SignUpTest {
    private ProfileScreen userProfile;

    public ByEmailTest() {
        super();
    }

    public void testUserFollowSingleSuccess() throws Exception {
        signUpScreen = homeScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();
        signUpScreen.skipInfo();
        suggestedUsersScreen = signUpScreen.waitForSuggestedUsers();

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
}
