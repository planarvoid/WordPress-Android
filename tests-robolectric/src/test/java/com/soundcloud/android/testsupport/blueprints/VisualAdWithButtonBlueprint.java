package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.ads.DisplayProperties;
import com.soundcloud.android.ads.ApiVisualAdWithButton;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;

@Blueprint(ApiVisualAdWithButton.class)
public class VisualAdWithButtonBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiVisualAdWithButton(
                    "http://image.visualad.com",
                    "http://clickthrough.visualad.com",
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    ModelFixtures.create(DisplayProperties.class)
            );
        }
    };

}
