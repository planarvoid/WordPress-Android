package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.configuration.features.Feature;

import java.util.Arrays;
import java.util.List;

public class TestFeatures {

    public static List<Feature> asList() {
        return Arrays.asList(
                new Feature("feature_disabled", false, Arrays.asList("mid_tier")),
                new Feature("feature_enabled", true, Arrays.asList("mid_tier")));
    }

}
