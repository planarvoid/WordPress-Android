package com.soundcloud.android.api.http;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.net.MediaType;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class SoundCloudRxHttpClient extends ScheduledOperations implements RxHttpClient  {
    private static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/vnd.com.soundcloud.mobile.v%d+json";

    private final JsonTransformer mJsonTransformer;
    private final Context mContext;
    private final WrapperFactory mWrapperFactory;

    public SoundCloudRxHttpClient(Context context) {
        this(context, new JacksonJsonTransformer(), new WrapperFactory(context));
    }

    @VisibleForTesting
    protected SoundCloudRxHttpClient(Context context, JsonTransformer jsonTransformer, WrapperFactory wrapperFactory) {
        mJsonTransformer = jsonTransformer;
        mContext = context;
        mWrapperFactory = wrapperFactory;
        subscribeOn(ScSchedulers.API_SCHEDULER);
    }


    @Override
    public <ModelType> Observable<ModelType> executeAPIRequest(final APIRequest apiRequest) {
        return schedule(Observable.create(new Func1<Observer<ModelType>, Subscription>() {
            /*
            TODO Version headers, gzip acceptance, connectivity check, proxy information
             */
            @Override
            public Subscription call(Observer<ModelType> observer) {

                APIResponse apiResponse = executeRequest(apiRequest);
                if (apiResponse.accountIsRateLimited()) {
                    //TODO We need to improve on this, so that further requests are prevented. Maybe have an event system based on RX
                    throw APIRequestException.rateLimited(apiRequest, apiResponse);
                } else if (apiResponse.isNotSuccess()) {
                    throw APIRequestException.badResponse(apiRequest, apiResponse);
                }

                if(apiRequest.getResourceType() != null && apiResponse.hasResponseBody()){
                    Object resource = parseJsonResponse(apiResponse, apiRequest);
                    notifyObserverOfResult(observer, apiRequest, resource);
                } else if(apiRequest.getResourceType() != null && !apiResponse.hasResponseBody()){
                    throw APIRequestException.badResponse(apiRequest,apiResponse,"Response could not be unmarshaled into resource type as response is empty");
                }
                observer.onCompleted();
                return Subscriptions.empty();

            }

        }));
    }

    private <T> void notifyObserverOfResult(Observer<T> observer, APIRequest apiRequest, Object resource) {
        @SuppressWarnings("unchecked")
        Collection<T> resources = isRequestedResourceTypeOfCollection(apiRequest) ? Collection.class.cast(resource) : Collections.singleton(resource);
        for (T modelType : resources) {
            observer.onNext(modelType);
        }
    }

    private boolean isRequestedResourceTypeOfCollection(APIRequest apiRequest) {
        return Collection.class.isAssignableFrom(apiRequest.getResourceType().getRawType());
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

    private APIResponse executeRequest(APIRequest apiRequest) {

        ApiWrapper apiWrapper = mWrapperFactory.createWrapper(mContext,apiRequest);
        try {
            HttpMethod httpMethod = HttpMethod.valueOf(apiRequest.getMethod().toUpperCase());
            HttpResponse response = httpMethod.execute(apiWrapper, createSCRequest(apiRequest));
            String responseBody = EntityUtils.toString(response.getEntity(), Charsets.UTF_8.name());
            return new APIResponse(response.getStatusLine().getStatusCode(),
                    responseBody, response.getAllHeaders());


        } catch (CloudAPI.InvalidTokenException e) {
            throw APIRequestException.authError(apiRequest, e);
        } catch (IOException e) {
            throw APIRequestException.networkCommsError(apiRequest, e);
        }
    }

    private Request createSCRequest(APIRequest<?> apiRequest) {
        return Request.to(apiRequest.getUriPath());
    }

    protected static class WrapperFactory{
        private final HttpProperties mHttpProperties;
        private final AccountOperations mAccountOperations;

        public WrapperFactory(Context context){
            this(new HttpProperties(), new AccountOperations(context));
        }
        @VisibleForTesting
        public WrapperFactory(HttpProperties httpProperties, AccountOperations accountOperations) {
            mHttpProperties = httpProperties;
            mAccountOperations = accountOperations;
        }

        public ApiWrapper createWrapper(Context context, APIRequest apiRequest){
            Wrapper wrapper = new Wrapper(context, mHttpProperties, mAccountOperations);
            String acceptContentType = apiRequest.isPrivate() ? format(PRIVATE_API_ACCEPT_CONTENT_TYPE, apiRequest.getVersion()) : MediaType.JSON_UTF_8.toString();
            wrapper.setDefaultContentType(acceptContentType);
            return wrapper;
        }
    }

}
