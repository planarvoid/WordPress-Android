package com.soundcloud.android.configuration.experiments;

import com.soundcloud.java.collections.Lists;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.List;

@Blueprint(Assignment.class)
public class AssignmentBlueprint {
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new Assignment(createLayers());
        }
    };

    private List<Layer> createLayers() {
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
