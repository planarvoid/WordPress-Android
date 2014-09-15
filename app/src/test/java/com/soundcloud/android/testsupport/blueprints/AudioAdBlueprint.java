package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.LeaveBehind;
import com.soundcloud.android.ads.VisualAd;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

import java.util.Collections;

@Blueprint(AudioAd.class)
public class AudioAdBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new AudioAd(
                    "adswizz:ads:869",
                    ModelFixtures.create(ApiTrack.class),
                    ModelFixtures.create(VisualAd.class),
                    ModelFixtures.create(LeaveBehind.class),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList()
            );
        }
    };
}
