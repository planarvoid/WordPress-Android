package com.soundcloud.android.configuration;

import com.soundcloud.android.configuration.UserPlan.Upsell;
import com.soundcloud.android.configuration.experiments.Assignment;
import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;
import java.util.List;

@Blueprint(Configuration.class)
public class ConfigurationBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return Configuration.builder()
                                .features(createFeatures())
                                .userPlan(new UserPlan("free", true, Optional.absent(), Collections.singletonList(new Upsell("high_tier", 30))))
                                .assignment(new Assignment(createLayers()))
                                .deviceManagement(new DeviceManagement(false, true))
                                .selfDestruct(false)
                                .imageSizeSpecs(Collections.emptyList())
                                .build();
        }
    };

    private static List<Feature> createFeatures() {
        return TestFeatures.asList();
    }

    static List<Layer> createLayers() {
        final Layer androidUi = new Layer("android-ui", 5, "experiment 5", 3, "variant 3");
        final Layer androidListeningTime = new Layer("android-listen", 3, "experiment 3", 9, "variant 9");

        return Lists.newArrayList(androidUi, androidListeningTime);
    }
}
