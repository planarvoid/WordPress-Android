package com.soundcloud.android.commands;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;

import java.io.IOException;

public abstract class ApiResourceCommand<I, O> extends Command<I, O> {

    private final ApiClient apiClient;

    public ApiResourceCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public O call() throws ApiRequestException, IOException, ApiMapperException {
        return apiClient.fetchMappedResponse(buildRequest());
    }

    @Override
    public Observable<O> toObservable(I input) {
        return super.toObservable(input).subscribeOn(ScSchedulers.API_SCHEDULER);
    }

    protected abstract ApiRequest<O> buildRequest();
}
