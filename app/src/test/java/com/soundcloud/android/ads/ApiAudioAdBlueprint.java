package com.soundcloud.android.ads;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;

@Blueprint(ApiAudioAd.class)
public class ApiAudioAdBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiAudioAd(
                    "adswizz:ads:869",
                    ModelFixtures.create(ApiTrack.class),
                    ModelFixtures.create(ApiCompanionAd.class),
                    ModelFixtures.create(ApiLeaveBehind.class),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList()
            );
        }
    };
}
