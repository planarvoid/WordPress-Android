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
        onboardScreen.clickSignUpButton();

        signUpScreen.clickFacebookButton();
        signUpScreen.acceptTerms();

        assertTrue(fbWebViewScreen.waitForContent());
        fbWebViewScreen.typeEmail(Facebook.getEmail());
        fbWebViewScreen.typePassword(Facebook.getPassword());
        fbWebViewScreen.submit();

        signUpScreen.waitForSuggestedUsers();
        assertTrue(suggestedUsersScreen.hasContent());

        assertTrue(suggestedUsersScreen.hasMusicSection());
        assertTrue(suggestedUsersScreen.hasAudioSection());
        assertTrue(suggestedUsersScreen.hasFacebookSection());

        suggestedUsersScreen.goToFacebook();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());
    }
}
