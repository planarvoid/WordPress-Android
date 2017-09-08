package com.soundcloud.android.tests.auth.signup;

import static com.soundcloud.android.api.ApiEndpoints.SIGN_UP;
import static com.soundcloud.android.framework.TestUser.generateEmail;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.auth.SignUpTest;
import org.junit.Test;

public class ByEmailTest extends SignUpTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testUserSuccess() throws Exception {
        addMockedResponse(SIGN_UP.path(), "sign-up-success.json");

        signUpBasicsScreen = homeScreen
                .clickSignUpButton()
                .clickByEmailButton()
                .typeEmail(generateEmail())
                .typePassword("password123")
                .typeAge(21)
                .chooseGenderCustom()
                .typeCustomGender("Genderqueer");

        assertTrue(signUpBasicsScreen.isDoneButtonEnabled());
        signUpBasicsScreen.signup();
        assertTrue(signUpBasicsScreen.acceptTermsButton().isOnScreen());
        signUpBasicsScreen.acceptTerms();
        assertTrue(signUpBasicsScreen.saveButton().isOnScreen());
        StreamScreen streamScreen = signUpBasicsScreen.saveSignUpDetails();

        assertThat(streamScreen, is(visible()));
    }

}
