package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.HomeScreen;

import android.test.FlakyTest;

public class ByEmail extends SignUpTestCase {
    public ByEmail() {
        super();
    }

    @FlakyTest
    public void testUserFollowSingleSuccess() throws Exception {
        signupScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();
        signUpScreen.skipInfo();
        signUpScreen.waitForSuggestedUsers();

        assert(suggestedUsersScreen.hasContent());
        assert(suggestedUsersScreen.hasMusicSection());
        assert(suggestedUsersScreen.hasAudioSection());
        assertFalse(suggestedUsersScreen.hasFacebookSection());

        suggestedUsersScreen.rockOut();

        String followedUsername = suggestedUsersCategoryScreen.followRandomUser();
        solo.goBack();

        final HomeScreen finish = suggestedUsersScreen.finish();
        assert(finish.hasItemByUsername(followedUsername));
    }
}
