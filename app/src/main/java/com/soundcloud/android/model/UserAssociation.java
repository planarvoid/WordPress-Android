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

    private @NotNull User       mUser;
    private @Nullable Date      mAddedAt;
    private @Nullable Date      mRemovedAt;

    public UserAssociation(Cursor cursor) {
        super(cursor);
        mUser = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromCursor(cursor, DBHelper.UserAssociationView._ID);
        mAddedAt = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.UserAssociationView.USER_ASSOCIATION_ADDED_AT)));
        mRemovedAt = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.UserAssociationView.USER_ASSOCIATION_REMOVED_AT)));
    }

    public UserAssociation(Type typeEnum, @NotNull User user) {
        super(typeEnum.collectionType);
        mUser = user;
    }

    public UserAssociation(Parcel in) {
        super(in);
        mUser = in.readParcelable(ClassLoader.getSystemClassLoader());
        mAddedAt = new Date(in.readLong());
        mRemovedAt = new Date(in.readLong());
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
        dest.writeSerializable(mAddedAt);
        dest.writeSerializable(mRemovedAt);
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
        if (mAddedAt != null && mAddedAt.getTime() > 0) cv.put(DBHelper.UserAssociations.ADDED_AT, mAddedAt.getTime());
        if (mRemovedAt != null && mRemovedAt.getTime() > 0) cv.put(DBHelper.UserAssociations.REMOVED_AT, mRemovedAt.getTime());
        return cv;
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

    /**
     * Mark this item for addition. It will be pushed to the server during the sync process
     */
    public void markForAddition() {
        mAddedAt = new Date(System.currentTimeMillis());
        mRemovedAt = null;
    }

    /**
     * Mark this item for deletion. The deletion will be pushed during the sync process.
     */
    public void markForRemoval() {
        mRemovedAt = new Date(System.currentTimeMillis());
        mAddedAt = null;
    }

    public boolean isMarkedForAddition() {
        return mAddedAt != null && mAddedAt.getTime() > 0;
    }

    public boolean isMarkedForRemoval() {
        return mRemovedAt != null && mRemovedAt.getTime() > 0;
    }
}
