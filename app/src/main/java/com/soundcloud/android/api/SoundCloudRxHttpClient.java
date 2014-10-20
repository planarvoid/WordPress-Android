package com.soundcloud.android.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

import android.content.Context;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Deprecated
public class SoundCloudRxHttpClient extends ScheduledOperations implements RxHttpClient {

    private final JsonTransformer jsonTransformer;
    private final ApiWrapperFactory wrapperFactory;
    private final HttpProperties httpProperties;

    public SoundCloudRxHttpClient() {
        this(ScSchedulers.API_SCHEDULER);
    }

    public SoundCloudRxHttpClient(Scheduler scheduler) {
        this(new JacksonJsonTransformer(), new ApiWrapperFactory(SoundCloudApplication.instance),
                new HttpProperties(SoundCloudApplication.instance.getResources()));
        subscribeOn(scheduler);
    }

    public SoundCloudRxHttpClient(Scheduler scheduler, JsonTransformer jsonTransformer,
                                  Context context, HttpProperties httpProperties) {
        this(jsonTransformer, new ApiWrapperFactory(context), httpProperties);
        subscribeOn(scheduler);
    }

    @VisibleForTesting
    protected SoundCloudRxHttpClient(JsonTransformer jsonTransformer, ApiWrapperFactory wrapperFactory,
                                     HttpProperties httpProperties) {
        this.jsonTransformer = jsonTransformer;
        this.wrapperFactory = wrapperFactory;
        this.httpProperties = httpProperties;
    }


    @Override
    public Observable<ApiResponse> fetchResponse(final ApiRequest apiRequest) {
        return schedule(Observable.create(new Observable.OnSubscribe<ApiResponse>() {
            @Override
            public void call(Subscriber<? super ApiResponse> subscriber) {
                try {
                    final ApiResponse response = executeRequest(apiRequest);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    }
                } catch (ApiRequestException e) {
                    subscriber.onError(e);
                }
            }

        }));
    }

    @Override
    public <ModelType> Observable<ModelType> fetchModels(final ApiRequest apiRequest) {
        return fetchResponse(apiRequest).flatMap(new Func1<ApiResponse, Observable<ModelType>>() {
            @Override
            public Observable<ModelType> call(ApiResponse apiResponse) {
                return mapResponseToModels(apiRequest, apiResponse);
            }
        });
    }

    private <ModelType> Observable<ModelType> mapResponseToModels(final ApiRequest apiRequest, final ApiResponse apiResponse) {
        return Observable.create(new Observable.OnSubscribe<ModelType>() {
            @Override
            public void call(Subscriber<? super ModelType> subscriber) {
                TypeToken resourceType = apiRequest.getResourceType();
                try {
                    if (resourceType != null && apiResponse.hasResponseBody()) {
                        Object resource = parseJsonResponse(apiResponse, apiRequest);
                        @SuppressWarnings("unchecked")
                        Collection<ModelType> resources = isRequestedResourceTypeOfCollection(resourceType) ? Collection.class.cast(resource) : Collections.singleton(resource);
                        RxUtils.emitIterable(subscriber, resources);
                    } else if (resourceType != null && !apiResponse.hasResponseBody()) {
                        subscriber.onError(ApiRequestException.unexpectedResponse(apiRequest, "Response could not be unmarshaled into resource type as response is empty"));
                    }
                    subscriber.onCompleted();
                } catch (ApiRequestException apiException) {
                    subscriber.onError(apiException);
                }
            }
        });
    }

    private boolean isRequestedResourceTypeOfCollection(TypeToken resourceType) {
        return Collection.class.isAssignableFrom(resourceType.getRawType());
    }

    private Object parseJsonResponse(ApiResponse apiResponse, ApiRequest apiRequest) throws ApiRequestException {
        Object resource;
        try {
            resource = jsonTransformer.fromJson(apiResponse.getResponseBody(), apiRequest.getResourceType());
        } catch (Exception e) {
            throw ApiRequestException.unexpectedResponse(apiRequest, e);
        }

        if (resource == null || UnknownResource.class.isAssignableFrom(resource.getClass())) {
            throw ApiRequestException.unexpectedResponse(apiRequest, "Response could not be deserialised");
        }
        return resource;
    }

    /*
    TODO Version headers, gzip acceptance, connectivity check, proxy information
     */
    private ApiResponse executeRequest(ApiRequest apiRequest) throws ApiRequestException {

        ApiWrapper apiWrapper = wrapperFactory.createWrapper(apiRequest);
        try {
            HttpMethod httpMethod = HttpMethod.valueOf(apiRequest.getMethod().toUpperCase(Locale.US));
            Log.d(this, "executing request: " + apiRequest);
            HttpResponse response = httpMethod.execute(apiWrapper, createSCRequest(apiRequest));
            String responseBody = EntityUtils.toString(response.getEntity(), Charsets.UTF_8.name());
            final int statusCode = response.getStatusLine().getStatusCode();
            ApiResponse apiResponse = new ApiResponse(apiRequest, statusCode, responseBody);
            if (apiResponse.isNotSuccess()) {
                throw apiResponse.getFailure();
            }
            return apiResponse;

        } catch (CloudAPI.InvalidTokenException e) {
            throw ApiRequestException.authError(apiRequest, e);
        } catch (IOException e) {
            throw ApiRequestException.networkError(apiRequest, e);
        } catch (ApiMapperException e) {
            throw ApiRequestException.malformedInput(apiRequest, e);
        }
    }

    private Request createSCRequest(ApiRequest<?> apiRequest) throws ApiMapperException {
        final boolean needsPrefix = apiRequest.isPrivate() && !apiRequest.getEncodedPath().startsWith(ApiClient.URI_APP_PREFIX);
        String baseUriPath = needsPrefix ? httpProperties.getApiMobileBaseUriPath() : ScTextUtils.EMPTY_STRING;
        Request request = Request.to(baseUriPath + apiRequest.getEncodedPath());


        final Multimap<String, String> queryParameters = apiRequest.getQueryParameters();
        Map<String, String> transformedParameters = Maps.toMap(queryParameters.keySet(), new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable String input) {
                return Joiner.on(",").join(queryParameters.get(input));
            }
        });

        for (Map.Entry<String, String> entry : transformedParameters.entrySet()) {
            request.add(entry.getKey(), entry.getValue());
        }

        request.setHeaders(apiRequest.getHeaders());

        final Object content = apiRequest.getContent();
        if (content != null) {
            request.withContent(jsonTransformer.toJson(content), ApiClient.PUBLIC_API_ACCEPT_CONTENT_TYPE);
        }
        return request;
    }

}
