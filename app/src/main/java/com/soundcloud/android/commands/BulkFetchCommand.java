package com.soundcloud.android.commands;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class BulkFetchCommand<ApiModel> extends Command<List<Urn>, List<ApiModel>, BulkFetchCommand<ApiModel>> {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final ApiClient apiClient;
    private final int pageSize;

    public BulkFetchCommand(ApiClient apiClient) {
        this(apiClient, DEFAULT_PAGE_SIZE);
    }

    public BulkFetchCommand(ApiClient apiClient, int pageSize) {
        this.apiClient = apiClient;
        this.pageSize = pageSize;
    }

    @Override
    public List<ApiModel> call() throws ApiRequestException, IOException, ApiMapperException {
        int pageIndex = 0;
        final List<ApiModel> results = new ArrayList<>(input.size());
        do {
            final int startIndex = pageIndex * pageSize;
            final int endIndex = Math.min(input.size(), (++pageIndex) * pageSize);
            final ApiRequest<ModelCollection<ApiModel>> request = buildRequest(input.subList(startIndex, endIndex));
            results.addAll(apiClient.fetchMappedResponse(request).getCollection());

        } while (pageIndex * pageSize < input.size());
        return results;
    }

    @Override
    public Observable<List<ApiModel>> toObservable() {
        return super.toObservable().subscribeOn(ScSchedulers.API_SCHEDULER);
    }

    protected abstract ApiRequest<ModelCollection<ApiModel>> buildRequest(List<Urn> urnPage);
}
