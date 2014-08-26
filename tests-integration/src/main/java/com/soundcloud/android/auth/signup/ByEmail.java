package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.ProfileScreen;

public class ByEmail extends SignUpTestCase {
    private ProfileScreen userProfile;

    public ByEmail() {
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
