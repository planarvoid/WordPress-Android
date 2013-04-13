package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

/**
 * Maps to <code>stream_item</code> item on backend.
 */
public class SoundAssociation extends ScResource implements PlayableHolder, Refreshable {

    private CharSequence _elapsedTime;
    public enum Type {
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

    public @NotNull Playable playable;
    public @Nullable User user;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public SoundAssociation() { }

    public SoundAssociation(Cursor cursor) {
        associationType = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TYPE));
        created_at = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TIMESTAMP)));
        user = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromCursor(cursor, DBHelper.SoundAssociationView.SOUND_ASSOCIATION_USER_ID);
        playable = Playable.fromCursor(cursor);
    }

    /**
     * Use this ctor to create sound associations for likes and reposts of playlists and tracks.
     * @param playable the track or playlist that was reposted or liked
     * @param typeEnum the kind of association
     */
    public SoundAssociation(@NotNull Playable playable, Date associatedAt, Type typeEnum) {
        this.playable = playable;
        this.created_at = associatedAt;
        this.associationType = typeEnum.collectionType;
    }

    /**
     * Creates a sound association for a track the user has created.
     */
    public SoundAssociation(Track track) {
        this(track, track.created_at, Type.TRACK);
    }

    /**
     * Creates a sound association for a playlist the user has created.
     */
    public SoundAssociation(Playlist playlist) {
        this(playlist, playlist.created_at, Type.PLAYLIST);
    }

    @Override
    public long getListItemId() {
        return getPlayable().id << 32 + associationType;
    }

    @Override
    public ScResource getRefreshableResource() {
        return playable;
    }


    @Override
    public boolean isStale() {
        return playable.isStale();
    }

    @Override
    public boolean isIncomplete() {
        return playable.isIncomplete();
    }

    public SoundAssociation(Parcel in) {
        associationType = in.readInt();
        created_at = new Date(in.readLong());
        playable = in.readParcelable(ClassLoader.getSystemClassLoader());
        user = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.CollectionItems.ITEM_ID, getPlayable().id);
        cv.put(DBHelper.CollectionItems.USER_ID, SoundCloudApplication.getUserId());
        cv.put(DBHelper.CollectionItems.COLLECTION_TYPE, associationType);
        cv.put(DBHelper.CollectionItems.RESOURCE_TYPE, getResourceType());
        cv.put(DBHelper.CollectionItems.CREATED_AT, created_at.getTime());
        return cv;
    }

    @JsonProperty("playlist")
    public void setPlaylist(Playlist playlist) {
        // check for null as it will try to set a null value on deserialization
        if (playlist != null) playable = playlist;
    }

    @JsonProperty("track")
    public void setTrack(Track track) {
        // check for null as it will try to set a null value on deserialization
        if (track != null) playable = track;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(associationType);
        dest.writeLong(created_at.getTime());
        dest.writeParcelable(playable, 0);
        dest.writeParcelable(user, 0);
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.COLLECTION_ITEMS.uri;
    }

    @Override
    public User getUser() {
        return playable.user;
    }

    @Override
    @NotNull public Playable getPlayable() {
        return playable;
    }

    public long getItemId() {
        return playable.getId();
    }

    public int getResourceType() {
        return playable.getTypeId();
    }

    @Override
    public void putDependencyValues(BulkInsertMap destination) {
        playable.putFullContentValues(destination);
        if (user != null)       user.putFullContentValues(destination);
    }

    // SoundAssociation is different from the other models in that they don't have IDs
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
                ", playable=" + playable +
                ", user=" + user +
                '}';
    }
}
