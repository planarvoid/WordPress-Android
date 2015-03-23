package com.soundcloud.android.profile;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

import static com.soundcloud.android.Expect.expect;

public class BirthdayInfoTest {

    private int currentYear;

    @Before
    public void setUp() {
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldAcceptValidMonthAndYear() {
        BirthdayInfo actual = BirthdayInfo.buildFrom(1, currentYear - 24);

        expect(actual).not.toBeNull();
        expect(actual.month).toEqual(1);
        expect(actual.year).toEqual(currentYear - 24);
    }

    @Test
    public void shouldDenyInvalidMonth() {
        expect(BirthdayInfo.buildFrom(0, currentYear - 24)).toBeNull();
        expect(BirthdayInfo.buildFrom(13, currentYear - 24)).toBeNull();
    }

    @Test
    public void shouldDenyInvalidYear() {
        expect(BirthdayInfo.buildFrom(1, currentYear - 13)).toBeNull();
        expect(BirthdayInfo.buildFrom(1, currentYear - 101)).toBeNull();
    }
}
