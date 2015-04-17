package com.soundcloud.android.api;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.ApiFileContentRequest.FileEntry;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
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

    private boolean assertBackgroundThread;

    @Inject
    public ApiClient(OkHttpClient httpClient, ApiUrlBuilder urlBuilder,
                     JsonTransformer jsonTransformer, DeviceHelper deviceHelper, AdIdHelper adIdHelper,
                     OAuth oAuth, UnauthorisedRequestRegistry unauthorisedRequestRegistry) {
        this.httpClient = httpClient;
        this.urlBuilder = urlBuilder;
        this.jsonTransformer = jsonTransformer;
        this.deviceHelper = deviceHelper;
        this.adIdHelper = adIdHelper;
        this.oAuth = oAuth;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
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

            final Map<String, String> existingParams = transformQueryParameters(request);
            builder.url(urlBuilder.from(request).withQueryParams(existingParams).build());
            setHttpHeaders(request, builder);

            switch (request.getMethod()) {
                case "GET":
                    builder.get();
                    break;
                case "POST":
                    builder.post(getRequestBody(request));
                    break;
                case "PUT":
                    builder.put(getRequestBody(request));
                    break;
                case "DELETE":
                    builder.delete();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + request.getMethod());
            }
            final com.squareup.okhttp.Request httpRequest = builder.build();
            logRequest(httpRequest);
            final Response response = httpClient.newCall(httpRequest).execute();
            if (response.code() == HttpStatus.SC_UNAUTHORIZED) {
                unauthorisedRequestRegistry.updateObservedUnauthorisedRequestTimestamp();
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
        builder.header(HttpHeaders.AUTHORIZATION, oAuth.getAuthorizationHeaderValue());

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
        } else if (request instanceof ApiFileContentRequest) {
            return getMultipartRequestBody((ApiFileContentRequest) request);
        } else {
            return RequestBody.create(MediaType.parse(request.getAcceptMediaType()), ScTextUtils.EMPTY_STRING);
        }
    }

    private RequestBody getJsonRequestBody(ApiObjectContentRequest request) throws UnsupportedEncodingException, ApiMapperException {
        final MediaType mediaType = MediaType.parse(request.getContentType());
        final byte[] content = jsonTransformer.toJson(request.getContent()).getBytes(Charsets.UTF_8.name());
        return RequestBody.create(mediaType, content);
    }

    private RequestBody getMultipartRequestBody(ApiFileContentRequest request) {
        final MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM);
        final List<FileEntry> files = request.getFiles();
        for (FileEntry fileEntry : files) {
            final RequestBody fileBody = RequestBody.create(MediaType.parse(fileEntry.contentType), fileEntry.file);
            builder.addFormDataPart(fileEntry.paramName, fileEntry.fileName, fileBody);
        }
        return builder.build();
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

    private Map<String, String> transformQueryParameters(ApiRequest apiRequest) {
        final Multimap<String, String> queryParameters = apiRequest.getQueryParameters();
        return Maps.toMap(queryParameters.keySet(), new Function<String, String>() {
            @Override
            public String apply(String input) {
                return Joiner.on(",").join(queryParameters.get(input));
            }
        });
    }
}
