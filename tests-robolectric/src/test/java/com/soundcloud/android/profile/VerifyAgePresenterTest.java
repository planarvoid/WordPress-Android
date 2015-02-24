package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

public class VerifyAgePresenterTest {

    private int currentYear;

    @Before
    public void setUp() {
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
    }

    @Test
    public void shouldAcceptValidMonthAndYear() {
        expect(VerifyAgePresenter.validMonthAndYear(1, currentYear - 24)).toBeTrue();
    }

    @Test
    public void shouldDenyInvalidMonth() {
        expect(VerifyAgePresenter.validMonthAndYear(0, currentYear - 24)).toBeFalse();
    }

    @Test
    public void shouldDenyInvalidYear() {
        expect(VerifyAgePresenter.validMonthAndYear(1, currentYear - 13)).toBeFalse();
        expect(VerifyAgePresenter.validMonthAndYear(1, currentYear - 101)).toBeFalse();
    }
}
