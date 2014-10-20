package com.soundcloud.android.api;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.UnknownResource;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class ApiClient {

    static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/vnd.com.soundcloud.mobile.v%d+json";
    // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
    static final String PUBLIC_API_ACCEPT_CONTENT_TYPE = "application/json";
    static final String URI_APP_PREFIX = "/app";

    private final HttpProperties httpProperties;
    private final JsonTransformer jsonTransformer;
    private final ApiWrapperFactory wrapperFactory;

    @Inject
    public ApiClient(HttpProperties httpProperties, JsonTransformer jsonTransformer, ApiWrapperFactory wrapperFactory) {
        this.httpProperties = httpProperties;
        this.jsonTransformer = jsonTransformer;
        this.wrapperFactory = wrapperFactory;
    }

    public ApiResponse fetchResponse(ApiRequest request) {
        ApiWrapper apiWrapper = wrapperFactory.createWrapper(request);
        try {
            HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().toUpperCase(Locale.US));
            Log.d(this, "executing request: " + request);
            HttpResponse response = httpMethod.execute(apiWrapper, adaptRequest(request));
            String responseBody = EntityUtils.toString(response.getEntity(), Charsets.UTF_8.name());
            return new ApiResponse(request, response.getStatusLine().getStatusCode(), responseBody);
        } catch (CloudAPI.InvalidTokenException e) {
            return new ApiResponse(ApiRequestException.authError(request, e));
        } catch (IOException e) {
            return new ApiResponse(ApiRequestException.networkError(request, e));
        } catch (ApiMapperException e) {
            return new ApiResponse(ApiRequestException.malformedInput(request, e));
        }
    }

    public <ResourceType> ResourceType fetchMappedResponse(ApiRequest<ResourceType> request) throws ApiMapperException {
        return mapResponse(request, fetchResponse(request));
    }

    // we currently need to route all external requests through java-api-wrapper, until we've moved to OkHttp,
    // so we need to adapt it to its own request format
    private Request adaptRequest(ApiRequest<?> apiRequest) throws ApiMapperException {
        final boolean needsPrefix = apiRequest.isPrivate() && !apiRequest.getEncodedPath().startsWith(URI_APP_PREFIX);
        String baseUriPath = needsPrefix ? httpProperties.getApiMobileBaseUriPath() : ScTextUtils.EMPTY_STRING;
        Request request = Request.to(baseUriPath + apiRequest.getEncodedPath());

        final Multimap<String, String> queryParameters = apiRequest.getQueryParameters();
        Map<String, String> transformedParameters = Maps.toMap(queryParameters.keySet(), new Function<String, String>() {
            @Override
            public String apply(String input) {
                return Joiner.on(",").join(queryParameters.get(input));
            }
        });

        for (String key : transformedParameters.keySet()) {
            request.add(key, transformedParameters.get(key));
        }

        request.setHeaders(apiRequest.getHeaders());

        final Object content = apiRequest.getContent();
        if (content != null) {
            request.withContent(jsonTransformer.toJson(content), ApiClient.PUBLIC_API_ACCEPT_CONTENT_TYPE);
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    <T> T mapResponse(final ApiRequest<T> apiRequest, final ApiResponse apiResponse) throws ApiMapperException {
        if (apiResponse.isSuccess() && apiResponse.hasResponseBody()) {
            return (T) parseJsonResponse(apiResponse, apiRequest);
        } else {
            throw new ApiMapperException("Empty response body or request failed");
        }
    }

    private Object parseJsonResponse(ApiResponse apiResponse, ApiRequest apiRequest) throws ApiMapperException {
        Object resource = jsonTransformer.fromJson(apiResponse.getResponseBody(), apiRequest.getResourceType());

        if (resource == null || UnknownResource.class.isAssignableFrom(resource.getClass())) {
            throw new ApiMapperException("Response could not be deserialized, or types do not match");
        }

        return resource;
    }
}
