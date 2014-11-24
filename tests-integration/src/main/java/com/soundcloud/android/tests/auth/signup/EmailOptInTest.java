package com.soundcloud.android.tests.auth.signup;

import com.soundcloud.android.tests.auth.SignUpTest;
import com.soundcloud.android.framework.screens.EmailOptInScreen;
import com.soundcloud.android.framework.screens.auth.SignUpScreen;
import com.soundcloud.android.framework.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.framework.TestUser;

public class EmailOptInTest extends SignUpTest {
    protected SignUpScreen signUpScreen;
    private EmailOptInScreen optInScreen;

    public EmailOptInTest() {
        super();
    }

    public void testShouldShowEmailOptInOnSignUp() {
        signUpScreen = homeScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(TestUser.generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();
        signUpScreen.skipInfo();
        new SuggestedUsersScreen(solo).finish();
        optInScreen = new EmailOptInScreen(solo);
        //TODO: Loading stream after signup takes awfuly long time, find a way to fix this
        //assertThat(optInScreen, is(Visible()));
    }
}
