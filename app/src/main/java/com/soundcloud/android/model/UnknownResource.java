package com.soundcloud.android.model;

import android.net.Uri;

public class UnknownResource extends ScResource {

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
