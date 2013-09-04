package com.soundcloud.android.api.http;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.net.MediaType;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.model.ModelCollection;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SoundCloudRxHttpClient extends ScheduledOperations implements RxHttpClient  {
    private static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/vnd.com.soundcloud.mobile.v%d+json";

    private final JsonTransformer mJsonTransformer;
    private final WrapperFactory mWrapperFactory;
    private final HttpProperties mHttpProperties;

    public SoundCloudRxHttpClient() {
        this(ScSchedulers.API_SCHEDULER);
    }

    public SoundCloudRxHttpClient(Scheduler scheduler) {
        this(new JacksonJsonTransformer(), new WrapperFactory(SoundCloudApplication.instance),
                new HttpProperties(SoundCloudApplication.instance.getResources()));
        subscribeOn(scheduler);
    }

    @VisibleForTesting
    protected SoundCloudRxHttpClient(JsonTransformer jsonTransformer, WrapperFactory wrapperFactory,
                                     HttpProperties httpProperties) {
        mJsonTransformer = jsonTransformer;
        mWrapperFactory = wrapperFactory;
        mHttpProperties = httpProperties;
    }


    @Override
    public Observable<APIResponse> fetchResponse(final APIRequest apiRequest) {
        return schedule(Observable.create(new Func1<Observer<APIResponse>, Subscription>() {
            @Override
            public Subscription call(Observer<APIResponse> observer) {
                BooleanSubscription subscription = new BooleanSubscription();
                final APIResponse response = executeRequest(apiRequest);
                if (!subscription.isUnsubscribed()) {
                    observer.onNext(response);
                    observer.onCompleted();
                }
                return subscription;
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

    @Override
    public <ModelType> Observable<Observable<ModelType>> fetchPagedModels(final APIRequest apiRequest) {
        return Observable.create(new Func1<Observer<Observable<ModelType>>, Subscription>() {
            @Override
            public Subscription call(final Observer<Observable<ModelType>> observableObserver) {
                observableObserver.onNext(getNextPage(observableObserver, apiRequest));
                return Subscriptions.empty();
            }
        });
    }

    private <ModelType> Observable<ModelType> getNextPage(final Observer<Observable<ModelType>> pageObserver, final APIRequest apiRequest) {
        return schedule(Observable.create(new Func1<Observer<ModelType>, Subscription>() {
            @Override
            public Subscription call(Observer<ModelType> itemObserver) {
                try {
                    Observable<ModelCollection<ModelType>> pageRequest = fetchModels(apiRequest);
                    final ModelCollection<ModelType> page = pageRequest.toBlockingObservable().last();

                    // emit items
                    RxUtils.emitIterable(itemObserver, page.getCollection());
                    itemObserver.onCompleted();

                    // emit next page or done
                    if (page.getNextLink().isPresent()) {
                        // TODO, honor params from initial request
                        final APIRequest<?> nextRequest = SoundCloudAPIRequest.RequestBuilder.get(page.getNextLink().get().getHref())
                                .forPrivateAPI(1)
                                .forResource(apiRequest.getResourceType()).build();
                        pageObserver.onNext(getNextPage(pageObserver, nextRequest));
                    } else {
                        pageObserver.onCompleted();
                    }
                } catch (Exception e) {
                    itemObserver.onError(e);
                }
                return Subscriptions.empty();
            }
        }));
    }

    private <ModelType> Observable<ModelType> mapResponseToModels(final APIRequest apiRequest, final APIResponse apiResponse) {
        return Observable.create(new Func1<Observer<ModelType>, Subscription>() {
            @Override
            public Subscription call(Observer<ModelType> observer) {
                TypeToken resourceType = apiRequest.getResourceType();
                if (resourceType != null && apiResponse.hasResponseBody()) {
                    Object resource = parseJsonResponse(apiResponse, apiRequest);
                    @SuppressWarnings("unchecked")
                    Collection<ModelType> resources = isRequestedResourceTypeOfCollection(resourceType) ? Collection.class.cast(resource) : Collections.singleton(resource);
                    RxUtils.emitIterable(observer, resources);
                } else if (resourceType != null && !apiResponse.hasResponseBody()) {
                    throw APIRequestException.badResponse(apiRequest, apiResponse, "Response could not be unmarshaled into resource type as response is empty");
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }
        });
    }

    private boolean isRequestedResourceTypeOfCollection(TypeToken resourceType) {
        return Collection.class.isAssignableFrom(resourceType.getRawType());
    }

    private Object parseJsonResponse(APIResponse apiResponse, APIRequest apiRequest) {
        Object resource;
        try {
            resource = mJsonTransformer.fromJson(apiResponse.getResponseBody(), apiRequest.getResourceType());
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
    private APIResponse executeRequest(APIRequest apiRequest) {

        ApiWrapper apiWrapper = mWrapperFactory.createWrapper(apiRequest);
        try {
            HttpMethod httpMethod = HttpMethod.valueOf(apiRequest.getMethod().toUpperCase());
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
        String baseUriPath = apiRequest.isPrivate() ? mHttpProperties.getApiMobileBaseUriPath() : ScTextUtils.EMPTY_STRING;
        Request request = Request.to(baseUriPath + apiRequest.getUriPath());
        final Multimap<String,String> queryParameters = apiRequest.getQueryParameters();

        Map<String, String> transformedParameters = Maps.toMap(queryParameters.keySet(), new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable String input) {
                return Joiner.on(",").join(queryParameters.get(input));
            }
        });

        for(String key : transformedParameters.keySet()){
            request.add(key, transformedParameters.get(key));
        }

        final Object content = apiRequest.getContent();
        if (content != null) {
            request.withContent(mJsonTransformer.toJson(content), MediaType.JSON_UTF_8.toString());
        }
        return request;
    }

    protected static class WrapperFactory {
        private final Context mContext;
        private final HttpProperties mHttpProperties;
        private final AccountOperations mAccountOperations;
        private final ApplicationProperties mApplicationProperties;

        public WrapperFactory(Context context){
            this(context, new HttpProperties(context.getResources()), new AccountOperations(context),
                    new ApplicationProperties(context.getResources()));
        }
        @VisibleForTesting
        public WrapperFactory(Context context, HttpProperties httpProperties, AccountOperations accountOperations,
                              ApplicationProperties applicationProperties) {
            mContext = context;
            mHttpProperties = httpProperties;
            mAccountOperations = accountOperations;
            mApplicationProperties = applicationProperties;
        }

        public ApiWrapper createWrapper(APIRequest apiRequest){
            Wrapper wrapper = new Wrapper(mContext, mHttpProperties,mAccountOperations, mApplicationProperties);
            String acceptContentType = apiRequest.isPrivate() ? format(PRIVATE_API_ACCEPT_CONTENT_TYPE, apiRequest.getVersion()) : MediaType.JSON_UTF_8.toString();
            wrapper.setDefaultContentType(acceptContentType);
            return wrapper;
        }
    }

}
