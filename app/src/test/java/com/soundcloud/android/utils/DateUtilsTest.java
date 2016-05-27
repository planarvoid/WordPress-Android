package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void shouldReturnYearFromDateString() throws Exception {
        int year = DateUtils.yearFromDateString("2010-10-10", "yyyy-MM-dd");

        assertThat(year).isEqualTo(2010);
    }
}