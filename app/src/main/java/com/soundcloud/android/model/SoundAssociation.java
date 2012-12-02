package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

/**
 * Maps to stream item on backend
 */
public class SoundAssociation extends ScResource implements Playable, Refreshable {

    private CharSequence _elapsedTime;


    enum Type {
        TRACK("track", ScContentProvider.CollectionItemTypes.TRACK),
        TRACK_REPOST("track_repost", ScContentProvider.CollectionItemTypes.REPOST),
        TRACK_LIKE("track_like", ScContentProvider.CollectionItemTypes.LIKE),
        PLAYLIST("playlist", ScContentProvider.CollectionItemTypes.PLAYLIST),
        PLAYLIST_REPOST("playlist_repost", ScContentProvider.CollectionItemTypes.REPOST),
        PLAYLIST_LIKE("playlist_like", ScContentProvider.CollectionItemTypes.LIKE);

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

    public @Nullable Track track;
    public @Nullable Playlist playlist;
    public @Nullable User user;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public SoundAssociation() {
    }

    public SoundAssociation(Cursor cursor) {
        associationType = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TYPE));
        created_at = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP)));

        switch (associationType) {
            case ScContentProvider.CollectionItemTypes.REPOST:
            case ScContentProvider.CollectionItemTypes.TRACK:
            case ScContentProvider.CollectionItemTypes.LIKE:
                track = new Track(cursor);
        }
    }

    @Override
    public ScResource getRefreshableResource() {
        return track; // TODO, playlist
    }

    @Override
    public boolean isStale() {
        return track != null && track.isStale(); // TODO, playlist
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
        cv.put(DBHelper.CollectionItems.CREATED_AT, created_at.getTime());
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
        return playlist != null ? playlist.getTypeId() : track.getTypeId();
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

    @Override @Nullable
    public Track getTrack() {
        return track;
    }

    @Nullable
    public Playlist getPlaylist() {
        return playlist;
    }

    @Override
    public CharSequence getTimeSinceCreated(Context context) {
        if (_elapsedTime == null) {
            _elapsedTime = ScTextUtils.getTimeElapsed(context.getResources(), created_at.getTime());
        }
        return _elapsedTime;
    }

    @Override
    public void refreshTimeSinceCreated(Context context) {
        _elapsedTime = null;
    }

    @Override
    public String toString() {
        return "SoundAssociation{" +
                "associationType=" + associationType +
                ", type='" + type + '\'' +
                ", created_at=" + created_at +
                ", track=" + track +
                ", playlist=" + playlist +
                ", user=" + user +
                '}';
    }
}
