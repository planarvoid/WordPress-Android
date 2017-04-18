package com.soundcloud.android.api;

import com.soundcloud.java.reflect.TypeToken;
import rx.Emitter;
import rx.Observable;

import javax.inject.Inject;
import java.io.IOException;

@Deprecated // use ApiClientRxV2 instead
public class ApiClientRx {

    private final ApiClient apiClient;

    @Inject
    public ApiClientRx(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Deprecated // use ApiClientRxV2 instead
    public ApiClient getApiClient() {
        return apiClient;
    }

    @Deprecated // use ApiClientRxV2 instead
    public Observable<ApiResponse> response(final ApiRequest request) {
        return Observable.create(emitter -> {
            final ApiResponse response = apiClient.fetchResponse(request);
            if (response.isSuccess()) {
                emitter.onNext(response);
                emitter.onCompleted();
            } else {
                emitter.onError(response.getFailure());
            }
        }, Emitter.BackpressureMode.ERROR);
    }

    @Deprecated // use ApiClientRxV2 instead
    public <T> Observable<T> mappedResponse(final ApiRequest request, final TypeToken<T> resourceType) {
        return Observable.create(emitter -> {
            try {
                final ApiResponse response = apiClient.fetchResponse(request);
                if (response.isSuccess()) {
                    emitter.onNext(apiClient.mapResponse(response, resourceType));
                    emitter.onCompleted();
                } else {
                    emitter.onError(response.getFailure());
                }
            } catch (ApiRequestException | ApiMapperException | IOException e) {
                emitter.onError(e);
            }
        }, Emitter.BackpressureMode.ERROR);
    }

    @Deprecated // use ApiClientRxV2 instead
    public <T> Observable<T> mappedResponse(final ApiRequest request, final Class<T> resourceType) {
        return mappedResponse(request, TypeToken.of(resourceType));
    }
}
