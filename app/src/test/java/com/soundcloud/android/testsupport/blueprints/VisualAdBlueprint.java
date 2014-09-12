package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.ads.DisplayProperties;
import com.soundcloud.android.ads.VisualAd;
import com.soundcloud.android.testsupport.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;

@Blueprint(VisualAd.class)
public class VisualAdBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            ModelFactory factory = TestHelper.getModelFactory();
            try {
                return new VisualAd(
                        "http://image.visualad.com",
                        "http://clickthrough.visualad.com",
                        Collections.<String>emptyList(),
                        Collections.<String>emptyList(),
                        factory.createModel(DisplayProperties.class)
                );
            } catch (CreateModelException e) {
                return null;
            }
        }
    };

}
