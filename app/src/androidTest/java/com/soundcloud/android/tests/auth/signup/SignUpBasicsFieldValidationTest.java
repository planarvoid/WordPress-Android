package com.soundcloud.android.tests.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.auth.SignUpTest;

public class SignUpBasicsFieldValidationTest extends SignUpTest {

    public void testDoneButtonEnabledWithValidInput() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signupMethodScreen.clickByEmailButton();

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typeEmail("slawomir@aol.com");
        signUpBasicsScreen.typePassword("password123");
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typeAge(21);
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));

        // gender is optional, so done button should stay enabled
        signUpBasicsScreen.chooseGender("Female");

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));

        signUpBasicsScreen.chooseGender("Custom");
        signUpBasicsScreen.typeCustomGender("Intersex");

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));
    }

    public void testDoneButtonNotEnabledWithInvalidInput() throws Exception {
        startWithValidSignupInput();

        // missing email
        signUpBasicsScreen.clearEmail();
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typeEmail("slawomir@aol.com");
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));

        // missing password
        signUpBasicsScreen.clearPassword();
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typePassword("password123");
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));
    }

    public void testToastShownForInvalidBirthYears() throws Exception {
        startWithValidSignupInput();

        // birth year too low
        signUpBasicsScreen.clearAge();
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));
        signUpBasicsScreen.typeAge(12);
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));
        signUpBasicsScreen.signup();
        assertTrue(waiter.expectToastWithText(toastObserver, solo.getString(R.string.authentication_error_age_not_valid)));
    }

    private void startWithValidSignupInput() {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signupMethodScreen.clickByEmailButton();

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        // start from a valid set
        signUpBasicsScreen.typeEmail("slawomir@aol.com");
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.typeAge(21);

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
