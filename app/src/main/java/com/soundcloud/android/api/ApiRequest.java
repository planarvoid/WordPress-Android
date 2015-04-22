package com.soundcloud.android.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.io.File;
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
    @NotNull private final Multimap<String, String> queryParams;
    private final Object content;
    @NotNull private final Map<String, String> headers;

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
               Boolean isPrivate, @NotNull Multimap<String, String> queryParams, Object content,
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

    public static class Builder {
        private final Uri uri;
        private final String httpMethod;
        private int endpointVersion;
        private Boolean isPrivate;
        private final Multimap<String, String> parameters;
        private final Map<String, String> headers;
        private Object content;
        private List<ApiFileContentRequest.FileEntry> files;

        public Builder(String uri, String methodName) {
            this.parameters = UriUtils.getQueryParameters(uri);
            this.uri = UriUtils.clearQueryParams(Uri.parse(uri));
            this.httpMethod = methodName;
            this.headers = new HashMap<>();
        }

        public ApiRequest build() {
            checkNotNull(isPrivate, "Must specify api mode");
            if (isPrivate) {
                checkArgument(endpointVersion > 0, "Not a valid api version: %s", endpointVersion);
            }
            if (content != null) {
                return new ApiObjectContentRequest(uri, httpMethod, endpointVersion, isPrivate, parameters, headers, content);
            } else if (files != null) {
                return new ApiFileContentRequest(uri, httpMethod, endpointVersion, isPrivate, parameters, headers, files);
            } else {
                return new ApiRequest(uri, httpMethod, endpointVersion, isPrivate, parameters, content, headers);
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

        public Builder withFile(File file, String paramName, String fileName, String contentType) {
            if (this.files == null) {
                files = new ArrayList<>();
            }
            files.add(new ApiFileContentRequest.FileEntry(file, paramName, fileName, contentType));
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
