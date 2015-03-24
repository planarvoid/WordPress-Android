package com.soundcloud.android.api;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.apache.http.HttpStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

public class ApiClient {

    private static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/vnd.com.soundcloud.mobile.v%d+json; charset=utf-8";
    // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
    private static final String PUBLIC_API_ACCEPT_CONTENT_TYPE = "application/json";
    private static final String TAG = "ApiClient";

    private final OkHttpClient httpClient;
    private final ApiUrlBuilder urlBuilder;
    private final JsonTransformer jsonTransformer;
    private final DeviceHelper deviceHelper;
    private final AdIdHelper adIdHelper;
    private final OAuth oAuth;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;

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

    public ApiResponse fetchResponse(ApiRequest request) {
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
        Log.d(TAG, "[OkHttp] " + request.method() + " " + request.urlString() + "; headers = " + request.headers());
    }

    private void logResponse(Response response) {
        Log.d(TAG, "[OkHttp] " + response);
    }

    private void setHttpHeaders(ApiRequest request, com.squareup.okhttp.Request.Builder builder) {
        // default headers
        builder.header(HttpHeaders.ACCEPT, getContentType(request));
        builder.header(HttpHeaders.USER_AGENT, deviceHelper.getUserAgent());
        builder.header(HttpHeaders.AUTHORIZATION, oAuth.getAuthorizationHeaderValue());

        // user identifiers
        final String udid = deviceHelper.getUDID();
        if (ScTextUtils.isNotBlank(udid)) {
            builder.header(ApiHeaders.UDID, udid);
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

    private String getContentType(ApiRequest request) {
        return request.isPrivate()
                ? String.format(Locale.US, PRIVATE_API_ACCEPT_CONTENT_TYPE, request.getVersion())
                : PUBLIC_API_ACCEPT_CONTENT_TYPE;
    }

    private RequestBody getRequestBody(ApiRequest request) throws ApiMapperException, UnsupportedEncodingException {
        final MediaType mediaType = MediaType.parse(getContentType(request));
        if (request.getContent() != null) {
            final byte[] content = jsonTransformer.toJson(request.getContent()).getBytes(Charsets.UTF_8.name());
            return RequestBody.create(mediaType, content);
        } else {
            return RequestBody.create(mediaType, ScTextUtils.EMPTY_STRING);
        }
    }

    public <ResourceType> ResourceType fetchMappedResponse(ApiRequest<ResourceType> request) throws IOException, ApiRequestException, ApiMapperException {
        return mapResponse(request, fetchResponse(request));
    }

    @SuppressWarnings("unchecked")
    <T> T mapResponse(final ApiRequest<T> apiRequest, final ApiResponse apiResponse) throws IOException, ApiRequestException, ApiMapperException {
        if (apiResponse.isSuccess()) {
            if (apiResponse.hasResponseBody()) {
                return (T) parseJsonResponse(apiResponse, apiRequest);
            } else {
                throw new ApiMapperException("Empty response body");
            }
        } else {
            throw apiResponse.getFailure();
        }
    }

    private Object parseJsonResponse(ApiResponse apiResponse, ApiRequest apiRequest) throws IOException, ApiMapperException {
        Object resource = jsonTransformer.fromJson(apiResponse.getResponseBody(), apiRequest.getResourceType());

        if (resource == null || UnknownResource.class.isAssignableFrom(resource.getClass())) {
            throw new ApiMapperException("Response could not be deserialized, or types do not match");
        }

        return resource;
    }

    private Map<String, String> transformQueryParameters(ApiRequest<?> apiRequest) {
        final Multimap<String, String> queryParameters = apiRequest.getQueryParameters();
        return Maps.toMap(queryParameters.keySet(), new Function<String, String>() {
            @Override
            public String apply(String input) {
                return Joiner.on(",").join(queryParameters.get(input));
            }
        });
    }
}
