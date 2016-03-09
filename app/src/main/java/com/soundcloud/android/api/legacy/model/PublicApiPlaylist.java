package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrackStats;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.java.collections.PropertySet;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
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
    @JsonView(Views.Full.class) public List<PublicApiTrack> tracks = new ArrayList<>(0);
    @JsonView(Views.Full.class) private int track_count;
    public boolean removed;

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

    @Override
    public long getDuration() {
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
                .put(PlaylistProperty.PLAYLIST_DURATION, duration)
                .put(PlaylistProperty.TRACK_COUNT, track_count)
                .put(PlayableProperty.LIKES_COUNT, likes_count)
                .put(PlayableProperty.IS_USER_LIKE, user_like);
    }

    public ApiPlaylist toApiMobilePlaylist() {
        ApiPlaylist apiPlaylist = new ApiPlaylist();
        apiPlaylist.setUrn(getUrn());
        apiPlaylist.setCreatedAt(created_at);
        apiPlaylist.setArtworkUrl(artwork_url);
        apiPlaylist.setDuration(duration);
        apiPlaylist.setPermalinkUrl(permalink_url);
        apiPlaylist.setSharing(sharing);
        apiPlaylist.setTitle(title);
        apiPlaylist.setTags(humanTags());
        apiPlaylist.setTrackCount(track_count);
        apiPlaylist.setUser(getUser().toApiMobileUser());

        final ApiTrackStats stats = new ApiTrackStats();
        stats.setLikesCount(likes_count);
        stats.setRepostsCount(reposts_count);
        apiPlaylist.setStats(stats);

        return apiPlaylist;
    }
}
