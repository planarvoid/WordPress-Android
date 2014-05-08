package com.soundcloud.android.auth.signup;

import static com.soundcloud.android.tests.TestUser.Facebook;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.auth.FBWebViewScreen;

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

        //otherwise field suggestions pop put and don't allow password field to be clicked
        fbWebViewScreen.typePassword(Facebook.getPassword());
        fbWebViewScreen.typeEmail(Facebook.getEmail());
        fbWebViewScreen.submit();

        suggestedUsersScreen = signUpScreen.waitForSuggestedUsers();
        assertTrue(suggestedUsersScreen.hasContent());
        assertTrue(suggestedUsersScreen.hasMusicSection());
        assertTrue(suggestedUsersScreen.hasAudioSection());
        assertTrue(suggestedUsersScreen.hasFacebookSection());

        suggestedUsersCategoryScreen = suggestedUsersScreen.goToFacebook();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());

        Facebook.unfollowAll(solo.getCurrentActivity());
    }
}
