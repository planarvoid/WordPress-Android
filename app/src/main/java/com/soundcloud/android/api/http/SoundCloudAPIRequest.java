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

    private Uri mUri;
    private String mHttpMethod;
    private int mEndpointVersion;
    private TypeToken<ResourceType> resourceType;
    private Boolean mIsPrivate;

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
        private String uriPath;
        private String mHttpMethod;
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

        public RequestBuilder<ResourceType> forVersion(int versionCode) {
            mEndpointVersion = versionCode;
            return this;
        }

        public APIRequest<ResourceType> build(){
            checkArgument(!isNullOrEmpty(nullToEmpty(uriPath).trim()), "URI needs to be valid value");
            if(mIsPrivate){
                checkArgument(mEndpointVersion > 0, "Not a valid version code: %s", mEndpointVersion);
            }
            checkNotNull(mIsPrivate, "Must specify resource type");
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

        public RequestBuilder<ResourceType> forPrivateAPI() {
            mIsPrivate = true;
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
