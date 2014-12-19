package com.soundcloud.android.api;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.inject.Inject;
import java.io.IOException;

public class ApiScheduler {

    private final ApiClient apiClient;
    private final Scheduler scheduler;

    @Inject
    public ApiScheduler(ApiClient apiClient, Scheduler scheduler) {
        this.apiClient = apiClient;
        this.scheduler = scheduler;
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
        }).subscribeOn(scheduler);
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
        }).subscribeOn(scheduler);
    }
}
