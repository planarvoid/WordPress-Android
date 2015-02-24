package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.configuration.features.Feature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFeatures {

    public static Map<String, Boolean> asMap() {
        final HashMap<String, Boolean> features = new HashMap<>();
        features.put("feature_disabled", false);
        features.put("feature_enabled", true);
        return features;
    }

    public static List<Feature> asList() {
        return Arrays.asList(new Feature("feature_disabled", false), new Feature("feature_enabled", true));
    }

}
