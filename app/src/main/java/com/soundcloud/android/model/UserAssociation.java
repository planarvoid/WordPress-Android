package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.behavior.Refreshable;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;

import java.util.Date;

/**
 * Currently maps to nothing on the back end. However, we should create UserAssociations there so we are consistent
 */
public class UserAssociation extends Association implements UserHolder {

    private @NotNull User mUser;

    public UserAssociation(Cursor cursor) {
        super(cursor);
        mUser = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromCursor(cursor, DBHelper.UserAssociationView._ID);
    }

    public UserAssociation(@NotNull User user, Type typeEnum, Date associatedAt) {
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
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.UserAssociations.TARGET_ID, getItemId());
        cv.put(DBHelper.UserAssociations.OWNER_ID, SoundCloudApplication.getUserId());
        cv.put(DBHelper.UserAssociations.ASSOCIATION_TYPE, associationType);
        cv.put(DBHelper.UserAssociations.RESOURCE_TYPE, getResourceType());
        cv.put(DBHelper.UserAssociations.CREATED_AT, created_at.getTime());
        return cv;
    }

    @Override
    public long getListItemId() {
        return getUser().id << 32 + associationType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAssociation)) return false;
        if (!super.equals(o)) return false;

        UserAssociation that = (UserAssociation) o;

        if (!mUser.equals(that.mUser)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mUser.hashCode();
        return result;
    }
}
