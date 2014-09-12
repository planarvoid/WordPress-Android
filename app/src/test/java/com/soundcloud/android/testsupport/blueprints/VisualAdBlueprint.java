package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.ads.DisplayProperties;
import com.soundcloud.android.ads.VisualAd;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;

@Blueprint(VisualAd.class)
public class VisualAdBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new VisualAd(
                    "http://image.visualad.com",
                    "http://clickthrough.visualad.com",
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    ModelFixtures.create(DisplayProperties.class)
            );
        }
    };

}
