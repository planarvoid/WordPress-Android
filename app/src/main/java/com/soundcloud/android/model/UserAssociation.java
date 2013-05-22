package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.database.Cursor;
import android.os.Parcel;

import java.util.Date;

/**
 * Currently maps to nothing on the back end. However, we should create UserAssociations there so we are consistent
 */
public class UserAssociation extends Association implements UserHolder {

    private @NotNull User mUser;

    public UserAssociation(Cursor cursor) {
        mUser = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromCursor(cursor, DBHelper.UserAssociationView._ID);
    }

    /**
     * Use this ctor to create user associations for followings or followers
     * @param user the user that was followed or is following the owner
     * @param typeEnum the kind of association (FOLLOWER or FOLLOWING)
     */
    public UserAssociation(@NotNull User user, Date associatedAt, Type typeEnum) {
        super(associatedAt, typeEnum.collectionType);
        this.mUser = user;
    }

    public UserAssociation(Parcel in) {
        super(in);
        mUser = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    @Override
    public User getUser() {
        return mUser;
    }

    @Nullable
    @Override
    public Playable getPlayable() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mUser, 0);
    }

    public long getItemId() {
        return mUser.getId();
    }

    @Override
    public long getListItemId() {
        return mUser.getId();
    }

    @Override
    public Refreshable getRefreshableResource() {
        return mUser;
    }

    public int getResourceType() {
        // currently there is on users in the user table. If we add groups or labels,
        // we may have to calculate varying resource types
        return User.TYPE;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destination) {
        super.putDependencyValues(destination);
        mUser.putFullContentValues(destination);
    }
}
