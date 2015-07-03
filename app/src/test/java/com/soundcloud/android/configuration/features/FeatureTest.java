package com.soundcloud.android.configuration.features;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class FeatureTest {

    @Test
    public void featurePlansIsEmptyIfNotSet() {
        Feature feature = new Feature("name", false, null);

        assertThat(feature.plans).isEmpty();
    }

}