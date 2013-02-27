package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.BulkInsertMap;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class Friend extends ScResource implements Refreshable {
    @SuppressWarnings("UnusedDeclaration")
    public long[] connection_ids;
    public User user;

    public Friend() {
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
        user.putFullContentValues(destination);
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
    public Intent getViewIntent() {
        return getUser().getViewIntent();
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

    @Override
    public boolean isIncomplete() {
        return user.isIncomplete();
    }
}
