package com.soundcloud.android.api.http;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.utils.ScTextUtils.isNotBlank;

import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import android.net.Uri;

import java.util.Collection;

public class SoundCloudAPIRequest<ResourceType> implements APIRequest<ResourceType>{

    private final Uri mUri;
    private final String mHttpMethod;
    private final int mEndpointVersion;
    private final TypeToken<ResourceType> mResourceType;
    private final Boolean mIsPrivate;
    private final Multimap<String, String> mQueryParams;
    private final Object mContent;

    private SoundCloudAPIRequest(Uri uri, String method, int endpointVersion, TypeToken<ResourceType> typeToken,
                                 Boolean isPrivate, Multimap<String, String> queryParams, Object content) {
        mUri = uri;
        mHttpMethod = method;
        mEndpointVersion = endpointVersion;
        mResourceType = typeToken;
        mIsPrivate = isPrivate;
        mQueryParams = queryParams;
        mContent = content;
    }

    @Override
    public String getUriPath() {
        return mUri.getPath();
    }

    @Override
    public TypeToken<ResourceType> getResourceType() {
        return mResourceType;
    }

    @Override
    public String getMethod() {
        return mHttpMethod;
    }

    @Override
    public int getVersion() {
        return mEndpointVersion;
    }

    @Override
    public boolean isPrivate() {
        return mIsPrivate;
    }

    @Override
    public Multimap<String, String> getQueryParameters(){
        return mQueryParams;
    }

    @Override
    public Object getContent() {
        return mContent;
    }


    public static class RequestBuilder<ResourceType> {
        private final String uriPath;
        private final String mHttpMethod;
        private int mEndpointVersion;
        private TypeToken<ResourceType> mResourceType;
        private Boolean mIsPrivate;
        private final Multimap<String, String> mParameters;
        private Object mContent;

        public RequestBuilder(String builder, String methodName) {
            uriPath = builder;
            mHttpMethod = methodName;
            mParameters = ArrayListMultimap.create();
        }

        public static <ResourceType> RequestBuilder<ResourceType> get(String uriPath) {
            return new RequestBuilder<ResourceType>(uriPath, HttpGet.METHOD_NAME);
        }

        public static <ResourceType> RequestBuilder<ResourceType> post(String uriPath) {
            return new RequestBuilder<ResourceType>(uriPath, HttpPost.METHOD_NAME);
        }

        public APIRequest<ResourceType> build(){
            checkArgument(isNotBlank(uriPath), "URI needs to be valid value");
            checkNotNull(mIsPrivate, "Must specify api mode");
            if(mIsPrivate){
                checkArgument(mEndpointVersion > 0, "Not a valid api version: %s", mEndpointVersion);
            }
            return new SoundCloudAPIRequest<ResourceType>(Uri.parse(uriPath), mHttpMethod, mEndpointVersion,
                    mResourceType, mIsPrivate, mParameters, mContent);
        }

        public RequestBuilder<ResourceType> forResource(TypeToken<ResourceType> typeToken) {
            mResourceType = typeToken;
            return this;
        }

        public RequestBuilder<ResourceType> forResource(Class<ResourceType> clazz) {
            if(!equal(clazz, null)){
                mResourceType = TypeToken.of(clazz);
            }
            return this;
        }

        public RequestBuilder<ResourceType> forPrivateAPI(int version) {
            mIsPrivate = true;
            mEndpointVersion = version;
            return this;
        }

        public RequestBuilder<ResourceType> forPublicAPI() {
            mIsPrivate = false;
            return this;
        }

        public RequestBuilder<ResourceType> addQueryParameters(String key, Object... values){
            for(Object object : values){
                mParameters.put(key, object.toString());
            }
            return this;
        }

        public RequestBuilder<ResourceType> addQueryParametersAsCollection(String key, Collection<? extends Object> values){
            mParameters.putAll(key, Collections2.transform(values, Functions.toStringFunction()));
            return this;
        }

        public RequestBuilder<ResourceType> withContent(Object content) {
            mContent = content;
            return this;
        }

    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("uriPath", mUri.getPath())
                .add("httpMethod", mHttpMethod)
                .add("endPointVersion", mEndpointVersion)
                .add("isPrivate", mIsPrivate)
                .add("resourceType", mResourceType)
                .add("content", mContent.toString()).toString();
    }
}
