package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.behavior.Creation;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToUser;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

public abstract class Association extends PublicApiResource implements PlayableHolder, Refreshable, RelatesToUser, Creation {

    @Nullable public PublicApiUser owner;
    public int associationType;
    public Date created_at;
    protected CharSequence elapsedTime;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public Association() {
    }

    public Association(Cursor cursor) {
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
        if (owner != null) {
            owner.putFullContentValues(destination);
        }
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

    public void setType(Type type) {
        associationType = type.collectionType;
    }

    @Override
    public void refreshTimeSinceCreated(Context context) {
        elapsedTime = null;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.UNKNOWN.uri; // not used
    }

    @Override
    public abstract long getListItemId();

    @Override
    public boolean isStale() {
        final Refreshable refreshableResource = getRefreshableResource();
        if (refreshableResource == null) {
            return true;
        }
        return refreshableResource.isStale();
    }

    @Override
    public boolean isIncomplete() {
        final Refreshable refreshableResource = getRefreshableResource();
        if (refreshableResource == null) {
            return true;
        }
        return refreshableResource.isIncomplete();
    }

    protected abstract int getResourceType();

    protected abstract long getItemId();

    public enum Type {
        TRACK(ScContentProvider.CollectionItemTypes.TRACK),
        TRACK_REPOST(ScContentProvider.CollectionItemTypes.REPOST),
        TRACK_LIKE(ScContentProvider.CollectionItemTypes.LIKE),
        PLAYLIST(ScContentProvider.CollectionItemTypes.PLAYLIST),
        PLAYLIST_REPOST(ScContentProvider.CollectionItemTypes.REPOST),
        PLAYLIST_LIKE(ScContentProvider.CollectionItemTypes.LIKE),
        FOLLOWING(ScContentProvider.CollectionItemTypes.FOLLOWING),
        FOLLOWER(ScContentProvider.CollectionItemTypes.FOLLOWER);
        public final int collectionType;

        Type(int collectionType) {
            this.collectionType = collectionType;
        }
    }
}
