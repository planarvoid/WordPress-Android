package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Deprecated
public class PublicApiPlaylist extends Playable implements PlaylistRecord {

    public static final String EXTRA = "com.soundcloud.android.playlist";
    public static final Creator<PublicApiPlaylist> CREATOR = new Creator<PublicApiPlaylist>() {
        @Override
        public PublicApiPlaylist createFromParcel(Parcel source) {
            return new PublicApiPlaylist(source);
        }

        @Override
        public PublicApiPlaylist[] newArray(int size) {
            return new PublicApiPlaylist[size];
        }
    };

    @JsonView(Views.Full.class) public String playlist_type;
    @JsonView(Views.Full.class) public String tracks_uri;
    @JsonView(Views.Full.class) public List<PublicApiTrack> tracks = new ArrayList<PublicApiTrack>(0);
    @JsonView(Views.Full.class) private int track_count;
    public boolean removed;

    public PublicApiPlaylist(ApiPlaylist playlist) {
        setUrn(playlist.getUrn().toString());
        setUser(new PublicApiUser(playlist.getUser()));
        setTitle(playlist.getTitle());
        artwork_url = playlist.getArtworkUrl();
        tag_list = playlist.getTags() == null ? ScTextUtils.EMPTY_STRING : TextUtils.join(" ", playlist.getTags());
        created_at = playlist.getCreatedAt();
        duration = playlist.getDuration();
        track_count = playlist.getTrackCount();
        sharing = Sharing.from(playlist.isPublic());
        PlayableStats stats = playlist.getStats();
        if (stats != null) {
            likes_count = stats.getLikesCount();
            reposts_count = stats.getRepostsCount();
        }
    }

    /**
     * Helper to instantiate a playlist the given user created locally. This playlist will have a negative timestamp
     * to indicate that it hasn't been synced to the API yet.
     */
    public static PublicApiPlaylist newUserPlaylist(PublicApiUser user, String title, boolean isPrivate, @NotNull List<PublicApiTrack> tracks) {
        PublicApiPlaylist playlist = new PublicApiPlaylist(-System.currentTimeMillis());
        playlist.user = user;
        playlist.title = title;
        playlist.sharing = isPrivate ? Sharing.PRIVATE : Sharing.PUBLIC;
        playlist.created_at = new Date();
        playlist.tracks = tracks;
        playlist.track_count = tracks.size();
        return playlist;
    }

    public PublicApiPlaylist() {
        super();
    }

    public PublicApiPlaylist(long id) {
        super(id);
    }

    public PublicApiPlaylist(Parcel in) {
        Bundle b = in.readBundle(getClass().getClassLoader());
        super.readFromBundle(b);

        playlist_type = b.getString("playlist_type");
        tracks_uri = b.getString("tracks_uri");
        track_count = b.getInt("track_count");
        tracks = b.getParcelableArrayList("tracks");
        if (tracks == null) {
            tracks = new LinkedList<>();
        }
    }

    public PublicApiPlaylist(Cursor cursor) {
        super(cursor);
        tracks_uri = cursor.getString(cursor.getColumnIndex(TableColumns.Sounds.TRACKS_URI));
        track_count = cursor.getInt(cursor.getColumnIndex(TableColumns.Sounds.TRACK_COUNT));
    }

    @Override
    public void setId(long id) {
        super.setId(id);
        urn = Urn.forPlaylist(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle b = super.getBundle();
        b.putString("playlist_type", playlist_type);
        b.putString("tracks_uri", tracks_uri);
        b.putInt("track_count", track_count);
        b.putParcelableArrayList("tracks", (ArrayList<? extends Parcelable>) tracks);
        dest.writeBundle(b);
    }

    public PublicApiPlaylist updateFrom(PublicApiPlaylist updatedItem, CacheUpdateMode cacheUpdateMode) {
        super.updateFrom(updatedItem, cacheUpdateMode);
        tracks_uri = updatedItem.tracks_uri;
        track_count = updatedItem.track_count;
        playlist_type = updatedItem.playlist_type;
        last_updated = updatedItem.last_updated;

        if (cacheUpdateMode == CacheUpdateMode.FULL) {
            tracks = updatedItem.tracks;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + getId() +
                ", title='" + title + "'" +
                ", permalink_url='" + permalink_url + "'" +
                ", duration=" + duration +
                ", user=" + user +
                ", track_count=" + (track_count == -1 ? tracks.size() : track_count) +
                ", tracks_uri='" + tracks_uri + '\'' +
                '}';
    }

    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(TableColumns.Sounds.TRACKS_URI, tracks_uri);
        cv.put(TableColumns.Sounds.TRACK_COUNT, track_count);
        if (!isIncomplete()){
            cv.put(TableColumns.Sounds.LAST_UPDATED, System.currentTimeMillis());
        }
        return cv;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.PLAYLISTS.uri;
    }

    @Override
    public Refreshable getRefreshableResource() {
        return this;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destMap) {
        super.putDependencyValues(destMap);
        int i = 0;
        for (PublicApiTrack t : tracks) {
            t.putFullContentValues(destMap);

            // add to relationship table
            ContentValues cv = new ContentValues();
            cv.put(TableColumns.PlaylistTracks.TRACK_ID, t.getId());
            cv.put(TableColumns.PlaylistTracks.POSITION, i);
            destMap.add(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(getId())), cv);
            i++;
        }
    }

    @Override
    public Uri toUri() {
        return Content.PLAYLISTS.forQuery(String.valueOf(getId()));
    }

    public List<PublicApiTrack> getTracks() {
        return tracks;
    }

    @Override
    public int getTypeId() {
        return DB_TYPE_PLAYLIST;
    }

    @Override
    public Intent getViewIntent() {
        return PlaylistDetailActivity.getIntent(getUrn(), Screen.DEEPLINK);
    }

    @Override
    public boolean isStale() {
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.PLAYLIST;
    }


    @Override
    public int getDuration() {
        return duration;
    }

    public int getTrackCount() {
        return Math.max(tracks.size(), track_count);
    }

    @Override
    public Iterable<String> getTags() {
        return humanTags();
    }

    @Override
    public String getPermalinkUrl() {
        return permalink_url;
    }

    @Override
    public int getLikesCount() {
        return likes_count;
    }

    @Override
    public int getRepostsCount() {
        return reposts_count;
    }

    @JsonProperty("track_count")
    public void setTrackCount(int count) {
        track_count = count;
    }

    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(PlaylistProperty.TRACK_COUNT, track_count)
                .put(PlayableProperty.LIKES_COUNT, likes_count)
                .put(PlayableProperty.IS_LIKED, user_like);
    }

}
