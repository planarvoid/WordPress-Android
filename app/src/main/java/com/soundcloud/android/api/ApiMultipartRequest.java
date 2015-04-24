package com.soundcloud.android.api;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.util.List;
import java.util.Map;

public class ApiMultipartRequest extends ApiRequest {

    private final List<FormPart> parts;

    ApiMultipartRequest(Uri uri, String method, int endpointVersion, Boolean isPrivate,
                        @NotNull Multimap<String, String> queryParams,
                        @NotNull Map<String, String> headers, List<FormPart> parts) {
        super(uri, method, endpointVersion, isPrivate, queryParams, headers);
        this.parts = parts;
    }

    public List<FormPart> getParts() {
        return parts;
    }

}
