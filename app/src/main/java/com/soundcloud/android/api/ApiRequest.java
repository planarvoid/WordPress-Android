package com.soundcloud.android.api;


import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.net.HttpHeaders;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiRequest {

    static final String HTTP_GET = "GET";
    static final String HTTP_POST = "POST";
    static final String HTTP_PUT = "PUT";
    static final String HTTP_DELETE = "DELETE";

    private static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/json; charset=utf-8";
    // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
    private static final String PUBLIC_API_ACCEPT_CONTENT_TYPE = "application/json";

    private final Uri uri;
    private final String httpMethod;
    private final Boolean isPrivate;
    private final MultiMap<String, String> queryParams;
    private final Map<String, String> headers;

    public static Builder get(String uri) {
        return new Builder(uri, HTTP_GET);
    }

    public static Builder post(String uri) {
        return new Builder(uri, HTTP_POST);
    }

    public static Builder put(String uri) {
        return new Builder(uri, HTTP_PUT);
    }

    public static Builder delete(String uri) {
        return new Builder(uri, HTTP_DELETE);
    }

    ApiRequest(Uri uri, String method,
               Boolean isPrivate, MultiMap<String, String> queryParams,
               Map<String, String> headers) {
        this.uri = uri;
        this.httpMethod = method;
        this.isPrivate = isPrivate;
        this.queryParams = queryParams;
        this.headers = headers;
    }

    public Uri getUri() {
        return uri;
    }

    public String getEncodedPath() {
        return uri.getEncodedPath();
    }

    public String getMethod() {
        return httpMethod;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @NotNull
    public MultiMap<String, String> getQueryParameters() {
        return queryParams;
    }

    @NotNull
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getAcceptMediaType() {
        return isPrivate
               ? PRIVATE_API_ACCEPT_CONTENT_TYPE
               : PUBLIC_API_ACCEPT_CONTENT_TYPE;
    }

    public enum Param {
        PAGE_SIZE("limit"),
        OAUTH_TOKEN("oauth_token"),
        LOCALE("locale");

        private final String parameter;

        Param(String parameter) {
            this.parameter = parameter;
        }

        @Override
        public String toString() {
            return parameter;
        }
    }

    public interface ProgressListener {
        void update(long bytesWritten, long totalBytes) throws IOException;
    }

    public static class Builder {
        private final Uri uri;
        private final String httpMethod;
        private Boolean isPrivate;
        private final MultiMap<String, String> parameters;
        private final Map<String, String> headers;
        private Object content;
        private List<FormPart> formParts;
        private ProgressListener progressListener;

        public Builder(String uri, String methodName) {
            this.parameters = UriUtils.getQueryParameters(uri);
            this.uri = UriUtils.clearQueryParams(Uri.parse(uri));
            this.httpMethod = methodName;
            this.headers = new HashMap<>();
        }

        public ApiRequest build() {
            checkNotNull(isPrivate, "Must specify api mode");
            if (content != null) {
                return new ApiObjectContentRequest(uri, httpMethod, isPrivate, parameters, headers, content);
            } else if (formParts != null) {
                return new ApiMultipartRequest(uri, httpMethod, isPrivate, parameters, headers,
                                               formParts, progressListener);
            } else {
                return new ApiRequest(uri, httpMethod, isPrivate, parameters, headers);
            }
        }

        public Builder forPrivateApi() {
            isPrivate = true;
            return this;
        }

        public Builder forPublicApi() {
            isPrivate = false;
            return this;
        }

        public Builder addQueryParam(String key, Object... values) {
            for (Object object : values) {
                parameters.put(key, object.toString());
            }
            return this;
        }

        public Builder addQueryParamIfAbsent(Param param, Object... values) {
            if (parameters.get(param.parameter).size() == 0) {
                return addQueryParam(param.parameter, values);
            }
            return this;
        }

        public Builder withContent(Object content) {
            this.content = content;
            return this;
        }

        public Builder withFormPart(FormPart formPart) {
            if (this.formParts == null) {
                formParts = new ArrayList<>();
            }
            formParts.add(formPart);
            return this;
        }

        public Builder withHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder withProgressListener(ProgressListener progressListener) {
            this.progressListener = progressListener;
            return this;
        }

        public Builder withFormMap(Map<String, String> params) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                withFormPart(StringPart.from(entry.getKey(), entry.getValue()));
            }
            return this;
        }

        public Builder withToken(FeatureFlags featureFlags, Token token) {
            withHeader(HttpHeaders.AUTHORIZATION, OAuth.createOAuthHeaderValue(featureFlags, token));
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
                          .add("uri", uri.toString())
                          .add("httpMethod", httpMethod)
                          .add("isPrivate", isPrivate).toString();
    }
}
