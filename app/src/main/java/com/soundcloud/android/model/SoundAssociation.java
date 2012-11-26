package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

/**
 * Maps to stream item on backend
 */
public class SoundAssociation extends ScResource {

    enum Type {
        TRACK("track", ScContentProvider.CollectionItemTypes.TRACK),
        TRACK_REPOST("track_repost", ScContentProvider.CollectionItemTypes.TRACK_REPOST),
        TRACK_LIKE("track_like", ScContentProvider.CollectionItemTypes.TRACK_LIKE),
        PLAYLIST("playlist", ScContentProvider.CollectionItemTypes.PLAYLIST),
        PLAYLIST_REPOST("playlist_repost", ScContentProvider.CollectionItemTypes.PLAYLIST_REPOST),
        PLAYLIST_LIKE("playlist_like", ScContentProvider.CollectionItemTypes.PLAYLIST_LIKE);

        Type(String type, int collectionType) {
            this.type = type;
            this.collectionType = collectionType;
        }

        public final String type;
        public final int collectionType;
    }

    public int associationType;
    public String type;
    public Date created_at;

    public Track track;
    public Playlist playlist;
    public User user;

    public SoundAssociation() { //for deserialization
    }

    public SoundAssociation(Cursor cursor) {
        associationType = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TYPE));
        created_at = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP)));
    }

    public SoundAssociation(Parcel in) {
        associationType = in.readInt();
        created_at = new Date(in.readLong());
        track = in.readParcelable(ClassLoader.getSystemClassLoader());
        playlist = in.readParcelable(ClassLoader.getSystemClassLoader());
        user = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CollectionItems.ITEM_ID, getSound().id);
        cv.put(DBHelper.CollectionItems.USER_ID, SoundCloudApplication.getUserId());
        cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, associationType);
        cv.put(DBHelper.CollectionItems.RESOURCE_TYPE, getResourceType());
        cv.put(DBHelper.CollectionItems.POSITION, created_at.getTime());
        return cv;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(associationType);
        dest.writeLong(created_at.getTime());
        dest.writeParcelable(track, 0);
        dest.writeParcelable(playlist, 0);
        dest.writeParcelable(user, 0);
    }


    @Override
    public Uri getBulkInsertUri() {
        return Content.COLLECTION_ITEMS.uri;
    }

    @Override
    public User getUser() {
        return track != null ? track.user : (playlist != null ? playlist.user : null);
    }

    @Override
    public Sound getSound() {
        return track != null ? track : playlist;
    }

    public int getResourceType() {
        return playlist != null ? Sound.DB_TYPE_PLAYLIST : Sound.DB_TYPE_TRACK;
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
}
