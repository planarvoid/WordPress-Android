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
import java.util.List;

public abstract class BulkFetchCommand<ApiModel> extends Command<List<Urn>, ModelCollection<ApiModel>, BulkFetchCommand<ApiModel>> {

    private final ApiClient apiClient;

    public BulkFetchCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ModelCollection<ApiModel> call() throws ApiRequestException, IOException, ApiMapperException {
        return apiClient.fetchMappedResponse(buildRequest());
    }

    @Override
    public Observable<ModelCollection<ApiModel>> toObservable() {
        return super.toObservable().subscribeOn(ScSchedulers.API_SCHEDULER);
    }

    protected abstract ApiRequest<ModelCollection<ApiModel>> buildRequest();
}
