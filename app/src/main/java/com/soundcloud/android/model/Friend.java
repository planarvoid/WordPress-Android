package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;

import android.net.Uri;
import android.util.Log;

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

    /**
     * Friends are not directly persisted in the database yet.
     *
     * @return null
     */
    @Override
    public Uri toUri() {
        Log.e(SoundCloudApplication.TAG, "Unexpected call to toUri on a Friend");
        return null;
    }
}
