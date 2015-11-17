package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToUser;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;
import android.util.Log;

public class Friend extends PublicApiResource implements Refreshable, RelatesToUser, UserHolder {
    @SuppressWarnings("UnusedDeclaration")
    public long[] connection_ids;
    public PublicApiUser user;

    public Friend() {
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
        user.putFullContentValues(destination);
    }

    public Friend(PublicApiUser user) {
        this.user = user;
    }

    @Override
    public Uri getBulkInsertUri() {
        return null;
    }

    @Override
    public PublicApiUser getUser() {
        return user;
    }

    @Override
    public Refreshable getRefreshableResource() {
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
