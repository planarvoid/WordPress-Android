package com.soundcloud.android.profile;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

@RunWith(SoundCloudTestRunner.class)
public class BirthdayInfoTest {

    private int currentYear;
    private int currentMonth;

    @Before
    public void setUp() {
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentMonth = Calendar.getInstance().get(Calendar.MONTH);
    }

    @Test
    public void returnsCurrentMonthAsBirthdayMonth() throws Exception {
        expect(BirthdayInfo.buildFrom(1).getMonth()).toEqual(currentMonth);
    }

    @Test
    public void returnsCurrentYearMinusAgeAsBirthdayYear() throws Exception {
        expect(BirthdayInfo.buildFrom(11).getYear()).toEqual(currentYear - 11);
    }

    @Test
    public void ageIsValidIfGreaterThan12() throws Exception {
        expect(BirthdayInfo.buildFrom(13).isValid()).toBeTrue();
    }

    @Test
    public void ageIsInvalidIfLessThan13() throws Exception {
        expect(BirthdayInfo.buildFrom(12).isValid()).toBeFalse();
    }

}
