package com.soundcloud.android.api;

import com.soundcloud.java.collections.MultiMap;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.util.List;
import java.util.Map;

public class ApiMultipartRequest extends ApiRequest {

    private final List<FormPart> parts;
    private final ProgressListener progressListener;

    ApiMultipartRequest(Uri uri, String method, int endpointVersion, Boolean isPrivate,
                        @NotNull MultiMap<String, String> queryParams,
                        @NotNull Map<String, String> headers, List<FormPart> parts, ProgressListener progressListener) {
        super(uri, method, endpointVersion, isPrivate, queryParams, headers);
        this.parts = parts;
        this.progressListener = progressListener;
    }

    public List<FormPart> getParts() {
        return parts;
    }

    public ProgressListener getProgressListener() {
        return progressListener;
    }

    boolean hasProgressListener() {
        return progressListener != null;
    }
}
