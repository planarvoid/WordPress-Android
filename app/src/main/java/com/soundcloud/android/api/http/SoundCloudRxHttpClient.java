package com.soundcloud.android.api.http;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.soundcloud.android.api.http.SoundCloudRxHttpClient.APIErrorReason.BAD_RESPONSE;
import static com.soundcloud.android.api.http.SoundCloudRxHttpClient.APIErrorReason.NETWORK_COMM_ERROR;
import static com.soundcloud.android.api.http.SoundCloudRxHttpClient.APIErrorReason.TOKEN_AUTH_ERROR;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SoundCloudRxHttpClient implements RxHttpClient {


    private static final Function<Header, String> HEADER_KEY_FUNCTION = new Function<Header, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Header input) {
            return input == null ? null : input.getName();
        }
    };
    private static final Function<Header, String> HEADER_VALUE_FUNCTION = new Function<Header, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Header input) {
            return input == null ? null : input.getValue();
        }
    };
    private AccountOperations mAccountOpertations;
    private HttpProperties mHttpProperties;
    private JsonTransformer mJsonTransformer;

    public SoundCloudRxHttpClient(Context context) {
        this(new AccountOperations(context), new HttpProperties(context.getResources()), new JacksonJsonTransformer());

    }

    @VisibleForTesting
    protected SoundCloudRxHttpClient(AccountOperations accountOperations, HttpProperties httpProperties,
                                     JsonTransformer jsonTransformer) {
        mAccountOpertations = accountOperations;
        mHttpProperties = httpProperties;
        mJsonTransformer = jsonTransformer;
    }


    @Override
    public <ModelType> Observable<ModelType> getResources(final APIRequest apiRequest) {
        return Observable.create(new Func1<Observer<ModelType>, Subscription>() {
            /*
            TODO Version headers, gzip acceptance
             */
            @Override
            public Subscription call(Observer<ModelType> observer) {
                APIResponse apiResponse = executeRequest(apiRequest);

                if (apiResponse.accountIsRateLimited()) {
                    //TODO Add logic to check if response has been rate limited and respond, maybe EventRegistry based on RX
                    apiRequest.toString();
                } else if (apiResponse.isNotSuccess()) {
                    throw APIRequestException.badResponse();
                }
                Object resource = parseJsonResponse(apiResponse, apiRequest);

                notifyObserverOfResult(observer, apiRequest, resource);

                return Subscriptions.empty();

            }

        });
    }

    private <T> void notifyObserverOfResult(Observer<T> observer, APIRequest apiRequest, Object resource) {
        boolean isCollection = apiRequest.getResourceType().isAssignableFrom(Collection.class.getGenericSuperclass());
        @SuppressWarnings("unchecked")
        Collection<T> resources = isCollection ? Collection.class.cast(resource) : Collections.singleton(resource);
        for(T modelType : resources){
            observer.onNext(modelType);
        }
    }

    private Object parseJsonResponse(APIResponse apiResponse, APIRequest resourceType) {
        Object resource;
        try {
            resource = mJsonTransformer.fromJson(apiResponse.getResponseBody(), resourceType.getResourceType());
        } catch (Exception e) {
            throw APIRequestException.badResponse(e);
        }

        if(UnknownResource.class.isAssignableFrom(resource.getClass())){
            throw APIRequestException.badResponse("Response could not be deserialised");
        }
        return resource;
    }

    private APIResponse executeRequest(APIRequest apiRequest) {

        ApiWrapper apiWrapper = createApiWrapper();
        try {


            HttpResponse response = apiWrapper.get(createSCRequest(apiRequest));
            String responseBody = EntityUtils.toString(response.getEntity(), Charsets.UTF_8.name());
            return new APIResponse(response.getStatusLine().getStatusCode(),
                    responseBody, createHeadersMap(response.getAllHeaders()));


        } catch(CloudAPI.InvalidTokenException e){
            throw APIRequestException.authError(e);
        } catch (IOException e) {
            throw APIRequestException.networkCommsError(e);
        }
    }

    private Map<String, String> createHeadersMap(Header[] allHeaders) {
        return Maps.transformValues(uniqueIndex(newArrayList(allHeaders), HEADER_KEY_FUNCTION), HEADER_VALUE_FUNCTION);
    }

    private ApiWrapper createApiWrapper() {
        return new ApiWrapper(mHttpProperties.getClientId(), mHttpProperties.getClientSecret(),
                AndroidCloudAPI.ANDROID_REDIRECT_URI, mAccountOpertations.getSoundCloudToken());
    }

    private Request createSCRequest(APIRequest<?> apiRequest) {
        Request request = Request.to(apiRequest.getUriPath());
        for (Map.Entry<String, String> parameter : apiRequest.getQueryParameters().entrySet()) {
            request.add(parameter.getKey(), parameter.getValue());
        }
        return request;

    }

    public enum APIErrorReason {
        TOKEN_AUTH_ERROR,
        NETWORK_COMM_ERROR,
        BAD_RESPONSE,
        UNKNOWN_ERROR
    }


    public static class APIRequestException extends RuntimeException {
        public static APIRequestException badResponse() {
            return new APIRequestException(BAD_RESPONSE);
        }

        public static APIRequestException badResponse(Exception exception) {
            return new APIRequestException(BAD_RESPONSE, exception);
        }

        public static APIRequestException badResponse(String msg) {
            return new APIRequestException(BAD_RESPONSE, msg);
        }

        public static APIRequestException networkCommsError(IOException ioException) {
            return new APIRequestException(NETWORK_COMM_ERROR, ioException);
        }


        public static APIRequestException authError(CloudAPI.InvalidTokenException e) {
            return new APIRequestException(TOKEN_AUTH_ERROR, e);
        }

        private APIRequestException(APIErrorReason errorReason, Exception e) {
            super(e);
            this.errorReason = errorReason;
        }

        private APIRequestException(APIErrorReason errorReason, String msg) {
            super(msg);
            this.errorReason = errorReason;
        }


        private APIRequestException(APIErrorReason errorReason) {
            this.errorReason = errorReason;
        }

        private APIErrorReason errorReason;

        public APIErrorReason reason() {
            return errorReason;
        }

    }


}
