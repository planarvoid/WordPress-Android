package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

/**
 * Maps to <code>stream_item</code> item on backend.
 */
public class SoundAssociation extends Association implements PlayableHolder {

    public @NotNull Playable playable;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public SoundAssociation() { }

    public SoundAssociation(Cursor cursor) {
        super(cursor);
        // single instance considerations
        if (Playable.isTrackCursor(cursor)){
            playable = SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor, DBHelper.SoundAssociationView._ID);
        } else {
            playable = SoundCloudApplication.MODEL_MANAGER.getCachedPlaylistFromCursor(cursor, DBHelper.SoundAssociationView._ID);
        }
    }

    /**
     * Use this ctor to create sound associations for likes and reposts of playlists and tracks.
     * @param playable the track or playlist that was reposted or liked
     * @param typeEnum the kind of association
     */
    public SoundAssociation(@NotNull Playable playable, Date associatedAt, Type typeEnum) {
        super(associatedAt, typeEnum.collectionType);
        this.playable = playable;
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
    public Refreshable getRefreshableResource() {
        return playable;
    }

    public SoundAssociation(Parcel in) {
        super(in);
        playable = in.readParcelable(ClassLoader.getSystemClassLoader());
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
        super.writeToParcel(dest, flags);
        dest.writeParcelable(playable, 0);
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

    @Override
    public long getItemId() {
        return playable.getId();
    }

    @Override
    public int getResourceType() {
        return playable.getTypeId();
    }

    @Override
    public void putDependencyValues(BulkInsertMap destination) {
        super.putDependencyValues(destination);
        playable.putFullContentValues(destination);
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
    public boolean equals(Object o) {
        if (super.equals(o)) {
            SoundAssociation that = (SoundAssociation) o;
            return playable.equals(that.playable)
                    && playable.getClass().equals(that.playable.getClass())
                    && associationType == that.associationType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + associationType;
        result = 31 * result + playable.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SoundAssociation{" +
                "associationType=" + associationType +
                ", created_at=" + created_at +
                ", playable=" + playable +
                ", user=" + owner +
                '}';
    }
}
