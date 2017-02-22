package com.soundcloud.android.api;

import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.strings.Strings;

import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class ApiUrlBuilder {

    private final String mobileApiBaseUrl;
    private final String publicApiBaseUrl;
    private final OAuth oAuth;
    private Uri.Builder uriBuilder;

    @Inject
    public ApiUrlBuilder(@Named(ApiModule.PUBLIC_API_BASE_URL) String publicApiBaseUrl,
                         @Named(ApiModule.MOBILE_API_BASE_URL) String mobileApiBaseUrl,
                         OAuth oAuth) {
        this.oAuth = oAuth;
        this.mobileApiBaseUrl = mobileApiBaseUrl;
        this.publicApiBaseUrl = publicApiBaseUrl;
    }

    public ApiUrlBuilder from(ApiEndpoints endpoint, Object... pathParams) {
        uriBuilder = Uri.parse(mobileApiBaseUrl + endpoint.unencodedPath(pathParams)).buildUpon();
        return withOAuthClientIdParam();
    }

    public ApiUrlBuilder from(ApiRequest request) {
        if (request.getUri().isAbsolute()) {
            // if the URI is aleady absolute, i.e. contains a schema and host, we take it as is
            uriBuilder = request.getUri().buildUpon();
        } else {
            // we expand the relative URI to contain the proper scheme and API host
            final String baseUri = request.isPrivate()
                                   ? mobileApiBaseUrl : publicApiBaseUrl;
            uriBuilder = Uri.parse(baseUri + request.getUri()).buildUpon();
        }
        return withOAuthClientIdParam();
    }

    private ApiUrlBuilder withOAuthClientIdParam() {
        return withQueryParam(OAuth.PARAM_CLIENT_ID, oAuth.getClientId());
    }

    public ApiUrlBuilder withQueryParams(MultiMap<String, String> params) {
        for (String key : params.keySet()) {
            withQueryParam(key, Strings.joinOn(',').join(params.get(key)));
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

    public Uri.Builder builder() {
        return uriBuilder;
    }

    public String build() {
        return uriBuilder.toString();
    }
}
