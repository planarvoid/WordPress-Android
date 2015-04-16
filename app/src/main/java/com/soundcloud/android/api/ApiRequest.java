package com.soundcloud.android.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public class ApiRequest {

    private final Uri uri;
    private final String httpMethod;
    private final int endpointVersion;
    private final Boolean isPrivate;
    @NotNull private final Multimap<String, String> queryParams;
    @NotNull private final Map<String, String> headers;
    @Nullable private final Object content;

    ApiRequest(Uri uri, String method, int endpointVersion,
               Boolean isPrivate, @NotNull Multimap<String, String> queryParams, @Nullable Object content,
               @NotNull Map<String, String> headers) {
        this.uri = uri;
        this.httpMethod = method;
        this.endpointVersion = endpointVersion;
        this.isPrivate = isPrivate;
        this.queryParams = queryParams;
        this.content = content;
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

    @Nullable
    public Object getContent() {
        return content;
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

    public static class Builder {
        private final Uri uri;
        private final String httpMethod;
        private int endpointVersion;
        private Boolean isPrivate;
        private final Multimap<String, String> parameters;
        private final Map<String, String> headers;
        private Object content;

        public Builder(String uri, String methodName) {
            this.parameters = UriUtils.getQueryParameters(uri);
            this.uri = UriUtils.clearQueryParams(Uri.parse(uri));
            this.httpMethod = methodName;
            this.headers = new HashMap<>();
        }

        public static Builder get(String uri) {
            return new Builder(uri, HttpGet.METHOD_NAME);
        }

        public static Builder post(String uri) {
            return new Builder(uri, HttpPost.METHOD_NAME);
        }

        public static Builder put(String uri) {
            return new Builder(uri, HttpPut.METHOD_NAME);
        }

        public static Builder delete(String uri) {
            return new Builder(uri, HttpDelete.METHOD_NAME);
        }

        public ApiRequest build() {
            checkNotNull(isPrivate, "Must specify api mode");
            if (isPrivate) {
                checkArgument(endpointVersion > 0, "Not a valid api version: %s", endpointVersion);
            }
            return new ApiRequest(uri, httpMethod, endpointVersion,
                    isPrivate, parameters, content, headers);
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

        public Builder withHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
                .add("uri", uri.toString())
                .add("httpMethod", httpMethod)
                .add("endPointVersion", endpointVersion)
                .add("isPrivate", isPrivate)
                .add("content", ScTextUtils.safeToString(content)).toString();
    }
}
