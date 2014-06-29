package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.screens.EmailConfirmScreen;
import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.HomeScreen;
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

    private HomeScreen dismissDistractions(EmailConfirmScreen confirmScreen) {
        FeatureFlags flags = new FeatureFlags(getActivity().getResources());
        if (flags.isEnabled(Feature.EMAIL_OPT_IN)) {
            EmailOptInScreen optIn = confirmScreen.clickConfirmLater();
            return optIn.clickNo();
        } else {
            if(confirmScreen.isVisible()){
                return confirmScreen.goBack();
            }
            return new HomeScreen(solo);
        }
    }

}
