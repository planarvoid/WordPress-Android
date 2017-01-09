package com.soundcloud.android.stream;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

class StreamHighlightsOperations {

    private static final int MIN_HIGHLIGHTS = 5;
    private final ApiClientRx apiClientRx;
    private final FeatureFlags featureFlags;
    private final Scheduler scheduler;

    @Inject
    StreamHighlightsOperations(ApiClientRx apiClientRx,
                               FeatureFlags featureFlags,
                               @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.featureFlags = featureFlags;
        this.scheduler = scheduler;
    }

    Observable<StreamItem.StreamHighlights> highlights() {
        if (featureFlags.isEnabled(Flag.STREAM_HIGHLIGHTS)) {
            ApiRequest apiRequest = ApiRequest.get(ApiEndpoints.STREAM_HIGHLIGHTS.path())
                                              .forPrivateApi().build();

            return apiClientRx.mappedResponse(apiRequest, new TypeToken<ModelCollection<ApiTrack>>() {
            })
                              .subscribeOn(scheduler)
                              .flatMap(apiTracks -> apiTracks.getCollection().size() < MIN_HIGHLIGHTS ?
                                                Observable.empty() :
                                                Observable.just(StreamItem.StreamHighlights.create(apiTracks.getCollection())));
        } else {
            return Observable.empty();
        }
    }
}
