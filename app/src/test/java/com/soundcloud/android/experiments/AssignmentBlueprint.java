package com.soundcloud.android.experiments;

import com.google.common.collect.Lists;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.NewInstance;
import com.tobedevoured.modelcitizen.field.ConstructorCallback;

import java.util.List;

@Blueprint(Assignment.class)
public class AssignmentBlueprint {
    @NewInstance
    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            Assignment assignment = new Assignment();
            assignment.setLayers(createLayers());
            return assignment;
        }
    };

    private List<Layer> createLayers() {
        Layer androidUi = new Layer();
        androidUi.setExperimentId(1);
        androidUi.setExperimentName("experiment 5");
        androidUi.setLayerName("android-ui");
        androidUi.setVariantId(1);
        androidUi.setVariantName("variant 3");
        Layer androidListeningTime = new Layer();
        androidListeningTime.setExperimentId(1);
        androidListeningTime.setExperimentName("experiment 3");
        androidListeningTime.setLayerName("android-listen");
        androidListeningTime.setVariantId(2);
        androidListeningTime.setVariantName("variant 9");

        return Lists.newArrayList(androidUi, androidListeningTime);
    }
}
