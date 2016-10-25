package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;

import javax.inject.Inject;

public class ChartsApi {
    private final ApiClientRx apiClientRx;

    @Inject
    public ChartsApi(ApiClientRx apiClientRx) {
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
        });
    }
}
