package com.soundcloud.android.task.collection;

import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.provider.Content;
import com.soundcloud.api.Request;

import android.net.Uri;

public class CollectionParams<T extends ScModel> {
    public Class<T> loadModel;
    public Uri contentUri;
    public boolean isRefresh;
    public Request request;
    public boolean refreshPageItems;
    public int startIndex;
    public int maxToLoad;
    public long timestamp;


    public Content getContent() {
        return Content.match(contentUri);
    }

    @Override
    public String toString() {
        return "CollectionParams{" +
                "loadModel=" + loadModel +
                ", contentUri=" + contentUri +
                ", isRefresh=" + isRefresh +
                ", request=" + request +
                ", refreshPageItems=" + refreshPageItems +
                ", startIndex=" + startIndex +
                ", maxToLoad=" + maxToLoad +
                ", timestamp=" + timestamp +
                '}';
    }
}
