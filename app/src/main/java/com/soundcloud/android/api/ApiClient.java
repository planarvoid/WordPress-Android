package com.soundcloud.android.api;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

public class ApiClient {

    static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/vnd.com.soundcloud.mobile.v%d+json; charset=utf-8";
    // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
    static final String PUBLIC_API_ACCEPT_CONTENT_TYPE = "application/json";
    static final String URI_APP_PREFIX = "/app";
    private static final String TAG = "ApiClient";

    private final FeatureFlags featureFlags;
    private final OkHttpClient httpClient;
    private final ApiUrlBuilder urlBuilder;
    private final JsonTransformer jsonTransformer;
    private final ApiWrapperFactory wrapperFactory;
    private final DeviceHelper deviceHelper;
    private final OAuth oAuth;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;

    @Inject
    public ApiClient(FeatureFlags featureFlags, OkHttpClient httpClient, ApiUrlBuilder urlBuilder,
                     JsonTransformer jsonTransformer, ApiWrapperFactory wrapperFactory, DeviceHelper deviceHelper,
                     OAuth oAuth, UnauthorisedRequestRegistry unauthorisedRequestRegistry) {
        this.featureFlags = featureFlags;
        this.httpClient = httpClient;
        this.urlBuilder = urlBuilder;
        this.jsonTransformer = jsonTransformer;
        this.wrapperFactory = wrapperFactory;
        this.deviceHelper = deviceHelper;
        this.oAuth = oAuth;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
    }

    public ApiResponse fetchResponse(ApiRequest request) {
        RequestResponseStrategy httpStrategy = featureFlags.isEnabled(Flag.OKHTTP)
                ? new OkHttpStrategy() : new ApacheHttpStrategy();
        try {
            return httpStrategy.fetchResponse(request);
        } catch (CloudAPI.InvalidTokenException e) {
            return new ApiResponse(ApiRequestException.authError(request, e));
        } catch (IOException e) {
            return new ApiResponse(ApiRequestException.networkError(request, e));
        } catch (ApiMapperException e) {
            return new ApiResponse(ApiRequestException.malformedInput(request, e));
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

    private interface RequestResponseStrategy {

         ApiResponse fetchResponse(ApiRequest request) throws IOException, ApiMapperException;

    }

    private final class OkHttpStrategy implements RequestResponseStrategy {

        @Override
        public ApiResponse fetchResponse(ApiRequest request) throws IOException, ApiMapperException {
            final com.squareup.okhttp.Request.Builder builder = new com.squareup.okhttp.Request.Builder();

            builder.url(resolveFullUrl(request));
            setHttpHeaders(request, builder);

            switch (HttpMethod.valueOf(request.getMethod())) {
                case GET:
                    builder.get();
                    break;
                case POST:
                    builder.post(getRequestBody(request));
                    break;
                case PUT:
                    builder.put(getRequestBody(request));
                    break;
                case DELETE:
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

        private String resolveFullUrl(ApiRequest request) {
            final Map<String, String> existingParams = transformQueryParameters(request);
            final ApiUrlBuilder builder = urlBuilder.from(request).withQueryParams(existingParams);
            if (!existingParams.containsKey(OAuth.PARAM_CLIENT_ID)) {
                builder.withQueryParam(OAuth.PARAM_CLIENT_ID, oAuth.getClientId());
            }
            return builder.build();
        }
    }

    private final class ApacheHttpStrategy implements RequestResponseStrategy {

        @Override
        public ApiResponse fetchResponse(ApiRequest request) throws IOException, ApiMapperException {
            ApiWrapper apiWrapper = wrapperFactory.createWrapper(request);
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().toUpperCase(Locale.US));
            Log.d(TAG, "executing request: " + request);
            HttpResponse response = httpMethod.execute(apiWrapper, adaptRequest(request));
            String responseBody = EntityUtils.toString(response.getEntity(), Charsets.UTF_8.name());
            return new ApiResponse(request, response.getStatusLine().getStatusCode(), responseBody);
        }

        // we currently need to route all external requests through java-api-wrapper, until we've moved to OkHttp,
        // so we need to adapt it to its own request format
        private Request adaptRequest(ApiRequest<?> apiRequest) throws ApiMapperException {
            final boolean needsPrefix = apiRequest.isPrivate() && !apiRequest.getEncodedPath().startsWith(URI_APP_PREFIX);
            String baseUriPath = needsPrefix ? urlBuilder.getHttpProperties().getApiMobileBaseUriPath() : ScTextUtils.EMPTY_STRING;
            final String requestUrl = baseUriPath + apiRequest.getEncodedPath();
            Request request = Request.to(requestUrl);

            Map<String, String> transformedParameters = transformQueryParameters(apiRequest);

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
