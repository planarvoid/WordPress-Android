package com.soundcloud.android.api;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.util.Map;

public class ApiObjectContentRequest extends ApiRequest {

    private final Object content;

    ApiObjectContentRequest(Uri uri, String method, int endpointVersion, Boolean isPrivate,
                            @NotNull Multimap<String, String> queryParams,
                            @NotNull Map<String, String> headers, Object content) {
        super(uri, method, endpointVersion, isPrivate, queryParams, headers);
        this.content = content;
    }

    public Object getContent() {
        return content;
    }

    public String getContentType() {
        return getAcceptMediaType();
    }
}
