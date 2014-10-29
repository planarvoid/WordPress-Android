package com.soundcloud.android.api;

import android.net.Uri;

import javax.inject.Inject;
import java.util.Map;

public class ApiUrlBuilder {

    private final HttpProperties httpProperties;
    private Uri.Builder uriBuilder;
    private boolean forceHttp;

    @Inject
    public ApiUrlBuilder(HttpProperties httpProperties) {
        this.httpProperties = httpProperties;
    }

    public ApiUrlBuilder from(ApiEndpoints endpoint) {
        uriBuilder = Uri.parse(httpProperties.getMobileApiBaseUrl() + endpoint.path()).buildUpon();
        return this;
    }

    public ApiUrlBuilder from(ApiEndpoints endpoint, Object... pathParams) {
        uriBuilder = Uri.parse(httpProperties.getMobileApiBaseUrl() + endpoint.unencodedPath(pathParams)).buildUpon();
        return this;
    }

    public ApiUrlBuilder from(ApiRequest<?> request) {
        if (request.getUri().isAbsolute()) {
            // if the URI is aleady absolute, i.e. contains a schema and host, we take it as is
            uriBuilder = request.getUri().buildUpon();
        } else {
            // we expand the relative URI to contain the proper scheme and API host
            final String baseUri = request.isPrivate()
                    ? httpProperties.getMobileApiBaseUrl() : httpProperties.getPublicApiBaseUrl();
            uriBuilder = Uri.parse(baseUri + request.getUri()).buildUpon();
        }
        return this;
    }

    public ApiUrlBuilder withQueryParams(Map<String, ?> params) {
        for (Map.Entry<String, ?> param : params.entrySet()) {
            withQueryParam(param.getKey(), param.getValue());
        }
        return this;
    }

    public ApiUrlBuilder withQueryParam(String param, Object value) {
        uriBuilder.appendQueryParameter(param, value.toString());
        return this;
    }

    public ApiUrlBuilder withQueryParam(ApiRequest.Param param, Object value) {
        uriBuilder.appendQueryParameter(param.toString(), value.toString());
        return this;
    }

    public ApiUrlBuilder forceHttp() {
        this.forceHttp = true;
        return this;
    }

    public Uri.Builder builder() {
        return uriBuilder;
    }

    public String build() {
        return forceHttp ? uriBuilder.toString().replaceFirst("https", "http") : uriBuilder.toString();
    }

    @Deprecated // only exists for Apache client legacy code
    HttpProperties getHttpProperties() {
        return httpProperties;
    }
}
