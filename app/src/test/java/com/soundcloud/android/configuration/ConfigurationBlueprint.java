package com.soundcloud.android.configuration;

import com.soundcloud.android.configuration.experiments.Layer;
import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import com.soundcloud.java.collections.Lists;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;
import java.util.List;

@Blueprint(Configuration.class)
public class ConfigurationBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new Configuration(createFeatures(), new UserPlan("free", Collections.singletonList("mid_tier")), createLayers(), new DeviceManagement(false, "device_123"));
        }
    };

    public static List<Feature> createFeatures() {
        return TestFeatures.asList();
    }

    public static List<Layer> createLayers() {
        Layer androidUi = new Layer();
        androidUi.setExperimentId(5);
        androidUi.setExperimentName("experiment 5");
        androidUi.setLayerName("android-ui");
        androidUi.setVariantId(3);
        androidUi.setVariantName("variant 3");
        Layer androidListeningTime = new Layer();
        androidListeningTime.setExperimentId(3);
        androidListeningTime.setExperimentName("experiment 3");
        androidListeningTime.setLayerName("android-listen");
        androidListeningTime.setVariantId(9);
        androidListeningTime.setVariantName("variant 9");

        return Lists.newArrayList(androidUi, androidListeningTime);
    }
}
