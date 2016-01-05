package com.soundcloud.android.tests.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.auth.SignUpTest;

public class SignUpBasicsFieldValidationTest extends SignUpTest {

    public void testDoneButtonEnabledWithValidInput() {
        signUpBasicsScreen = homeScreen
                .clickSignUpButton()
                .clickByEmailButton();

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen
                .typeEmail("slawomir@aol.com")
                .typePassword("password123");
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typeAge(21);
        assertThat("Done button should be enabled", signUpBasicsScreen.isDoneButtonEnabled());

        // gender is optional, so done button should stay enabled
        signUpBasicsScreen.chooseGender("Female");
        assertThat("Done button should be enabled", signUpBasicsScreen.isDoneButtonEnabled());

        signUpBasicsScreen.chooseGender("Custom");
        signUpBasicsScreen.typeCustomGender("Intersex");

        assertThat("Done button should be enabled", signUpBasicsScreen.isDoneButtonEnabled());
    }

    public void testDoneButtonNotEnabledWithInvalidInput() {
        startWithValidSignupInput();

        // missing email
        signUpBasicsScreen.clearEmail();
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typeEmail("slawomir@aol.com");
        assertThat("Done button should be enabled", signUpBasicsScreen.isDoneButtonEnabled());

        // missing password
        signUpBasicsScreen.clearPassword();
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typePassword("password123");
        assertThat("Done button should be enabled", signUpBasicsScreen.isDoneButtonEnabled());
    }

    public void testToastShownForInvalidBirthYears() {
        startWithValidSignupInput();

        // birth year too low
        signUpBasicsScreen.clearAge();
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typeAge(12);
        assertThat("Done button should be enabled", signUpBasicsScreen.isDoneButtonEnabled());

        signUpBasicsScreen.signup();
        assertTrue(waiter.expectToastWithText(toastObserver, solo.getString(R.string.authentication_error_age_not_valid)));
    }

    private void startWithValidSignupInput() {
        signUpBasicsScreen = homeScreen.clickSignUpButton().clickByEmailButton();

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        // start from a valid set
        signUpBasicsScreen
                .typeEmail("slawomir@aol.com")
                .typePassword("password123")
                .typeAge(21);

        assertThat("Done button should be enabled", signUpBasicsScreen.isDoneButtonEnabled());
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
