package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.behavior.Creation;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToUser;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

public abstract class Association extends PublicApiResource implements PlayableHolder, Refreshable, RelatesToUser, Creation {

    public enum Type {
        TRACK(CollectionStorage.CollectionItemTypes.TRACK),
        TRACK_REPOST(CollectionStorage.CollectionItemTypes.REPOST),
        TRACK_LIKE(CollectionStorage.CollectionItemTypes.LIKE),
        PLAYLIST(CollectionStorage.CollectionItemTypes.PLAYLIST),
        PLAYLIST_REPOST(CollectionStorage.CollectionItemTypes.REPOST),
        PLAYLIST_LIKE(CollectionStorage.CollectionItemTypes.LIKE),
        FOLLOWING(CollectionStorage.CollectionItemTypes.FOLLOWING),
        FOLLOWER(CollectionStorage.CollectionItemTypes.FOLLOWER);

        Type(int collectionType) {
            this.collectionType = collectionType;
        }

        public final int collectionType;
    }


    public @Nullable PublicApiUser owner;
    public int              associationType;
    public Date             created_at;

    protected CharSequence mElapsedTime;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public Association() {}

    public Association(Cursor cursor){
        owner = SoundCloudApplication.sModelManager.getUser(cursor.getLong(cursor.getColumnIndex(TableColumns.AssociationView.ASSOCIATION_OWNER_ID)));
        created_at = new Date(cursor.getLong(cursor.getColumnIndex(TableColumns.AssociationView.ASSOCIATION_TIMESTAMP)));
        associationType = cursor.getInt(cursor.getColumnIndex(TableColumns.AssociationView.ASSOCIATION_TYPE));
    }

    public Association(int associationType) {
        this(associationType, new Date());
    }

    public Association(int associationType, Date associatedAt) {
        this.associationType = associationType;
        this.created_at = associatedAt;
    }

    public Association(Parcel in) {
        associationType = in.readInt();
        created_at = new Date(in.readLong());
        owner = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.CollectionItems.ITEM_ID, getItemId());
        cv.put(TableColumns.CollectionItems.USER_ID, SoundCloudApplication.instance.getAccountOperations().getLoggedInUserId());
        cv.put(TableColumns.CollectionItems.COLLECTION_TYPE, associationType);
        cv.put(TableColumns.CollectionItems.RESOURCE_TYPE, getResourceType());
        cv.put(TableColumns.CollectionItems.CREATED_AT, created_at.getTime());
        return cv;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destination) {
        if (owner != null) owner.putFullContentValues(destination);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(associationType);
        dest.writeLong(created_at.getTime());
        dest.writeParcelable(owner, 0);
    }

    // Associations are different from the other models in that they don't have IDs
    // when inserted, but are defined over the resource they refer to.
    @Override
    public Uri toUri() {
        throw new UnsupportedOperationException();
    }

    @JsonProperty("type")
    public void setType(String type) {
        for (Type t : Type.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                associationType = t.collectionType;
                break;
            }
        }
    }

    public void setType(Type type){
        associationType = type.collectionType;
    }

    @Override
    public CharSequence getTimeSinceCreated(Context context) {
        if (mElapsedTime == null) {
            mElapsedTime = ScTextUtils.getTimeElapsed(context.getResources(), created_at.getTime());
        }
        return mElapsedTime;
    }

    @Override
    public void refreshTimeSinceCreated(Context context) {
        mElapsedTime = null;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.COLLECTION_ITEMS.uri;
    }

    protected abstract int getResourceType();

    protected abstract long getItemId();

    @Override
    public abstract long getListItemId();

    @Override
    public boolean isStale() {
        return getRefreshableResource().isStale();
    }

    @Override
    public boolean isIncomplete() {
        return getRefreshableResource().isIncomplete();
    }
}
