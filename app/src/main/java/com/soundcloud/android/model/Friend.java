package com.soundcloud.android.model;

import android.net.Uri;

public class Friend extends ScResource implements Refreshable {
    public long[] connection_ids;
    public User user;

    public Friend() {
    }

    @Override
    public Uri getBulkInsertUri() {
        return null;
    }

    @Override
    public Track getTrack() {
        return null;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public long getRefreshableId() {
        return user.id;
    }

    @Override
    public ScResource getRefreshableResource() {
        return user;
    }

    @Override
    public boolean isStale() {
        return user.isStale();
    }
}
