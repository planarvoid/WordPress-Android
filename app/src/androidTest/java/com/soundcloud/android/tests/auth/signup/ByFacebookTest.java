package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.Facebook;

import com.soundcloud.android.screens.auth.FBWebViewScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByFacebookTest extends SignUpTest {
    FBWebViewScreen fbWebViewScreen;

    public ByFacebookTest() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testUserFollowSingleSuccess() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();

        signUpMethodScreen.clickFacebookButton();
        signUpMethodScreen.acceptTerms();
        fbWebViewScreen = new FBWebViewScreen(solo);

        //otherwise field suggestions pop put and don't allow password field to be clicked
        fbWebViewScreen.typePassword(Facebook.getPassword());
        fbWebViewScreen.typeEmail(Facebook.getEmail());
        fbWebViewScreen.submit();

        suggestedUsersScreen = signUpMethodScreen.waitForSuggestedUsers();
        assertTrue(suggestedUsersScreen.hasContent());
        assertTrue(suggestedUsersScreen.hasMusicSection());
        assertTrue(suggestedUsersScreen.hasAudioSection());
        assertTrue(suggestedUsersScreen.hasFacebookSection());

        suggestedUsersCategoryScreen = suggestedUsersScreen.goToFacebook();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());
        suggestedUsersCategoryScreen.deselectAll();
    }
}
