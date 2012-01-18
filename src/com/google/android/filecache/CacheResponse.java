package com.google.android.filecache;

import java.io.File;

// FileCache response has package access - override
public class CacheResponse extends FileCacheResponse {
    public CacheResponse(File file) {
        super(file);
    }
}
