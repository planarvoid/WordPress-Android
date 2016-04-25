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
        final Layer androidUi = new Layer("android-ui", 5, "experiment 5", 3, "variant 3");
        final Layer androidListeningTime = new Layer("android-listen", 3, "experiment 3", 9, "variant 9");

        return Lists.newArrayList(androidUi, androidListeningTime);
    }
}
