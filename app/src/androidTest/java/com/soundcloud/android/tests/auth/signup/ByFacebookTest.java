package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.framework.TestUser.Facebook;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.EmailOptInScreen;
import com.soundcloud.android.screens.StreamScreen;
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

    public void testUserSuccess() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();

        signUpMethodScreen.clickFacebookButton();
        signUpMethodScreen.acceptTerms();
        fbWebViewScreen = new FBWebViewScreen(solo);

        //otherwise field suggestions pop put and don't allow password field to be clicked
        fbWebViewScreen.typePassword(Facebook.getPassword());
        fbWebViewScreen.typeEmail(Facebook.getEmail());
        fbWebViewScreen.submit();

        final EmailOptInScreen optInScreen = new EmailOptInScreen(solo);
        assertThat(optInScreen, is(visible()));

        final StreamScreen streamScreen = optInScreen.clickNo();
        assertThat(streamScreen, is(visible()));
    }
}
