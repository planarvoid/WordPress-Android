package com.soundcloud.android.auth.signup;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.auth.FBWebViewScreen;

import static com.soundcloud.android.tests.TestUser.Facebook;

public class ByFacebook extends SignUpTestCase {
    FBWebViewScreen fbWebViewScreen;

    public ByFacebook() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        fbWebViewScreen = new FBWebViewScreen(solo);
    }

    public void testUserFollowSingleSuccess() throws Exception {
        signUpScreen = homeScreen.clickSignUpButton();

        signUpScreen.clickFacebookButton();
        signUpScreen.acceptTerms();
        assertTrue(fbWebViewScreen.waitForContent());

        fbWebViewScreen.typeEmail(Facebook.getEmail());
        fbWebViewScreen.typePassword(Facebook.getPassword());
        fbWebViewScreen.submit();

        suggestedUsersScreen = signUpScreen.waitForSuggestedUsers();
        assertTrue(suggestedUsersScreen.hasContent());
        assertTrue(suggestedUsersScreen.hasMusicSection());
        assertTrue(suggestedUsersScreen.hasAudioSection());
        assertTrue(suggestedUsersScreen.hasFacebookSection());

        suggestedUsersCategoryScreen = suggestedUsersScreen.goToFacebook();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());

        Facebook.unfollowAll(getInstrumentation().getTargetContext(), solo.getCurrentActivity());
    }
}
