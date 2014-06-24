package com.soundcloud.android.auth.signup;

import static com.soundcloud.android.tests.hamcrest.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.auth.SignUpTestCase;
import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.tests.TestUser;

public class EmailOptInTest extends SignUpTestCase {
    protected SignUpScreen signUpScreen;
    private EmailOptInScreen optInScreen;

    public EmailOptInTest() {
        super();
    }

    public void ShouldShowEmailOptInOnSignUp() {
        signUpScreen = homeScreen.clickSignUpButton();

        // TODO : Re-use the same user
        signUpScreen.typeEmail(TestUser.generateEmail());
        signUpScreen.typePassword("password123");

        signUpScreen.signup();
        signUpScreen.acceptTerms();
        signUpScreen.skipInfo();
        new SuggestedUsersScreen(solo).finish();
        optInScreen = new EmailOptInScreen(solo);
        assertThat(optInScreen, is(Visible()));
    }
}
