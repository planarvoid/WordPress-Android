package com.soundcloud.android.api;

import com.soundcloud.java.collections.MultiMap;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.util.Map;

public class ApiObjectContentRequest extends ApiRequest {

    private final Object content;

    ApiObjectContentRequest(Uri uri,
                            String method,
                            Boolean isPrivate,
                            @NotNull MultiMap<String, String> queryParams,
                            @NotNull Map<String, String> headers,
                            Object content,
                            boolean sendAuthorizationToken) {
        super(uri, method, isPrivate, queryParams, headers, sendAuthorizationToken);
        this.content = content;
    }

    public Object getContent() {
        return content;
    }

    public String getContentType() {
        return getAcceptMediaType();
    }
}
