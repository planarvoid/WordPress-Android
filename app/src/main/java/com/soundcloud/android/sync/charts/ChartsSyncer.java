package com.soundcloud.android.sync.charts;


import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;

public class ChartsSyncer implements Callable<Boolean> {

    private final ApiClient apiClient;
    private final StoreChartsCommand storeChartsCommand;

    @Inject
    public ChartsSyncer(ApiClient apiClient, StoreChartsCommand storeChartsCommand) {
        this.apiClient = apiClient;
        this.storeChartsCommand = storeChartsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.CHARTS_FEATURED.path()).forPrivateApi().build();
        final ApiChartBucket apiCharts = getApiCharts(request);
        final WriteResult writeResult = storeChartsCommand.call(apiCharts);
        return writeResult.success();
    }

    private ApiChartBucket getApiCharts(ApiRequest request)
            throws IOException, ApiRequestException, ApiMapperException {
        return apiClient.fetchMappedResponse(request, new TypeToken<ApiChartBucket>() {
        });
    }
}
