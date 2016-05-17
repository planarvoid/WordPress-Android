package com.soundcloud.android.discovery;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import rx.Observable;

import javax.inject.Inject;

class RecommendedStationsOperations {

    private final FeatureFlags featureFlags;

    @Inject
    public RecommendedStationsOperations(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public Observable<DiscoveryItem> stations() {
        if (featureFlags.isEnabled(Flag.RECOMMENDED_STATIONS)) {
            return Observable.<DiscoveryItem>just(new RecommendedStationsBucket());
        } else {
            return Observable.empty();
        }
    }

    public void clearData() {
    }
}
