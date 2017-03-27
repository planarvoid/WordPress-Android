package com.soundcloud.android.tests.auth.signup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.screens.auth.signup.SignupSpamScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailShowingCaptchaTest extends SignUpTest {

    @Override
    protected void addInitialStubMappings() {
        stubFor(post(urlPathEqualTo(ApiEndpoints.SIGN_UP.path()))
                        .willReturn(aResponse().withStatus(428).withBody("{\"error_key\": \"captcha_required\"}")));
    }

    @Override
    protected boolean wiremockLoggingEnabled() {
        return true;
    }

    public void testSignupTriggersCaptcha() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen.typeEmail("someemail-which-triggers-captcha@siliconninjas.net")
                          .typePassword("password123")
                          .typeAge(21)
                          .signup()
                          .acceptTerms();

        SignupSpamScreen dialog = new SignupSpamScreen(solo);
        assertThat(dialog.isVisible(), is(true));

        // To complete the test we could press the "try again" button
        // and check if the activity would loose focus.
        // This does not work if the user is asked to choose which application will open the captcha URL
    }
}
