package com.soundcloud.android.collections.tasks;

import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;

import android.net.Uri;

public class CollectionParams<T extends ScModel> {
    public Class<T> loadModel;
    public Uri contentUri;
    public boolean isRefresh;
    private Request request;
    public boolean refreshPageItems;
    public int startIndex;
    public int maxToLoad;
    public long timestamp;


    public Content getContent() {
        return Content.match(contentUri);
    }

    public Uri getPagedUri() {
        if (contentUri == null) {
            return null;
        }

        Uri.Builder b = contentUri.buildUpon();
        if (startIndex > 0) {
            b.appendQueryParameter(ScContentProvider.Parameter.OFFSET, String.valueOf(startIndex));
        }
        b.appendQueryParameter(ScContentProvider.Parameter.LIMIT, String.valueOf(maxToLoad));
        return b.build();
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

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public boolean isRefresh() {
        return isRefresh;
    }
}
