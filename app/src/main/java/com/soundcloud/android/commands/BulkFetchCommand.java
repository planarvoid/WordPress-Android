package com.soundcloud.android.commands;

import static com.soundcloud.android.utils.Urns.VALID_URN_PREDICATE;
import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.partition;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BulkFetchCommand<ApiModel>
        extends LegacyCommand<List<Urn>, Collection<ApiModel>, BulkFetchCommand<ApiModel>> {

    private static final int DEFAULT_PAGE_SIZE = 100;

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
        final List<Urn> validUrns = newArrayList(filter(input, VALID_URN_PREDICATE));
        final Collection<ApiModel> results = new ArrayList<>(validUrns.size());
        final List<List<Urn>> batchesOfUrns = partition(validUrns, pageSize);

        for (List<Urn> urns : batchesOfUrns) {
            final ApiRequest request = buildRequest(urns);
            Iterables.addAll(results, apiClient.fetchMappedResponse(request, provideResourceType()));
        }

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
