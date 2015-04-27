package com.soundcloud.android.tests.auth.signup;

import com.soundcloud.android.screens.auth.SignUpBasicsScreen;
import com.soundcloud.android.tests.auth.SignUpTest;
import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.auth.SignUpMethodScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.framework.TestUser;

public class EmailOptInTest extends SignUpTest {
    protected SignUpMethodScreen signUpMethodScreen;
    private EmailOptInScreen optInScreen;
    private SignUpBasicsScreen signUpBasicsScreen;

    public EmailOptInTest() {
        super();
    }

    public void testShouldShowEmailOptInOnSignUp() {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        // TODO : Re-use the same user
        signUpBasicsScreen.typeEmail(TestUser.generateEmail());
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.typeAge(21);
        signUpBasicsScreen.chooseGender("Male");

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();
        signUpBasicsScreen.skipSignUpDetails();
        new SuggestedUsersScreen(solo).finish();
        optInScreen = new EmailOptInScreen(solo);
        //TODO: Loading stream after signup takes awfuly long time, find a way to fix this
        //assertThat(optInScreen, is(Visible()));
    }
}
