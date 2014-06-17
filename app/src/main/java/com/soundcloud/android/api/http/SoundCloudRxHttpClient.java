package com.soundcloud.android.api.http;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.properties.ApplicationProperties;
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

public class SoundCloudRxHttpClient extends ScheduledOperations implements RxHttpClient {
    private static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/vnd.com.soundcloud.mobile.v%d+json";
    // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
    private static final String PUBLIC_API_ACCEPT_CONTENT_TYPE = "application/json";

    @VisibleForTesting
    static final String URI_APP_PREFIX = "/app";

    private final JsonTransformer jsonTransformer;
    private final WrapperFactory wrapperFactory;
    private final HttpProperties httpProperties;

    public SoundCloudRxHttpClient() {
        this(ScSchedulers.API_SCHEDULER);
    }

    public SoundCloudRxHttpClient(Scheduler scheduler) {
        this(new JacksonJsonTransformer(), new WrapperFactory(SoundCloudApplication.instance),
                new HttpProperties(SoundCloudApplication.instance.getResources()));
        subscribeOn(scheduler);
    }

    public SoundCloudRxHttpClient(Scheduler scheduler, JsonTransformer jsonTransformer,
                                  Context context, HttpProperties httpProperties) {
        this(jsonTransformer, new WrapperFactory(context), httpProperties);
        subscribeOn(scheduler);
    }

    @VisibleForTesting
    protected SoundCloudRxHttpClient(JsonTransformer jsonTransformer, WrapperFactory wrapperFactory,
                                     HttpProperties httpProperties) {
        this.jsonTransformer = jsonTransformer;
        this.wrapperFactory = wrapperFactory;
        this.httpProperties = httpProperties;
    }


    @Override
    public Observable<APIResponse> fetchResponse(final APIRequest apiRequest) {
        return schedule(Observable.create(new Observable.OnSubscribe<APIResponse>() {
            @Override
            public void call(Subscriber<? super APIResponse> subscriber) {
                try {
                    final APIResponse response = executeRequest(apiRequest);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(response);
                        subscriber.onCompleted();
                    }
                } catch (APIRequestException e) {
                    subscriber.onError(e);
                }
            }

        }));
    }

    @Override
    public <ModelType> Observable<ModelType> fetchModels(final APIRequest apiRequest) {
        return fetchResponse(apiRequest).flatMap(new Func1<APIResponse, Observable<ModelType>>() {
            @Override
            public Observable<ModelType> call(APIResponse apiResponse) {
                return mapResponseToModels(apiRequest, apiResponse);
            }
        });
    }

    private <ModelType> Observable<ModelType> mapResponseToModels(final APIRequest apiRequest, final APIResponse apiResponse) {
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
                        subscriber.onError(APIRequestException.badResponse(apiRequest, apiResponse, "Response could not be unmarshaled into resource type as response is empty"));
                    }
                    subscriber.onCompleted();
                } catch (APIRequestException apiException) {
                    subscriber.onError(apiException);
                }
            }
        });
    }

    private boolean isRequestedResourceTypeOfCollection(TypeToken resourceType) {
        return Collection.class.isAssignableFrom(resourceType.getRawType());
    }

    private Object parseJsonResponse(APIResponse apiResponse, APIRequest apiRequest) throws APIRequestException {
        Object resource;
        try {
            resource = jsonTransformer.fromJson(apiResponse.getResponseBody(), apiRequest.getResourceType());
        } catch (Exception e) {
            throw APIRequestException.badResponse(apiRequest, apiResponse, e);
        }

        if (resource == null || UnknownResource.class.isAssignableFrom(resource.getClass())) {
            throw APIRequestException.badResponse(apiRequest, apiResponse, "Response could not be deserialised");
        }
        return resource;
    }

    /*
    TODO Version headers, gzip acceptance, connectivity check, proxy information
     */
    private APIResponse executeRequest(APIRequest apiRequest) throws APIRequestException {

        ApiWrapper apiWrapper = wrapperFactory.createWrapper(apiRequest);
        try {
            HttpMethod httpMethod = HttpMethod.valueOf(apiRequest.getMethod().toUpperCase(Locale.US));
            Log.d(this, "executing request: " + apiRequest);
            HttpResponse response = httpMethod.execute(apiWrapper, createSCRequest(apiRequest));
            String responseBody = EntityUtils.toString(response.getEntity(), Charsets.UTF_8.name());
            APIResponse apiResponse = new APIResponse(response.getStatusLine().getStatusCode(),
                    responseBody, response.getAllHeaders());
            if (apiResponse.accountIsRateLimited()) {
                //TODO We need to improve on this, so that further requests are prevented. Maybe have an event system based on RX
                throw APIRequestException.rateLimited(apiRequest, apiResponse);
            } else if (apiResponse.isNotSuccess()) {
                throw APIRequestException.badResponse(apiRequest, apiResponse);
            }
            return apiResponse;

        } catch (CloudAPI.InvalidTokenException e) {
            throw APIRequestException.authError(apiRequest, e);
        } catch (IOException e) {
            throw APIRequestException.networkCommsError(apiRequest, e);
        }
    }

    private Request createSCRequest(APIRequest<?> apiRequest) throws IOException {
        final boolean needsPrefix = apiRequest.isPrivate() && !apiRequest.getEncodedPath().startsWith(URI_APP_PREFIX);
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

        for (String key : transformedParameters.keySet()) {
            request.add(key, transformedParameters.get(key));
        }

        final Object content = apiRequest.getContent();
        if (content != null) {
            request.withContent(jsonTransformer.toJson(content), PUBLIC_API_ACCEPT_CONTENT_TYPE);
        }
        return request;
    }

    protected static class WrapperFactory {
        private final Context context;
        private final HttpProperties httpProperties;
        private final AccountOperations accountOperations;
        private final ApplicationProperties applicationProperties;

        public WrapperFactory(Context context) {
            this(context, new HttpProperties(context.getResources()),
                    SoundCloudApplication.fromContext(context).getAccountOperations(),
                    new ApplicationProperties(context.getResources()));
        }

        @VisibleForTesting
        public WrapperFactory(Context context, HttpProperties httpProperties, AccountOperations accountOperations,
                              ApplicationProperties applicationProperties) {
            this.context = context;
            this.httpProperties = httpProperties;
            this.accountOperations = accountOperations;
            this.applicationProperties = applicationProperties;
        }

        public ApiWrapper createWrapper(APIRequest apiRequest) {
            PublicApiWrapper publicApiWrapper = new PublicApiWrapper(context, httpProperties, accountOperations, applicationProperties);
            String acceptContentType = apiRequest.isPrivate()
                    ? format(Locale.US, PRIVATE_API_ACCEPT_CONTENT_TYPE, apiRequest.getVersion())
                    : PUBLIC_API_ACCEPT_CONTENT_TYPE;
            publicApiWrapper.setDefaultContentType(acceptContentType);
            return publicApiWrapper;
        }
    }

}
