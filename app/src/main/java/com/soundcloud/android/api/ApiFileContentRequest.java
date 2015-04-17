package com.soundcloud.android.api;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ApiFileContentRequest extends ApiRequest {

    private final List<FileEntry> files;

    ApiFileContentRequest(Uri uri, String method, int endpointVersion, Boolean isPrivate,
                          @NotNull Multimap<String, String> queryParams,
                          @NotNull Map<String, String> headers, List<FileEntry> files) {
        super(uri, method, endpointVersion, isPrivate, queryParams, headers);
        this.files = files;
    }

    public List<FileEntry> getFiles() {
        return files;
    }

    static class FileEntry {
        final File file;
        final String paramName;
        final String fileName;
        final String contentType;

        FileEntry(File file, String paramName, String fileName, String contentType) {
            this.file = file;
            this.paramName = paramName;
            this.fileName = fileName;
            this.contentType = contentType;
        }
    }
}
