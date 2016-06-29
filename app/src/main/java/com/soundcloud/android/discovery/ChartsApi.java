package com.soundcloud.android.discovery;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
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

    Observable<ApiChart> chartTracks(ChartType type, String genre) {
        final ApiRequest request = ApiRequest
                .get(ApiEndpoints.CHARTS.path())
                .addQueryParam("type", type.value())
                .addQueryParam("genre", genre)
                .forPrivateApi()
                .build();
        return apiClientRx.mappedResponse(request, ApiChart.class);
    }

    Observable<ApiChart> chartTracks(String nextPageLink) {
        final ApiRequest request = ApiRequest.get(nextPageLink).forPrivateApi().build();
        return apiClientRx.mappedResponse(request, ApiChart.class);
    }

    Observable<ModelCollection<ApiChart>> allGenres() {
        final ApiRequest request = ApiRequest
                .get(ApiEndpoints.CHARTS_GENRES.path())
                .addQueryParam("type", ChartType.TRENDING.value())
                .forPrivateApi()
                .build();
        return apiClientRx.mappedResponse(request, new TypeToken<ModelCollection<ApiChart>> (){
        });
    }
}
