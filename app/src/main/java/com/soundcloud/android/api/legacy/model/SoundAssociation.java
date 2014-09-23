package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import java.util.Date;

/**
 * Maps to <code>stream_item</code> item on backend.
 */
public class SoundAssociation extends Association implements PlayableHolder {

    public @Nullable Playable playable;

    @SuppressWarnings("UnusedDeclaration") //for deserialization
    public SoundAssociation() { }

    public SoundAssociation(Cursor cursor) {
        super(cursor);
        // single instance considerations
        if (Playable.isTrackCursor(cursor)){
            playable = SoundCloudApplication.sModelManager.getCachedTrackFromCursor(cursor, TableColumns.SoundAssociationView._ID);
        } else {
            playable = SoundCloudApplication.sModelManager.getCachedPlaylistFromCursor(cursor, TableColumns.SoundAssociationView._ID);
        }
    }

    /**
     * Use this ctor to create sound associations for likes and reposts of playlists and tracks.
     * @param playable the track or playlist that was reposted or liked
     * @param typeEnum the kind of association
     */
    public SoundAssociation(@NotNull Playable playable, Date associatedAt, Type typeEnum) {
        super(typeEnum.collectionType, associatedAt);
        this.playable = playable;
    }

    /**
     * Creates a sound association for a track the user has created.
     */
    public SoundAssociation(PublicApiTrack track) {
        this(track, track.created_at, Type.TRACK);
    }

    /**
     * Creates a sound association for a playlist the user has created.
     */
    public SoundAssociation(PublicApiPlaylist playlist) {
        this(playlist, playlist.created_at, Type.PLAYLIST);
    }

    @Override
    public long getListItemId() {
        return getPlayable().getId() << 32 + associationType;
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
    public void setPlaylist(PublicApiPlaylist playlist) {
        // check for null as it will try to set a null value on deserialization
        if (playlist != null) {
            playable = playlist;
        }
    }

    @JsonProperty("track")
    public void setTrack(PublicApiTrack track) {
        // check for null as it will try to set a null value on deserialization
        if (track != null) {
            playable = track;
        }
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
    public PublicApiUser getUser() {
        return playable.user;
    }

    @Nullable
    @Override
    public Playable getPlayable() {
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
            mElapsedTime = ScTextUtils.formatTimeElapsed(context.getResources(), created_at.getTime());
        }
        return mElapsedTime;
    }

    @Override
    public void refreshTimeSinceCreated(Context context) {
        mElapsedTime = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SoundAssociation)) return false;
        if (!super.equals(o)) return false;

        SoundAssociation that = (SoundAssociation) o;

        if (playable != null ? !playable.equals(that.playable) : that.playable != null) return false;
        if (associationType != that.associationType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (playable != null ? playable.hashCode() : 0);
        result = 31 * result + associationType;
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
