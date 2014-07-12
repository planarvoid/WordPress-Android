package com.soundcloud.android.api.legacy.model;

import android.net.Uri;

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
