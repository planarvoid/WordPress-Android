package com.soundcloud.android.model;

import android.net.Uri;

public class Friend extends ScResource implements Refreshable {
    @SuppressWarnings("UnusedDeclaration")
    public long[] connection_ids;
    public User user;

    public Friend() {
    }

    public Friend(User user) {
        this.user = user;
    }

    @Override
    public Uri getBulkInsertUri() {
        return null;
    }

    @Override
    public Track getPlayable() {
        return null;
    }

    @Override
    public User getUser() {
        return user;
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
