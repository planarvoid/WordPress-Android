package com.soundcloud.android.ads;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;

@Blueprint(ApiInterstitial.class)
public class ApiInterstitialBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiInterstitial(
                    "adswizz:35",
                    "http://image.visualad.com",
                    "http://clickthrough.visualad.com",
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList()
            );
        }
    };
}
