package com.soundcloud.android.ads;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

public class AdFixtures {

    public static ApiAdsForTrack fullAdsForTrack(){
        return new ApiAdsForTrack(newArrayList(
                new ApiAdWrapper(ModelFixtures.create(ApiInterstitial.class)),
                new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class)))
        );
    }
}
