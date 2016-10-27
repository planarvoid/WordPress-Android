package com.soundcloud.android.discovery.charts;


import static com.soundcloud.android.storage.Tables.Charts.BUCKET_TYPE_ALL_GENRES;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiChartBucket;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

class ChartGenresSyncer implements Callable<Boolean> {

    private final ApiClient apiClient;
    private final StoreChartsCommand storeChartsCommand;

    @Inject
    ChartGenresSyncer(ApiClient apiClient, StoreChartsCommand storeChartsCommand) {
        this.apiClient = apiClient;
        this.storeChartsCommand = storeChartsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.CHARTS_GENRES.path())
                                             .addQueryParam("type", ChartType.TRENDING.value())
                                             .forPrivateApi().build();
        final ModelCollection<ApiChart<ApiImageResource>> apiCharts = getApiCharts(request);
        final List<ApiChartBucket> apiChartBuckets = singletonList(new ApiChartBucket(apiCharts.getCollection(),
                                                                                      BUCKET_TYPE_ALL_GENRES));
        final WriteResult writeResult = storeChartsCommand.call(apiChartBuckets);
        return writeResult.success();
    }

    private ModelCollection<ApiChart<ApiImageResource>> getApiCharts(ApiRequest request)
            throws IOException, ApiRequestException, ApiMapperException {
        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiChart<ApiImageResource>>>() {
        });
    }
}
