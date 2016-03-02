package com.soundcloud.android.onboarding.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class GenderOptionTest {

    @Test
    public void returnsNullForPreferNotToSay() {
        assertThat(GenderOption.NO_PREF.getApiValue("foo")).isNull();
    }

    @Test
    public void returnsNullForCustomAndEmptyCustomValue() {
        assertThat(GenderOption.CUSTOM.getApiValue("")).isNull();
        assertThat(GenderOption.CUSTOM.getApiValue(null)).isNull();
    }

    @Test
    public void returnsObjectForMale() {
        assertThat(GenderOption.MALE.getApiValue(null)).isEqualTo("male");
    }

    @Test
    public void returnsObjectForFemale() {
        assertThat(GenderOption.FEMALE.getApiValue(null)).isEqualTo("female");
    }

    @Test
    public void returnsObjectForCustomWithCustomSpecified() {
        assertThat(GenderOption.CUSTOM.getApiValue("fluid")).isEqualTo("fluid");
    }
}
