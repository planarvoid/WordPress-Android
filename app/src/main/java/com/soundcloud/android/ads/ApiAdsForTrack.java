package com.soundcloud.android.ads;


import static com.google.common.collect.Lists.newArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApiAdsForTrack {

    @Nullable private final ApiAudioAd apiAudioAd;
    @Nullable private final ApiLeaveBehind interstitial;

    @JsonCreator
    public ApiAdsForTrack(@Nullable ApiAudioAd apiAudioAd) {
        this.apiAudioAd = apiAudioAd;
        this.interstitial = new ApiLeaveBehind(
                "adswizz:ads:1176",
                "https://va.sndcdn.com/min/1024x1024-souncloud-mobile-insertitial-v2-a.jpg",
                "http://www.squarespace.com",
                newArrayList("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A1176%3Bview_key%3A1412107801030769%3Bzone_id%3A58&loc=&listenerId=f56c156ee01040f5bd84b26ba2c4d217&sessionId=973c285fa3cc839df9ea8d69945a98&ip=%3A%3Affff%3A65.219.232.12&OAGEO=dXMlN0NueSU3Q2Jyb29rbHluJTdDJTdDNDAuNjUwMTAwNzA4MDA3ODElN0MtNzMuOTQ5NjAwMjE5NzI2NTYlN0M1MDElN0M3MTglN0MlN0MlM0ElM0FmZmZmJTNBNjUuMjE5LjIzMi4xMiU3Q3Zlcml6b24rYnVzaW5lc3M=&user_agent=curl%2F7.30.0&cbs=3719895"),
                newArrayList("https://promoted.soundcloud.com/track?reqType=SCAdClicked&protocolVersion=2.0&adId=1176&zoneId=58&cb=4c99f4c6cd4c4c50979844130f8efe22")
        );
    }

    public ApiAdsForTrack(@Nullable ApiAudioAd apiAudioAd, @Nullable ApiLeaveBehind interstitial) {
        this.apiAudioAd = apiAudioAd;
        this.interstitial = interstitial;
    }

    public boolean hasAudioAd() {
        return apiAudioAd != null;
    }

    public boolean hasInterstitialAd() {
        return interstitial != null;
    }

    @NotNull
    public ApiAudioAd audioAd() {
        if (apiAudioAd == null) {
            throw new IllegalStateException("Audio ad is not present");
        }
        return apiAudioAd;
    }

    @NotNull
    public ApiLeaveBehind interstitialAd() {
        if (interstitial == null) {
            throw new IllegalStateException("Interstitial ad is not present");
        }
        return interstitial;
    }
}
