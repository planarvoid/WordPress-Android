package com.soundcloud.android.olddiscovery.charts;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Observable;

import javax.inject.Inject;

class ChartsApi {
    private final ApiClientRxV2 apiClientRx;

    @Inject
    ChartsApi(ApiClientRxV2 apiClientRx) {
        this.apiClientRx = apiClientRx;
    }

    Observable<ApiChart<ApiTrack>> chartTracks(ChartType type, String genre) {
        final ApiRequest request = ApiRequest
                .get(ApiEndpoints.CHARTS.path())
                .addQueryParam("type", type.value())
                .addQueryParam("genre", genre)
                .forPrivateApi()
                .build();
        return getMappedResponse(request);
    }

    private Observable<ApiChart<ApiTrack>> getMappedResponse(ApiRequest request) {
        return apiClientRx.mappedResponse(request, new TypeToken<ApiChart<ApiTrack>>() {
        }).toObservable();
    }
}
