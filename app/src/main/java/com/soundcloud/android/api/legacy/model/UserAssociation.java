package com.soundcloud.android.api.legacy.model;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Currently maps to nothing on the back end. However, we should create UserAssociations there so we are consistent
 */
public class UserAssociation extends Association implements UserHolder {

    public static final Function<UserAssociation, String> TO_TOKEN_FUNCTION = new Function<UserAssociation, String>(){
        @Override
        public String apply(UserAssociation input) {
            return input.getToken();
        }
    };

    public static final Predicate<UserAssociation> HAS_TOKEN_PREDICATE = new Predicate<UserAssociation>(){
        @Override
        public boolean apply(UserAssociation input) {
            return input.hasToken();
        }
    };

    private @NotNull PublicApiUser mUser;
    private @Nullable Date      mAddedAt;
    private @Nullable Date      mRemovedAt;

    private @Nullable String    mToken;

    public enum LocalState {
        NONE, PENDING_ADDITION, PENDING_REMOVAL
    }
    public UserAssociation(Cursor cursor) {
        super(cursor);
        mUser = SoundCloudApplication.sModelManager.getCachedUserFromCursor(cursor, TableColumns.UserAssociationView._ID);
        mAddedAt = convertDirtyDate(cursor.getLong(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_ADDED_AT)));
        mRemovedAt = convertDirtyDate(cursor.getLong(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_REMOVED_AT)));
        mToken = cursor.getString(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_TOKEN));
    }

    public UserAssociation(Type typeEnum, @NotNull PublicApiUser user) {
        super(typeEnum.collectionType);
        mUser = user;
    }

    public UserAssociation(Parcel in) {
        super(in);
        mUser = in.readParcelable(ClassLoader.getSystemClassLoader());
        mAddedAt = (Date) in.readSerializable();
        mRemovedAt = (Date) in.readSerializable();
        mToken = in.readString();
    }

    @Override
    public PublicApiUser getUser() {
        return mUser;
    }

    @Nullable
    @Override
    public Playable getPlayable() {
        return null;
    }

    @Nullable
    public String getToken() {
        return mToken;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mUser, 0);
        dest.writeSerializable(mAddedAt);
        dest.writeSerializable(mRemovedAt);
        dest.writeString(mToken);
    }

    public long getItemId() {
        return mUser.getId();
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.UserAssociations.TARGET_ID, getItemId());
        cv.put(TableColumns.UserAssociations.OWNER_ID, SoundCloudApplication.instance.getAccountOperations().getLoggedInUserId());
        cv.put(TableColumns.UserAssociations.ASSOCIATION_TYPE, associationType);
        cv.put(TableColumns.UserAssociations.RESOURCE_TYPE, getResourceType());
        cv.put(TableColumns.UserAssociations.CREATED_AT, created_at.getTime());
        cv.put(TableColumns.UserAssociations.ADDED_AT, mAddedAt == null ? null : mAddedAt.getTime());
        cv.put(TableColumns.UserAssociations.REMOVED_AT, mRemovedAt == null ? null : mRemovedAt.getTime());
        cv.put(TableColumns.UserAssociations.TOKEN, mToken);
        return cv;
    }

    @Override
    public long getListItemId() {
        return getUser().getId() << 32 + associationType;
    }

    @Override
    public Refreshable getRefreshableResource() {
        return mUser;
    }

    public int getResourceType() {
        // currently there is on users in the user table. If we add groups or labels,
        // we may have to calculate varying resource types
        return PublicApiUser.TYPE;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.USER_ASSOCIATIONS.uri;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destination) {
        super.putDependencyValues(destination);
        mUser.putFullContentValues(destination);
    }

    public UserAssociation markForAddition(){
        return markForAddition(null);
    }

    public UserAssociation markForAddition(@Nullable String token){
        setLocalSyncState(LocalState.PENDING_ADDITION);
        mUser.addAFollower();
        mToken = token;
        return this;
    }

    public UserAssociation markForRemoval() {
        setLocalSyncState(LocalState.PENDING_REMOVAL);
        mUser.removeAFollower();
        return this;
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
                mToken = null;
                break;

            case NONE:
                mRemovedAt = null;
                mAddedAt = null;
                mToken = null;
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


    public boolean hasToken() {
        return ScTextUtils.isNotBlank(mToken);
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

    public static final Parcelable.Creator<UserAssociation> CREATOR = new Parcelable.Creator<UserAssociation>() {
        public UserAssociation createFromParcel(Parcel in) {
            return new UserAssociation(in);
        }

        public UserAssociation[] newArray(int size) {
            return new UserAssociation[size];
        }
    };

    @Nullable
    private Date convertDirtyDate(long timestamp){
        return (timestamp <= 0) ? null : new Date(timestamp);
    }
}
