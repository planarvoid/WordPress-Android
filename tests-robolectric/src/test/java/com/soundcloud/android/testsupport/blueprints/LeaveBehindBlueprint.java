package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.ads.ApiLeaveBehind;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;

@Blueprint(ApiLeaveBehind.class)
public class LeaveBehindBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiLeaveBehind(
                    "adswizz:35",
                    "http://image.visualad.com",
                    "http://clickthrough.visualad.com",
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList()
            );
        }
    };
}
