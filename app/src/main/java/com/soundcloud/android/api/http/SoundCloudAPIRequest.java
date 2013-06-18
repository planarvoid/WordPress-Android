package com.soundcloud.android.api.http;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import com.google.common.base.Objects;
import com.google.common.reflect.TypeToken;
import org.apache.http.client.methods.HttpGet;

import android.net.Uri;

public class SoundCloudAPIRequest<ResourceType> implements APIRequest<ResourceType>{

    private final Uri mUri;
    private final String mHttpMethod;
    private final int mEndpointVersion;
    private final TypeToken<ResourceType> resourceType;
    private final Boolean mIsPrivate;

    private SoundCloudAPIRequest(Uri uri, String method, int endpointVersion, TypeToken<ResourceType> typeToken,
                                 Boolean isPrivate) {
        mUri = uri;
        mHttpMethod = method;
        mEndpointVersion = endpointVersion;
        resourceType = typeToken;
        mIsPrivate = isPrivate;
    }

    @Override
    public String getUriPath() {
        return mUri.getPath();
    }

    @Override
    public TypeToken<ResourceType> getResourceType() {
        return resourceType;
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


    public static class RequestBuilder<ResourceType> {
        private final String uriPath;
        private final String mHttpMethod;
        private int mEndpointVersion;
        private TypeToken<ResourceType> mResourceType;
        private Boolean mIsPrivate;

        public RequestBuilder(String builder, String methodName) {
            uriPath = builder;
            mHttpMethod = methodName;
        }

        public static <ResourceType> RequestBuilder<ResourceType> get(String uriPath) {
            return new RequestBuilder<ResourceType>(uriPath, HttpGet.METHOD_NAME);
        }

        public APIRequest<ResourceType> build(){
            checkArgument(!isNullOrEmpty(nullToEmpty(uriPath).trim()), "URI needs to be valid value");
            checkNotNull(mIsPrivate, "Must specify api mode");
            if(mIsPrivate){
                checkArgument(mEndpointVersion > 0, "Not a valid api version: %s", mEndpointVersion);
            }
            return new SoundCloudAPIRequest<ResourceType>(Uri.parse(uriPath), mHttpMethod, mEndpointVersion,
                    mResourceType, mIsPrivate);
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

    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("uriPath", mUri.getPath())
                .add("httpMethod", mHttpMethod)
                .add("endPointVersion", mEndpointVersion)
                .add("isPrivate", mIsPrivate)
                .add("resourceType", resourceType).toString();
    }
}
