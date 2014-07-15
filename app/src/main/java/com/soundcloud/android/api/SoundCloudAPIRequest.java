package com.soundcloud.android.api;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;

import android.net.Uri;

import java.util.Collection;

public class SoundCloudAPIRequest<ResourceType> implements APIRequest<ResourceType> {

    private final Uri uri;
    private final String httpMethod;
    private final int endpointVersion;
    private final TypeToken<ResourceType> resourceType;
    private final Boolean isPrivate;
    private final Multimap<String, String> queryParams;
    private final Object content;

    private SoundCloudAPIRequest(Uri uri, String method, int endpointVersion, TypeToken<ResourceType> typeToken,
                                 Boolean isPrivate, Multimap<String, String> queryParams, Object content) {
        this.uri = uri;
        this.httpMethod = method;
        this.endpointVersion = endpointVersion;
        this.resourceType = typeToken;
        this.isPrivate = isPrivate;
        this.queryParams = queryParams;
        this.content = content;
    }

    @Override
    public String getEncodedPath() {
        return uri.getEncodedPath();
    }

    @Override
    public TypeToken<ResourceType> getResourceType() {
        return resourceType;
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public int getVersion() {
        return endpointVersion;
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public Multimap<String, String> getQueryParameters() {
        return queryParams;
    }

    @Override
    public Object getContent() {
        return content;
    }


    public static class RequestBuilder<ResourceType> {
        private final String uri;
        private final String httpMethod;
        private int endpointVersion;
        private TypeToken<ResourceType> resourceType;
        private Boolean isPrivate;
        private final Multimap<String, String> parameters;
        private Object content;

        public RequestBuilder(String uri, String methodName) {
            this.uri = uri;
            httpMethod = methodName;
            parameters = UriUtils.getQueryParameters(uri);
        }

        public static <ResourceType> RequestBuilder<ResourceType> get(String uri) {
            return new RequestBuilder<ResourceType>(uri, HttpGet.METHOD_NAME);
        }

        public static <ResourceType> RequestBuilder<ResourceType> post(String uri) {
            return new RequestBuilder<ResourceType>(uri, HttpPost.METHOD_NAME);
        }

        public static <ResourceType> RequestBuilder<ResourceType> put(String uri) {
            return new RequestBuilder<ResourceType>(uri, HttpPut.METHOD_NAME);
        }

        public static <ResourceType> RequestBuilder<ResourceType> delete(String uri) {
            return new RequestBuilder<ResourceType>(uri, HttpDelete.METHOD_NAME);
        }

        public APIRequest<ResourceType> build() {
            checkArgument(isNotBlank(uri), "URI needs to be valid value");
            checkNotNull(isPrivate, "Must specify api mode");
            if (isPrivate) {
                checkArgument(endpointVersion > 0, "Not a valid api version: %s", endpointVersion);
            }
            return new SoundCloudAPIRequest<ResourceType>(Uri.parse(uri), httpMethod, endpointVersion,
                    resourceType, isPrivate, parameters, content);
        }

        public RequestBuilder<ResourceType> forResource(TypeToken<ResourceType> typeToken) {
            resourceType = typeToken;
            return this;
        }

        public RequestBuilder<ResourceType> forResource(Class<ResourceType> clazz) {
            if (!equal(clazz, null)) {
                resourceType = TypeToken.of(clazz);
            }
            return this;
        }

        public RequestBuilder<ResourceType> forPrivateAPI(int version) {
            isPrivate = true;
            endpointVersion = version;
            return this;
        }

        public RequestBuilder<ResourceType> forPublicAPI() {
            isPrivate = false;
            return this;
        }

        public RequestBuilder<ResourceType> addQueryParameters(String key, Object... values) {
            for (Object object : values) {
                parameters.put(key, object.toString());
            }
            return this;
        }

        public RequestBuilder<ResourceType> addQueryParametersAsCollection(String key, Collection<? extends Object> values) {
            parameters.putAll(key, Collections2.transform(values, Functions.toStringFunction()));
            return this;
        }

        public RequestBuilder<ResourceType> withContent(Object content) {
            this.content = content;
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
                .add("resourceType", resourceType)
                .add("content", ScTextUtils.safeToString(content)).toString();
    }
}
