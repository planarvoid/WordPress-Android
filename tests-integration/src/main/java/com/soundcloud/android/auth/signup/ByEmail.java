package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.screens.EmailConfirmScreen;
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

        EmailConfirmScreen confirmScreen = suggestedUsersScreen.finish();
        HomeScreen home = dismissDistractions(confirmScreen);

        assert(home.hasItemByUsername(followedUsername));
    }

    private HomeScreen dismissDistractions(EmailConfirmScreen confirmScreen) {
        FeatureFlags flags = new FeatureFlags(getActivity().getResources());
        if (flags.isEnabled(Feature.EMAIL_OPT_IN)) {
            EmailOptInScreen optIn = confirmScreen.clickConfirmLater();
            return optIn.clickNo();
        } else {
            return confirmScreen.goBack();
        }
    }

}
