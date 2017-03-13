package com.soundcloud.android.onboarding.auth.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class AgeRestrictionAuthResultTest {

    private static final String MINIMUM_AGE = "15";
    private AuthTaskResult authTaskResult = AgeRestrictionAuthResult.create(MINIMUM_AGE);

    @Test
    public void ageRestricitonIsNoSuccess() throws Exception {
        assertThat(authTaskResult.wasSuccess()).isFalse();
    }

    @Test
    public void ageRestrictionErrorCorrectlyIdentified() throws Exception {
        assertThat(authTaskResult.wasAgeRestricted()).isTrue();
    }

    @Test
    public void errorContainsMinimumAge() throws Exception {
        assertThat(((AgeRestrictionAuthResult) authTaskResult).getMinimumAge()).isEqualTo(MINIMUM_AGE);
    }
}
