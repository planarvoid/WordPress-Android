package com.soundcloud.android.api;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import javax.inject.Inject;

public class ApiScheduler {

    private final ApiClient apiClient;
    private final Scheduler scheduler;

    @Inject
    public ApiScheduler(ApiClient apiClient, Scheduler scheduler) {
        this.apiClient = apiClient;
        this.scheduler = scheduler;
    }

    public <T> Observable<T> mappedResponse(final ApiRequest<T> request) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                try {
                    final ApiResponse response = apiClient.fetchResponse(request);
                    if (response.isSuccess()) {
                        subscriber.onNext(apiClient.mapResponse(request, response));
                    } else {
                        subscriber.onError(response.getFailure());
                    }
                    subscriber.onCompleted();
                } catch (ApiMapperException e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(scheduler);
    }
}
