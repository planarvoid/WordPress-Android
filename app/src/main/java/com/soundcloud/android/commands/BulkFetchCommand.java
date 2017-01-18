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

public abstract class BulkFetchCommand<ApiModel, OutputModel>
        extends LegacyCommand<List<Urn>, Collection<OutputModel>, BulkFetchCommand<ApiModel, OutputModel>> {

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
    public Collection<OutputModel> call() throws ApiRequestException, IOException, ApiMapperException {
        final List<Urn> validUrns = newArrayList(filter(input, VALID_URN_PREDICATE));
        final Collection<ApiModel> results = new ArrayList<>(validUrns.size());
        final List<List<Urn>> batchesOfUrns = partition(validUrns, pageSize);

        for (List<Urn> urns : batchesOfUrns) {
            final ApiRequest request = buildRequest(urns);
            Iterables.addAll(results, apiClient.fetchMappedResponse(request, provideResourceType()));
        }

        return transformResults(results);
    }

    // TODO : Remove this when we are returning all Api-Mobile entities and don't have to transform
    protected abstract Collection<OutputModel> transformResults(Collection<ApiModel> results);

    @Override
    public Observable<Collection<OutputModel>> toObservable() {
        return super.toObservable().subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    // TODO : When should make this use ModelCollection, as soon as we move users to api-mobile
    protected abstract TypeToken<? extends Iterable<ApiModel>> provideResourceType();

    protected abstract ApiRequest buildRequest(List<Urn> urnPage);

}
