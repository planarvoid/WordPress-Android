package com.soundcloud.android.onboarding.auth;

import org.junit.Test;

import static com.soundcloud.android.Expect.expect;

public class GenderOptionTest {

    @Test
    public void returnsNullForPreferNotToSay() {
        expect(GenderOption.NO_PREF.getApiValue("foo")).toBeNull();
    }

    @Test
    public void returnsNullForCustomAndEmptyCustomValue() {
        expect(GenderOption.CUSTOM.getApiValue("")).toBeNull();
        expect(GenderOption.CUSTOM.getApiValue(null)).toBeNull();
    }

    @Test
    public void returnsObjectForMale() {
        expect(GenderOption.MALE.getApiValue(null)).toBe("male");
    }

    @Test
    public void returnsObjectForFemale() {
        expect(GenderOption.FEMALE.getApiValue(null)).toBe("female");
    }

    @Test
    public void returnsObjectForCustomWithCustomSpecified() {
        expect(GenderOption.CUSTOM.getApiValue("fluid")).toBe("fluid");
    }
}
