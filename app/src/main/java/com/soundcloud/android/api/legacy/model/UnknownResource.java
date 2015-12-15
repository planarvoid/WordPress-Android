package com.soundcloud.android.api.legacy.model;

import android.annotation.SuppressLint;
import android.net.Uri;

@SuppressLint("ParcelCreator")
public class UnknownResource extends PublicApiResource {

    public UnknownResource() {}

    public UnknownResource(long id) {
        super(id);
    }

    @Override
    public Uri toUri() {
        return null;
    }

    @Override
    public Uri getBulkInsertUri() {
        return null;
    }
}
