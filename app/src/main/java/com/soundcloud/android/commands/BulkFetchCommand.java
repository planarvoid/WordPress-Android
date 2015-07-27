package com.soundcloud.android.commands;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BulkFetchCommand<ApiModel> extends LegacyCommand<List<Urn>, Collection<ApiModel>, BulkFetchCommand<ApiModel>> {

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
    public Collection<ApiModel> call() throws ApiRequestException, IOException, ApiMapperException {
        int pageIndex = 0;
        final Collection<ApiModel> results = new ArrayList<>(input.size());
        do {
            final int startIndex = pageIndex * pageSize;
            final int endIndex = Math.min(input.size(), (++pageIndex) * pageSize);
            final ApiRequest request = buildRequest(input.subList(startIndex, endIndex));

            CollectionUtils.addAll(results, apiClient.fetchMappedResponse(request, provideResourceType()));

        } while (pageIndex * pageSize < input.size());
        return results;
    }

    @Override
    public Observable<Collection<ApiModel>> toObservable() {
        return super.toObservable().subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    // TODO : When should make this use ModelCollection, as soon as we move users to api-mobile
    protected abstract TypeToken<? extends Iterable<ApiModel>> provideResourceType();

    protected abstract ApiRequest buildRequest(List<Urn> urnPage);
}
