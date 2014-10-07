package com.soundcloud.android.testsupport.blueprints;

import com.soundcloud.android.ads.ApiAdsForTrack;
import com.soundcloud.android.ads.ApiAudioAd;
import com.soundcloud.android.ads.ApiLeaveBehind;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.callback.ConstructorCallback;

@Blueprint(ApiAdsForTrack.class)
public class AdsForTrackBlueprint {

    ConstructorCallback constructor = new ConstructorCallback() {
        @Override
        public Object createInstance() {
            return new ApiAdsForTrack(
                    ModelFixtures.create(ApiAudioAd.class),
                    ModelFixtures.create(ApiLeaveBehind.class)
            );
        }
    };
}
