package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.HomeScreen;

public class ByEmail extends SignUpTestCase {
    public ByEmail() {
        super();
    }

    public void testUserFollowSingleSuccess() throws Exception {
        onboardScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();

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
