package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
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

    public static final Function<UserAssociation, String> TO_TOKEN_FUNCTION = new Function<UserAssociation, String>() {
        @Override
        public String apply(UserAssociation input) {
            return input.getToken();
        }
    };

    public static final Predicate<UserAssociation> HAS_TOKEN_PREDICATE = new Predicate<UserAssociation>() {
        @Override
        public boolean apply(UserAssociation input) {
            return input.hasToken();
        }
    };

    public static final Parcelable.Creator<UserAssociation> CREATOR = new Parcelable.Creator<UserAssociation>() {
        public UserAssociation createFromParcel(Parcel in) {
            return new UserAssociation(in);
        }
        public UserAssociation[] newArray(int size) {
            return new UserAssociation[size];
        }
    };

    @NotNull private final PublicApiUser user;
    @Nullable private Date addedAt;
    @Nullable private Date removedAt;
    @Nullable private String token;

    public UserAssociation(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.sModelManager.getCachedUserFromCursor(cursor, TableColumns.UserAssociationView._ID);
        addedAt = convertDirtyDate(cursor.getLong(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_ADDED_AT)));
        removedAt = convertDirtyDate(cursor.getLong(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_REMOVED_AT)));
        token = cursor.getString(cursor.getColumnIndex(TableColumns.UserAssociationView.USER_ASSOCIATION_TOKEN));
    }

    public UserAssociation(Type typeEnum, @NotNull PublicApiUser user) {
        super(typeEnum.collectionType);
        this.user = user;
    }

    public UserAssociation(Parcel in) {
        super(in);
        user = in.readParcelable(ClassLoader.getSystemClassLoader());
        addedAt = (Date) in.readSerializable();
        removedAt = (Date) in.readSerializable();
        token = in.readString();
    }

    @Override
    public PublicApiUser getUser() {
        return user;
    }

    @Nullable
    @Override
    public Playable getPlayable() {
        return null;
    }

    @Nullable
    public String getToken() {
        return token;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(user, 0);
        dest.writeSerializable(addedAt);
        dest.writeSerializable(removedAt);
        dest.writeString(token);
    }

    public long getItemId() {
        return user.getId();
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.UserAssociations.TARGET_ID, getItemId());
        cv.put(TableColumns.UserAssociations.OWNER_ID, SoundCloudApplication.instance.getAccountOperations().getLoggedInUserId());
        cv.put(TableColumns.UserAssociations.ASSOCIATION_TYPE, associationType);
        cv.put(TableColumns.UserAssociations.RESOURCE_TYPE, getResourceType());
        cv.put(TableColumns.UserAssociations.CREATED_AT, created_at.getTime());
        cv.put(TableColumns.UserAssociations.ADDED_AT, addedAt == null ? null : addedAt.getTime());
        cv.put(TableColumns.UserAssociations.REMOVED_AT, removedAt == null ? null : removedAt.getTime());
        cv.put(TableColumns.UserAssociations.TOKEN, token);
        return cv;
    }

    @Override
    public long getListItemId() {
        return getUser().getId() << 32 + associationType;
    }

    @Override
    public Refreshable getRefreshableResource() {
        return user;
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
        user.putFullContentValues(destination);
    }

    public UserAssociation markForAddition() {
        return markForAddition(null);
    }

    public UserAssociation markForAddition(@Nullable String token) {
        setLocalSyncState(LocalState.PENDING_ADDITION);
        user.addAFollower();
        this.token = token;
        return this;
    }

    public UserAssociation markForRemoval() {
        setLocalSyncState(LocalState.PENDING_REMOVAL);
        user.removeAFollower();
        return this;
    }

    public void clearLocalSyncState() {
        setLocalSyncState(LocalState.NONE);
    }

    public LocalState getLocalSyncState() {
        if (addedAt != null) {
            return LocalState.PENDING_ADDITION;
        } else if (removedAt != null) {
            return LocalState.PENDING_REMOVAL;
        } else {
            return LocalState.NONE;
        }
    }

    private void setLocalSyncState(LocalState newState) {
        switch (newState) {
            case PENDING_ADDITION:
                addedAt = new Date(System.currentTimeMillis());
                removedAt = null;
                break;

            case PENDING_REMOVAL:
                removedAt = new Date(System.currentTimeMillis());
                addedAt = null;
                token = null;
                break;

            case NONE:
                removedAt = null;
                addedAt = null;
                token = null;
                break;

            default:
                throw new IllegalArgumentException("Unknown newState: " + newState.name());
        }
    }

    public boolean hasToken() {
        return ScTextUtils.isNotBlank(token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserAssociation)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        UserAssociation that = (UserAssociation) o;

        if (addedAt != null ? !addedAt.equals(that.addedAt) : that.addedAt != null) {
            return false;
        }
        if (removedAt != null ? !removedAt.equals(that.removedAt) : that.removedAt != null) {
            return false;
        }
        if (!user.equals(that.user)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + user.hashCode();
        result = 31 * result + (addedAt != null ? addedAt.hashCode() : 0);
        result = 31 * result + (removedAt != null ? removedAt.hashCode() : 0);
        return result;
    }

    @Nullable
    private Date convertDirtyDate(long timestamp) {
        return (timestamp <= 0) ? null : new Date(timestamp);
    }

    public enum LocalState {
        NONE, PENDING_ADDITION, PENDING_REMOVAL
    }
}
