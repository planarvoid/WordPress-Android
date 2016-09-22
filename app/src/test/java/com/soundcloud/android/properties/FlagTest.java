package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.EnumSet;
import java.util.Locale;

@RunWith(MockitoJUnitRunner.class)
public class FlagTest {
    private static final Flag FEATURE_FLAG = Flag.TEST_FEATURE;
    private static final Flag FEATURE_FLAG_UNDER_DEVELOPMENT = Flag.TEST_FEATURE_UNDER_DEVELOPMENT;

    @Test
    public void featureShouldNotBeUnderDevelopment() {
        assertThat(FEATURE_FLAG.isUnderDevelopment()).isFalse();
        assertThat(FEATURE_FLAG.featureValue()).isTrue();
    }

    @Test
    public void featureShouldBeUnderDevelopment() {
        assertThat(FEATURE_FLAG_UNDER_DEVELOPMENT.isUnderDevelopment()).isTrue();
        assertThat(FEATURE_FLAG_UNDER_DEVELOPMENT.featureValue()).isFalse();
    }

    @Test
    public void featuresShouldReturnAllFlagsExceptTestingOnes() {
        final EnumSet<Flag> features = Flag.features();

        assertThat(features).hasSize(Flag.values().length - 2);
        assertThat(features).doesNotContain(FEATURE_FLAG, FEATURE_FLAG_UNDER_DEVELOPMENT);
    }

    @Test
    public void featureNameShouldBeLowerCase() {
        assertThat(FEATURE_FLAG.featureName()).isEqualTo(FEATURE_FLAG.name().toLowerCase(Locale.US));
        assertThat(FEATURE_FLAG_UNDER_DEVELOPMENT.featureName()).isEqualTo(FEATURE_FLAG_UNDER_DEVELOPMENT.name().toLowerCase(Locale.US));
    }
}
