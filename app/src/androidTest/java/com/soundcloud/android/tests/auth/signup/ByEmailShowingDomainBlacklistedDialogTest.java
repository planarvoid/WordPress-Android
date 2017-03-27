package com.soundcloud.android.tests.auth.signup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.screens.auth.signup.SignupDomainBlacklistedScreen;
import com.soundcloud.android.tests.auth.SignUpTest;

public class ByEmailShowingDomainBlacklistedDialogTest extends SignUpTest {

    @Override
    protected void addInitialStubMappings() {
        stubFor(post(urlPathEqualTo(ApiEndpoints.SIGN_UP.path()))
                        .willReturn(aResponse().withStatus(429).withBody("{\"error_key\": \"domain_blacklisted\"}")));
    }

    public void testDomainBlacklistedSignup() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen.typeEmail(generateEmail());
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.typeAge(21);

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();

        SignupDomainBlacklistedScreen dialog = new SignupDomainBlacklistedScreen(solo);
        assertThat(dialog.isVisible(), is(true));
    }

    protected String generateEmail() {
        return "blacklistedEmail3@0815.ru";
    }

}
