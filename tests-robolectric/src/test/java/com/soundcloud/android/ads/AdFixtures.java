package com.soundcloud.android.ads;

import com.google.common.collect.Lists;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

public class AdFixtures {

    public static ApiAdsForTrack interstitial(){
        return new ApiAdsForTrack(Lists.newArrayList(
                new ApiAdWrapper(ModelFixtures.create(ApiInterstitial.class)))
        );
    }

    public static ApiAdsForTrack audioAd(){
        return new ApiAdsForTrack(Lists.newArrayList(
                new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class)))
        );
    }

    public static ApiAdsForTrack fullAdsForTrack(){
        return new ApiAdsForTrack(Lists.newArrayList(
                new ApiAdWrapper(ModelFixtures.create(ApiInterstitial.class)),
                new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class)))
        );
    }
}
