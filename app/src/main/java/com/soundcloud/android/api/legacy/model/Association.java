package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToUser;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

@Deprecated // remove after migrating affiliations to api-mobile
public abstract class Association extends PublicApiResource implements PlayableHolder, Refreshable, RelatesToUser {

    public int associationType;
    public Date created_at;
    protected CharSequence elapsedTime;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public Association() {
    }

    public Association(Cursor cursor) {
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
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(associationType);
        dest.writeLong(created_at.getTime());
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
