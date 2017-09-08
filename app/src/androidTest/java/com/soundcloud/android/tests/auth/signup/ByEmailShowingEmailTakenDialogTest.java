package com.soundcloud.android.tests.auth.signup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.api.ApiEndpoints.SIGN_UP;
import static com.soundcloud.android.framework.TestUser.testUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.auth.signup.SignupEmailTakenScreen;
import com.soundcloud.android.tests.auth.SignUpTest;
import org.junit.Test;

public class ByEmailShowingEmailTakenDialogTest extends SignUpTest {

    @Override
    protected void addInitialStubMappings() {
        stubFor(post(urlPathEqualTo(SIGN_UP.path()))
                        .willReturn(aResponse().withStatus(400).withBody("{\"error_key\": \"email_taken\"}")));
    }

    @Test
    public void testEmailTakenSignup() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signUpMethodScreen.clickByEmailButton();

        signUpBasicsScreen.typeEmail(generateEmail());
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.typeAge(21);

        signUpBasicsScreen.signup();
        signUpBasicsScreen.acceptTerms();

        SignupEmailTakenScreen dialog = new SignupEmailTakenScreen(solo);
        assertThat(dialog.isVisible(), is(true));
    }

    protected String generateEmail() {
        return testUser.getEmail();
    }

}
