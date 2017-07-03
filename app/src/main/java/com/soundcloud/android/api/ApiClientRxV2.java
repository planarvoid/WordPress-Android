package com.soundcloud.android.api;

import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Completable;
import io.reactivex.Single;

import javax.inject.Inject;
import java.io.IOException;

/**
 * This Client should probably be renamed back to `ApiClientRx` once we fully migrated to RxJava2
 */
public class ApiClientRxV2 {

    private final ApiClient apiClient;

    @Inject
    public ApiClientRxV2(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public Single<ApiResponse> response(final ApiRequest request) {
        return Single.create(emitter -> {
            final ApiResponse response = apiClient.fetchResponse(request);
            if (response.isSuccess()) {
                emitter.onSuccess(response);
            } else {
                if (!emitter.isDisposed()) {
                    emitter.onError(response.getFailure());
                }
            }
        });
    }

    public Completable ignoreResultRequest(final ApiRequest request) {
        return response(request).toCompletable();
    }

    public <T> Single<T> mappedResponse(final ApiRequest request, final TypeToken<T> resourceType) {
        return Single.create(emitter -> {
            try {
                final ApiResponse response = apiClient.fetchResponse(request);
                if (response.isSuccess()) {
                    emitter.onSuccess(apiClient.mapResponse(response, resourceType));
                } else {
                    if (!emitter.isDisposed()) {
                        emitter.onError(response.getFailure());
                    }
                }
            } catch (ApiRequestException | ApiMapperException | IOException e) {
                emitter.onError(e);
            }
        });
    }

    public <T> Single<T> mappedResponse(final ApiRequest request, final Class<T> resourceType) {
        return mappedResponse(request, TypeToken.of(resourceType));
    }
}
