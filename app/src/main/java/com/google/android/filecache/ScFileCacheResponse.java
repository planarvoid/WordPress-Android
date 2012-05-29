package com.google.android.filecache;

import com.soundcloud.android.cache.FileCache;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

// FileCache response has package access - override
public class ScFileCacheResponse extends FileCacheResponse {
    private final File cachedFile;

    public ScFileCacheResponse(File file) {
        super(file);
        cachedFile = file;
    }

    @Override public InputStream getBody() throws IOException {
        touch();
        return super.getBody();
    }

    public void touch() {
        if (cachedFile != null && !cachedFile.setLastModified(System.currentTimeMillis())) {
            if (Build.VERSION.SDK_INT < 11) {
                Log.w(FileCache.TAG, "could not touch "+cachedFile);
            }
        }
    }
}
