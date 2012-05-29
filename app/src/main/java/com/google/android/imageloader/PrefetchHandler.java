package com.google.android.imageloader;

import com.google.android.filecache.FileResponseCache;

import java.io.IOException;
import java.net.ContentHandler;
import java.net.URLConnection;

public class PrefetchHandler extends ContentHandler {
    @Override
    public Object getContent(URLConnection connection) throws IOException {
        return FileResponseCache.sink().getContent(connection);
    }
}
