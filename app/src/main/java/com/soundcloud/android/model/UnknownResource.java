package com.soundcloud.android.model;

import android.net.Uri;

import java.util.List;

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

    @Override
    public User getUser() {
        return null;
    }

    @Override
    public Track getPlayable() {
        return null;
    }
}
