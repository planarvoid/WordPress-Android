package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtilsTest {

    @Test
    public void shouldReturnYearFromDateString() throws Exception {
        int year = DateUtils.yearFromDateString("2010-10-10", "yyyy-MM-dd");

        assertThat(year).isEqualTo(2010);
    }

    @Test
    public void isInRange_absent() throws Exception {
        assertThat(DateUtils.isInLast(Optional.absent(), 10, Calendar.HOUR_OF_DAY)).isFalse();
    }

    @Test
    public void isInRange_before() throws Exception {
        long time = new Date().getTime() - TimeUnit.MILLISECONDS.convert(11, TimeUnit.DAYS);
        assertThat(DateUtils.isInLast(Optional.of(new Date(time)), 10, Calendar.DATE)).isFalse();
    }

    @Test
    public void isInRange_after() throws Exception {
        long time = new Date().getTime() + TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
        assertThat(DateUtils.isInLast(Optional.of(new Date(time)), 10, Calendar.DATE)).isTrue();
    }

    @Test
    public void isInRange_now() throws Exception {
        long time = new Date().getTime();
        assertThat(DateUtils.isInLast(Optional.of(new Date(time)), 10, Calendar.DATE)).isTrue();
    }

    @Test
    public void isInRange() throws Exception {
        long time = new Date().getTime() - TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS);
        assertThat(DateUtils.isInLast(Optional.of(new Date(time)), 10, Calendar.DATE)).isTrue();
    }
}
