package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.HomeScreen;

public class ByEmail extends SignUpTestCase {
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

        String followedUsername = suggestedUsersCategoryScreen.followRandomUser();
        solo.goBack();

        final EmailOptInScreen emailOptIn = suggestedUsersScreen.finish();
        solo.goBack();
        final HomeScreen home = emailOptIn.clickNo();

        assert(home.hasItemByUsername(followedUsername));
    }
}
