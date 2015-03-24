package com.soundcloud.android.api;

import rx.Observable;
import rx.Subscriber;

import javax.inject.Inject;
import java.io.IOException;

public class ApiClientRx {

    private final ApiClient apiClient;

    @Inject
    public ApiClientRx(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public Observable<ApiResponse> response(final ApiRequest<?> request) {
        return Observable.create(new Observable.OnSubscribe<ApiResponse>() {
            @Override
            public void call(Subscriber<? super ApiResponse> subscriber) {
                final ApiResponse response = apiClient.fetchResponse(request);
                if (response.isSuccess()) {
                    subscriber.onNext(response);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(response.getFailure());
                }
            }
        });
    }

    public <T> Observable<T> mappedResponse(final ApiRequest<T> request) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                try {
                    final ApiResponse response = apiClient.fetchResponse(request);
                    if (response.isSuccess()) {
                        subscriber.onNext(apiClient.mapResponse(request, response));
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(response.getFailure());
                    }
                } catch (ApiRequestException | ApiMapperException | IOException e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
