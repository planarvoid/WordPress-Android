package com.soundcloud.android.model;

import android.net.Uri;

public class UnknownResource extends ScResource {
    @Override
    public Uri getBulkInsertUri() {
        return null;
    }

    @Override
    public User getUser() {
        return null;
    }

    @Override
    public Track getSound() {
        return null;
    }
}
