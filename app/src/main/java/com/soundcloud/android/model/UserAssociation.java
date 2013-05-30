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

    public enum LocalState {
        NONE, PENDING_ADDITION, PENDING_REMOVAL
    }

    public UserAssociation(Cursor cursor) {
        super(cursor);
        mUser = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromCursor(cursor, DBHelper.UserAssociationView._ID);
        mAddedAt = convertDirtyDate(cursor.getLong(cursor.getColumnIndex(DBHelper.UserAssociationView.USER_ASSOCIATION_ADDED_AT)));
        mRemovedAt = convertDirtyDate(cursor.getLong(cursor.getColumnIndex(DBHelper.UserAssociationView.USER_ASSOCIATION_REMOVED_AT)));
    }

    public UserAssociation(Type typeEnum, @NotNull User user) {
        super(typeEnum.collectionType);
        mUser = user;
    }

    public UserAssociation(Parcel in) {
        super(in);
        mUser = in.readParcelable(ClassLoader.getSystemClassLoader());
        mAddedAt = convertDirtyDate(in.readLong());
        mRemovedAt = convertDirtyDate(in.readLong());
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
        dest.writeLong(mAddedAt == null ? -1 : mAddedAt.getTime());
        dest.writeLong(mRemovedAt == null ? -1 : mRemovedAt.getTime());
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
        cv.put(DBHelper.UserAssociations.ADDED_AT, mAddedAt == null ? null : mAddedAt.getTime());
        cv.put(DBHelper.UserAssociations.REMOVED_AT, mRemovedAt == null ? null : mRemovedAt.getTime());
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

    public void markForAddition(){
        setLocalSyncState(LocalState.PENDING_ADDITION);
    }

    public void markForRemoval() {
        setLocalSyncState(LocalState.PENDING_REMOVAL);
    }

    public void clearLocalSyncState() {
        setLocalSyncState(LocalState.NONE);
    }

    private void setLocalSyncState(LocalState newState) {
        switch (newState) {
            case PENDING_ADDITION:
                mAddedAt = new Date(System.currentTimeMillis());
                mRemovedAt = null;
                break;

            case PENDING_REMOVAL:
                mRemovedAt = new Date(System.currentTimeMillis());
                mAddedAt = null;
                break;

            case NONE:
                mRemovedAt = null;
                mAddedAt = null;
                break;
        }
    }

    public LocalState getLocalSyncState(){
        if (mAddedAt != null){
            return LocalState.PENDING_ADDITION;
        } else if (mRemovedAt != null){
            return LocalState.PENDING_REMOVAL;
        } else {
            return LocalState.NONE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAssociation)) return false;
        if (!super.equals(o)) return false;

        UserAssociation that = (UserAssociation) o;

        if (mAddedAt != null ? !mAddedAt.equals(that.mAddedAt) : that.mAddedAt != null) return false;
        if (mRemovedAt != null ? !mRemovedAt.equals(that.mRemovedAt) : that.mRemovedAt != null) return false;
        if (!mUser.equals(that.mUser)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mUser.hashCode();
        result = 31 * result + (mAddedAt != null ? mAddedAt.hashCode() : 0);
        result = 31 * result + (mRemovedAt != null ? mRemovedAt.hashCode() : 0);
        return result;
    }

    @Nullable
    private Date convertDirtyDate(long timestamp){
        return (timestamp <= 0) ? null : new Date(timestamp);
    }
}
