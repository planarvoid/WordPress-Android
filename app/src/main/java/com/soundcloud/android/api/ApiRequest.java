package com.soundcloud.android.api;


import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.java.objects.MoreObjects;
import org.apache.http.auth.AUTH;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApiRequest {

    static final String HTTP_GET = "GET";
    static final String HTTP_POST = "POST";
    static final String HTTP_PUT = "PUT";
    static final String HTTP_DELETE = "DELETE";

    private static final String PRIVATE_API_ACCEPT_CONTENT_TYPE = "application/vnd.com.soundcloud.mobile.v%d+json; charset=utf-8";
    // do not use MediaType.JSON_UTF8; the public API does not accept qualified media types that include charsets
    private static final String PUBLIC_API_ACCEPT_CONTENT_TYPE = "application/json";

    private final Uri uri;
    private final String httpMethod;
    private final int endpointVersion;
    private final Boolean isPrivate;
    private final Multimap<String, String> queryParams;
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

    ApiRequest(Uri uri, String method, int endpointVersion,
               Boolean isPrivate, Multimap<String, String> queryParams,
               Map<String, String> headers) {
        this.uri = uri;
        this.httpMethod = method;
        this.endpointVersion = endpointVersion;
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

    public int getVersion() {
        return endpointVersion;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @NotNull
    public Multimap<String, String> getQueryParameters() {
        return ImmutableMultimap.copyOf(queryParams);
    }

    @NotNull
    public Map<String, String> getHeaders() {
        return ImmutableMap.copyOf(headers);
    }

    public String getAcceptMediaType() {
        return isPrivate
                ? String.format(Locale.US, PRIVATE_API_ACCEPT_CONTENT_TYPE, endpointVersion)
                : PUBLIC_API_ACCEPT_CONTENT_TYPE;
    }

    public enum Param {
        PAGE_SIZE("limit"),
        OAUTH_TOKEN("oauth_token");

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
        private int endpointVersion;
        private Boolean isPrivate;
        private final Multimap<String, String> parameters;
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
            if (isPrivate) {
                checkArgument(endpointVersion > 0, "Not a valid api version: " + endpointVersion);
            }
            if (content != null) {
                return new ApiObjectContentRequest(uri, httpMethod, endpointVersion, isPrivate, parameters, headers, content);
            } else if (formParts != null) {
                return new ApiMultipartRequest(uri, httpMethod, endpointVersion, isPrivate, parameters, headers,
                        formParts, progressListener);
            } else {
                return new ApiRequest(uri, httpMethod, endpointVersion, isPrivate, parameters, headers);
            }
        }

        public Builder forPrivateApi(int version) {
            isPrivate = true;
            endpointVersion = version;
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

        public Builder addQueryParam(Param param, Object... values) {
            return addQueryParam(param.parameter, values);
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

        public Builder withToken(Token token) {
            withHeader(AUTH.WWW_AUTH_RESP, OAuth.createOAuthHeaderValue(token));
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
                .add("uri", uri.toString())
                .add("httpMethod", httpMethod)
                .add("endPointVersion", endpointVersion)
                .add("isPrivate", isPrivate).toString();
    }
}
