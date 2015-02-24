package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.ads.DisplayProperties;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(DisplayProperties.class)
public class DisplayPropertiesBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new DisplayProperties(
                    "#111111",
                    "#222222",
                    "#333333",
                    "#444444",
                    "#555555",
                    "#666666"
            );
        }
    };

}
