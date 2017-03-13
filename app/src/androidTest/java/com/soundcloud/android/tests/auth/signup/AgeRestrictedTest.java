package com.soundcloud.android.tests.auth.signup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.framework.TestUser.generateEmail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.screens.auth.signup.SignupAgeRestrictedScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class AgeRestrictedTest extends SignUpTest {

    private static final int MINIMUM_AGE = 16;
    private static final String AGE_RESTRICTED_RESPONSE = "{\n\"error_key\": \"age_restricted\",\n \"minimum_age\": \"" + MINIMUM_AGE + "\"\n}";

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void addInitialStubMappings() {
        stubFor(post(urlPathEqualTo(ApiEndpoints.SIGN_UP.path()))
                        .willReturn(aResponse().withStatus(400).withBody(AGE_RESTRICTED_RESPONSE)));
    }

    public void testUserAgeRestricted() throws Exception {
        signUpBasicsScreen = homeScreen
                .clickSignUpButton()
                .clickByEmailButton()
                .typeEmail(generateEmail())
                .typePassword("password123")
                .typeAge(15);

        assertTrue(signUpBasicsScreen.isDoneButtonEnabled());
        signUpBasicsScreen.signup();
        assertTrue(signUpBasicsScreen.acceptTermsButton().isOnScreen());
        signUpBasicsScreen.acceptTerms();

        SignupAgeRestrictedScreen dialog = new SignupAgeRestrictedScreen(solo, MINIMUM_AGE);
        assertThat(dialog.isVisible(), is(true));
    }

}
