package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

/**
 * Currently maps to nothing on the back end. However, we should create UserAssociations there so we are consistent
 */
public class UserAssociation extends Association {

    public @NotNull User user;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public UserAssociation() { }

    public UserAssociation(Cursor cursor) {
        user = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromCursor(cursor, DBHelper.UserAssociationView._ID);
    }

    /**
     * Use this ctor to create user associations for followings or followers
     * @param user the user that was followed or is following the owner
     * @param typeEnum the kind of association (FOLLOWER or FOLLOWING)
     */
    public UserAssociation(@NotNull User user, Date associatedAt, Type typeEnum) {
        super(associatedAt, typeEnum.collectionType);
        this.user = user;
    }

    public UserAssociation(Parcel in) {
        super(in);
        user = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.COLLECTION_ITEMS.uri;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Nullable
    @Override
    public Playable getPlayable() {
        return null;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(user, 0);
    }

    public long getItemId() {
        return user.getId();
    }

    @Override
    public long getListItemId() {
        return user.getId();
    }

    @Override
    public ScResource getRefreshableResource() {
        return user;
    }

    @Override
    public boolean isStale() {
        return user.isStale();
    }

    @Override
    public boolean isIncomplete() {
        return user.isIncomplete();
    }

    public int getResourceType() {
        // currently there is on users in the user table. If we add groups or labels,
        // we may have to calculate varying resource types
        return User.TYPE;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destination) {
        super.putDependencyValues(destination);
        user.putFullContentValues(destination);
    }
}
