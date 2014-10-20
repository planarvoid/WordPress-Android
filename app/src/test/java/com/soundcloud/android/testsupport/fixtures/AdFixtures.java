package com.soundcloud.android.testsupport.fixtures;

import com.google.common.collect.Lists;
import com.soundcloud.android.ads.ApiAdWrapper;
import com.soundcloud.android.ads.ApiAdsForTrack;
import com.soundcloud.android.ads.ApiAudioAd;
import com.soundcloud.android.ads.ApiInterstitial;

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
