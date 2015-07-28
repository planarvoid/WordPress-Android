package com.soundcloud.android.api;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.reflect.TypeToken;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.apache.http.HttpStatus;

import android.os.Looper;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class ApiClient {

    private static final String TAG = "ApiClient";

    private final OkHttpClient httpClient;
    private final ApiUrlBuilder urlBuilder;
    private final JsonTransformer jsonTransformer;
    private final DeviceHelper deviceHelper;
    private final AdIdHelper adIdHelper;
    private final OAuth oAuth;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    private final AccountOperations accountOperations;

    private boolean assertBackgroundThread;

    @Inject
    public ApiClient(OkHttpClient httpClient, ApiUrlBuilder urlBuilder,
                     JsonTransformer jsonTransformer, DeviceHelper deviceHelper, AdIdHelper adIdHelper,
                     OAuth oAuth, UnauthorisedRequestRegistry unauthorisedRequestRegistry, AccountOperations accountOperations) {
        this.httpClient = httpClient;
        this.urlBuilder = urlBuilder;
        this.jsonTransformer = jsonTransformer;
        this.deviceHelper = deviceHelper;
        this.adIdHelper = adIdHelper;
        this.oAuth = oAuth;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
        this.accountOperations = accountOperations;
    }

    public void setAssertBackgroundThread(boolean assertBackgroundThread) {
        this.assertBackgroundThread = assertBackgroundThread;
    }

    public ApiResponse fetchResponse(ApiRequest request) {
        if (assertBackgroundThread) {
            checkState(Thread.currentThread() != Looper.getMainLooper().getThread(),
                    "Detected execution of API request on main thread");
        }
        try {
            final com.squareup.okhttp.Request.Builder builder = new com.squareup.okhttp.Request.Builder();

            final MultiMap<String, String> existingParams = request.getQueryParameters();
            builder.url(urlBuilder.from(request).withQueryParams(existingParams).build());
            setHttpHeaders(request, builder);

            switch (request.getMethod()) {
                case ApiRequest.HTTP_GET:
                    builder.get();
                    break;
                case ApiRequest.HTTP_POST:
                    builder.post(getRequestBody(request));
                    break;
                case ApiRequest.HTTP_PUT:
                    builder.put(getRequestBody(request));
                    break;
                case ApiRequest.HTTP_DELETE:
                    builder.delete();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + request.getMethod());
            }
            final Request httpRequest = builder.build();
            logRequest(httpRequest);
            final Response response = httpClient.newCall(httpRequest).execute();
            if (response.code() == HttpStatus.SC_UNAUTHORIZED) {
                if (accountOperations.hasValidToken()) {
                    unauthorisedRequestRegistry.updateObservedUnauthorisedRequestTimestamp();
                }
            }
            logResponse(response);
            return new ApiResponse(request, response.code(), response.body().string());
        } catch (IOException e) {
            return new ApiResponse(ApiRequestException.networkError(request, e));
        } catch (ApiMapperException e) {
            return new ApiResponse(ApiRequestException.malformedInput(request, e));
        }
    }

    private void logRequest(com.squareup.okhttp.Request request) {
        Log.d(TAG, "[OkHttp][" + Thread.currentThread().getName() + "] " + request.method() + " "
                + request.urlString() + "; headers = " + request.headers());
    }

    private void logResponse(Response response) {
        Log.d(TAG, "[OkHttp] " + response);
    }

    private void setHttpHeaders(ApiRequest request, com.squareup.okhttp.Request.Builder builder) {
        // default headers
        builder.header(HttpHeaders.ACCEPT, request.getAcceptMediaType());
        builder.header(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());
        builder.header(ApiHeaders.APP_VERSION, String.valueOf(deviceHelper.getAppVersionCode()));

        if (accountOperations.getSoundCloudToken().valid()) {
            builder.header(HttpHeaders.AUTHORIZATION, oAuth.getAuthorizationHeaderValue());
        }

        // user identifiers
        if (deviceHelper.hasUdid()) {
            builder.header(ApiHeaders.UDID, deviceHelper.getUdid());
        }
        if (adIdHelper.isAvailable()) {
            builder.header(ApiHeaders.ADID, adIdHelper.getAdId());
            builder.header(ApiHeaders.ADID_TRACKING, String.valueOf(adIdHelper.getAdIdTracking()));
        }

        // transfer other HTTP headers
        final Map<String, String> headers = request.getHeaders();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
    }

    private RequestBody getRequestBody(ApiRequest request) throws ApiMapperException, UnsupportedEncodingException {
        if (request instanceof ApiObjectContentRequest) {
            return getJsonRequestBody((ApiObjectContentRequest) request);
        } else if (request instanceof ApiMultipartRequest) {
            return getMultipartRequestBody((ApiMultipartRequest) request);
        } else {
            return RequestBody.create(MediaType.parse(request.getAcceptMediaType()), ScTextUtils.EMPTY_STRING);
        }
    }

    private RequestBody getJsonRequestBody(ApiObjectContentRequest request) throws UnsupportedEncodingException, ApiMapperException {
        final MediaType mediaType = MediaType.parse(request.getContentType());
        final byte[] content = jsonTransformer.toJson(request.getContent()).getBytes(Charsets.UTF_8.name());
        return RequestBody.create(mediaType, content);
    }

    private RequestBody getMultipartRequestBody(ApiMultipartRequest request) {
        final MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM);
        final List<FormPart> parts = request.getParts();
        for (FormPart part : parts) {
            if (part instanceof StringPart) {
                String value = ((StringPart) part).getValue();
                builder.addFormDataPart(part.getPartName(), value);
            } else if (part instanceof FilePart) {
                final FilePart filePart = (FilePart) part;
                final RequestBody requestBody = RequestBody.create(MediaType.parse(part.getContentType()), filePart.getFile());
                builder.addFormDataPart(filePart.getPartName(), filePart.getFileName(), requestBody);
            }
        }
        RequestBody multipartBody = builder.build();
        if (request.hasProgressListener()) {
            multipartBody = new ProgressRequestBody(multipartBody, request.getProgressListener());
        }
        return multipartBody;
    }

    public <ResourceType> ResourceType fetchMappedResponse(ApiRequest request, TypeToken<ResourceType> resourceType)
            throws IOException, ApiRequestException, ApiMapperException {
        return mapResponse(fetchResponse(request), resourceType);
    }

    public <ResourceType> ResourceType fetchMappedResponse(ApiRequest request, Class<ResourceType> resourceType)
            throws IOException, ApiRequestException, ApiMapperException {
        return fetchMappedResponse(request, TypeToken.of(resourceType));
    }

    @SuppressWarnings("unchecked")
    <T> T mapResponse(ApiResponse apiResponse, TypeToken<T> typeToken)
            throws IOException, ApiRequestException, ApiMapperException {
        if (apiResponse.isSuccess()) {
            if (apiResponse.hasResponseBody()) {
                return parseJsonResponse(apiResponse, typeToken);
            } else {
                throw new ApiMapperException("Empty response body");
            }
        } else {
            throw apiResponse.getFailure();
        }
    }

    private <T> T parseJsonResponse(ApiResponse apiResponse, TypeToken<T> typeToken) throws IOException, ApiMapperException {
        final T resource = jsonTransformer.fromJson(apiResponse.getResponseBody(), typeToken);

        if (resource == null || UnknownResource.class.isAssignableFrom(resource.getClass())) {
            throw new ApiMapperException("Response could not be deserialized, or types do not match");
        }

        return resource;
    }

}
