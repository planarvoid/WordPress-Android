package com.soundcloud.android.api.http;


import rx.Observable;

public interface RxHttpClient {

    <ModelType> Observable<ModelType> getResources(APIRequest apiRequest);
}
