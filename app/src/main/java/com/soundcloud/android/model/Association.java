package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import javax.annotation.Nullable;
import java.util.Date;

public abstract class Association extends ScResource implements Refreshable, Creation {

    public enum Type {
        TRACK("track", ScContentProvider.CollectionItemTypes.TRACK),
        TRACK_REPOST("track_repost", ScContentProvider.CollectionItemTypes.REPOST),
        TRACK_LIKE("track_like", ScContentProvider.CollectionItemTypes.LIKE),
        PLAYLIST("playlist", ScContentProvider.CollectionItemTypes.PLAYLIST),
        PLAYLIST_REPOST("playlist_repost", ScContentProvider.CollectionItemTypes.REPOST),
        PLAYLIST_LIKE("playlist_like", ScContentProvider.CollectionItemTypes.LIKE),
        FOLLOWING("following", ScContentProvider.CollectionItemTypes.FOLLOWING),
        FOLLOWER("follower", ScContentProvider.CollectionItemTypes.FOLLOWER);

        Type(String type, int collectionType) {
            this.type = type;
            this.collectionType = collectionType;
        }

        public final String type;
        public final int collectionType;
    }


    public @Nullable User   owner;
    public int              associationType;
    public String           type;
    public Date             created_at;

    protected CharSequence mElapsedTime;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public Association() {}

    public Association(Cursor cursor){
        associationType = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TYPE));
        created_at = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP)));
        owner = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromSoundViewCursor(cursor);
    }

    public Association(Date associatedAt, int associationType) {
        this.created_at = associatedAt;
        this.associationType = associationType;
    }

    public Association(Parcel in) {
        associationType = in.readInt();
        created_at = new Date(in.readLong());
        owner = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CollectionItems.ITEM_ID, getItemId());
        cv.put(DBHelper.CollectionItems.USER_ID, SoundCloudApplication.getUserId());
        cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, associationType);
        cv.put(DBHelper.CollectionItems.RESOURCE_TYPE, getResourceType());
        cv.put(DBHelper.CollectionItems.CREATED_AT, created_at.getTime());
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
            if (t.type.equalsIgnoreCase(type)) {
                associationType = t.collectionType;
            }
        }
        this.type = type;
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

    protected abstract int getResourceType();

    protected abstract long getItemId();

    @Override
    public abstract long getListItemId();

    @Override
    public abstract ScResource getRefreshableResource();

    @Override
    public abstract boolean isStale();

    @Override
    public abstract boolean isIncomplete();
}
