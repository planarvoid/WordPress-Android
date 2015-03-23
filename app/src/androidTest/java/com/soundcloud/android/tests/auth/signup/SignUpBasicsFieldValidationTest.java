package com.soundcloud.android.tests.auth.signup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.tests.auth.SignUpTest;

public class SignUpBasicsFieldValidationTest extends SignUpTest {

    public void testDoneButtonEnabledWithValidInput() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signupMethodScreen.clickByEmailButton();

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.typeEmail("slawomir@aol.com");
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.chooseBirthMonth("March");
        signUpBasicsScreen.typeBirthYear("1975");

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));

        // gender is optional, so done button should stay enabled
        signUpBasicsScreen.chooseGender("Female");

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));

        signUpBasicsScreen.chooseGender("Custom");
        signUpBasicsScreen.typeCustomGender("Intersex");

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));
    }

    public void testDoneButtonNotEnabledWithInvalidInput() throws Exception {
        signUpMethodScreen = homeScreen.clickSignUpButton();
        signUpBasicsScreen = signupMethodScreen.clickByEmailButton();

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        // start from a valid set
        signUpBasicsScreen.typeEmail("slawomir@aol.com");
        signUpBasicsScreen.typePassword("password123");
        signUpBasicsScreen.chooseBirthMonth("March");
        signUpBasicsScreen.typeBirthYear("1975");

        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));

        // invalid birth years
        signUpBasicsScreen.clearBirthYear();
        signUpBasicsScreen.typeBirthYear("10996");
        assertThat(signUpBasicsScreen.getBirthYear(), equalTo("1099"));
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.clearBirthYear();
        signUpBasicsScreen.typeBirthYear("999");
        assertThat(signUpBasicsScreen.getBirthYear(), equalTo("999"));
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.clearBirthYear();
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(false));

        signUpBasicsScreen.clearBirthYear();
        signUpBasicsScreen.typeBirthYear("1975");
        assertThat(signUpBasicsScreen.isDoneButtonEnabled(), is(true));

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
}
