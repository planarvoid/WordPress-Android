package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.api.ApiEndpoints.SIGN_UP;
import static com.soundcloud.android.framework.TestUser.Facebook;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.auth.FBWebViewScreen;
import com.soundcloud.android.tests.auth.SignUpTest;
import org.junit.Test;

public class ByFacebookTest extends SignUpTest {
    FBWebViewScreen fbWebViewScreen;

    public ByFacebookTest() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testUserSuccess() throws Exception {
        addMockedResponse(SIGN_UP.path(), "sign-up-success.json");

        signUpMethodScreen = homeScreen.clickSignUpButton();

        signUpMethodScreen.clickFacebookButton();
        signUpMethodScreen.acceptTerms();
        fbWebViewScreen = new FBWebViewScreen(solo);

        //otherwise field suggestions pop put and don't allow password field to be clicked
        fbWebViewScreen.typePassword(Facebook.getPassword());
        fbWebViewScreen.typeEmail(Facebook.getEmail());
        fbWebViewScreen.submit();

        StreamScreen streamScreen = new StreamScreen(solo);
        assertThat(streamScreen, is(visible()));
    }
}
