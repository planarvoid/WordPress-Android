package com.soundcloud.android.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;

public class BirthdayInfoTest {

    private int currentYear;
    private int currentMonth;

    @Before
    public void setUp() {
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1; // default returned month is 0 based
    }

    @Test
    public void returnsOneBasedMonthAsBirthdayMonth() {
        assertThat(BirthdayInfo.buildFrom(1).getMonth()).isEqualTo(currentMonth);
    }

    @Test
    public void returnsCurrentYearMinusAgeAsBirthdayYear() {
        assertThat(BirthdayInfo.buildFrom(11).getYear()).isEqualTo(currentYear - 11);
    }

    @Test
    public void ageIsValidIfGreaterThan12() {
        assertThat(BirthdayInfo.buildFrom(13).isValid()).isTrue();
    }

    @Test
    public void ageIsInvalidIfLessThan13() {
        assertThat(BirthdayInfo.buildFrom(12).isValid()).isFalse();
    }

}
