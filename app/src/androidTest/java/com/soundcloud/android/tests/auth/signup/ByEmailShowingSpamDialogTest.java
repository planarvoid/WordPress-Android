package com.soundcloud.android.tests.auth.signup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.api.ApiEndpoints.SIGN_UP;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.screens.elements.SignUpSpamDialogElement;
import com.soundcloud.android.tests.auth.SignUpTest;
import org.junit.Test;

public class ByEmailShowingSpamDialogTest extends SignUpTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void addInitialStubMappings() {
        stubFor(post(urlPathEqualTo(SIGN_UP.path()))
                        .willReturn(aResponse().withStatus(429).withBody("{\"error_key\": \"domain_blacklisted\"}")));
    }

    @Test
    public void testUserBlockedSpam() throws Exception {
        signUpBasicsScreen = homeScreen
                .clickSignUpButton()
                .clickByEmailButton()
                .typeEmail("blocked-mail-test-sc@yopmail.com")
                .typePassword("password123")
                .typeAge(21)
                .chooseGenderCustom()
                .typeCustomGender("Genderqueer");

        assertTrue(signUpBasicsScreen.isDoneButtonEnabled());
        signUpBasicsScreen.signup();
        assertTrue(signUpBasicsScreen.acceptTermsButton().isOnScreen());
        SignUpSpamDialogElement signUpSpamDialogElement = signUpBasicsScreen.clickAcceptTermsOpensSpamDialog();
        assertTrue(signUpSpamDialogElement.isVisible());
        assertTrue(signUpSpamDialogElement.clickCancelButton().isVisible());
    }
}
