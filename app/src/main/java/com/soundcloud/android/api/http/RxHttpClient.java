package com.soundcloud.android.api.http;


import rx.Observable;

/**
 * Represent a HTTP client that returns the result using Rx Observables.
 */
public interface RxHttpClient {

    /**
     * Makes a request based on the {@link APIRequest} provided. If a resource type is specified
     * onNext will be called as necessary with the unmarshalled objects as well as onCompleted for successful requests.
     * If no resource type specified then onCompleted will be called for successful requests.
     * If any errors occurs (Not success response, unmarshalling issues),
     * an {@link APIRequestException} will be raised.
     * @param apiRequest The request to be made
     * @param <ModelType> The resource type (which can be of collection or single POJO)
     * @return An observable which delivers the result of the request
     */
    <ModelType> Observable<ModelType> fetchModels(APIRequest apiRequest) throws APIRequestException;

    Observable<APIResponse> fetchResponse(APIRequest apiRequest) throws APIRequestException;;
}
