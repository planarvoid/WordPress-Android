package com.soundcloud.android.stream;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;

import javax.inject.Inject;

class StreamHighlightsOperations {

    private final ApiClientRx apiClientRx;
    private final FeatureFlags featureFlags;

    @Inject
    public StreamHighlightsOperations(ApiClientRx apiClientRx, FeatureFlags featureFlags) {
        this.apiClientRx = apiClientRx;
        this.featureFlags = featureFlags;
    }

    Observable<StreamItem.StreamHighlights> highlights(){
        if (featureFlags.isEnabled(Flag.STREAM_HIGHLIGHTS)) {
            ApiRequest apiRequest = ApiRequest.get(ApiEndpoints.STREAM_HIGHLIGHTS.path())
                                              .forPrivateApi().build();
            return apiClientRx.mappedResponse(apiRequest, new TypeToken<ModelCollection<ApiTrack>>() {})
                              .subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER)
                              .map(apiTracks -> StreamItem.StreamHighlights.create(apiTracks.getCollection()));
        } else {
            return Observable.empty();
        }
    }
}
