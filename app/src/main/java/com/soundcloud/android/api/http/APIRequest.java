package com.soundcloud.android.api.http;


import com.google.common.reflect.TypeToken;

import java.util.Map;

public interface APIRequest<ResourceType> {
    String getUriPath();
    Map<String, String> getQueryParameters();
    TypeToken<ResourceType> getResourceType();


}
