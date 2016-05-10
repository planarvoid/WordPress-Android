package com.soundcloud.android.discovery;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import rx.Observable;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class RecommendedStationsOperations {

    private final FeatureFlags featureFlags;

    @Inject
    public RecommendedStationsOperations(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public Observable<List<DiscoveryItem>> getRecommendations() {
        if (featureFlags.isEnabled(Flag.RECOMMENDED_STATIONS)) {
            return Observable.just(Collections.<DiscoveryItem>singletonList(new RecommendedStationsBucket()));
        } else {
            return Observable.empty();
        }
    }
}
