package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Collection;
import java.util.Date;

/**
 * Maps to stream item on backend
 */
public class SoundAssociation extends ScResource {

    public static final String TRACK_REPOST = "track_repost";
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
        cv.put(DBHelper.CollectionItems.POSITION,  created_at.getTime());
        return cv;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(associationType);
        dest.writeLong(created_at.getTime());
        dest.writeParcelable(track,0);
        dest.writeParcelable(playlist,0);
        dest.writeParcelable(user,0);
    }


    @Override
    public Uri getBulkInsertUri() {
        return null;
    }

    @Override
    public User getUser() {
        return track != null ? track.user : (playlist != null ? playlist.user: null);
    }

    @Override
    public Sound getSound() {
        return track != null ? track : playlist;
    }

    public int getResourceType(){
        return playlist != null ? Sound.DB_TYPE_PLAYLIST : Sound.DB_TYPE_TRACK;
    }

    @JsonProperty("type")
    public void setType(String type){
        if (type.equalsIgnoreCase("track")) {
            associationType = ScContentProvider.CollectionItemTypes.TRACK;
        } else if (type.equalsIgnoreCase(TRACK_REPOST)) {
            associationType = ScContentProvider.CollectionItemTypes.TRACK_REPOST;
        } else if (type.equalsIgnoreCase("track_like")) {
            associationType = ScContentProvider.CollectionItemTypes.TRACK_LIKE;
        } else if (type.equalsIgnoreCase("playlist")) {
            associationType = ScContentProvider.CollectionItemTypes.PLAYLIST;
        } else if (type.equalsIgnoreCase("playlist_repost")) {
            associationType = ScContentProvider.CollectionItemTypes.PLAYLIST_REPOST;
        } else if (type.equalsIgnoreCase("playlist_like")) {
            associationType = ScContentProvider.CollectionItemTypes.PLAYLIST_LIKE;
        }
        this.type = type;
    }


}
