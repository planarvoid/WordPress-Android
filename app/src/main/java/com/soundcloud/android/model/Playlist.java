package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Playlist extends PlayableResource {
    @JsonView(Views.Full.class) public String playlist_type;
    @JsonView(Views.Full.class) public String tracks_uri;
    @JsonView(Views.Full.class) public List<Track> tracks;

    public Playlist() {
        super();
    }

    public Playlist(Parcel in) {
        Bundle b = in.readBundle();
        super.readFromBundle(b);

        playlist_type = b.getString("playlist_type");
        tracks_uri = b.getString("tracks_uri");
        tracks = b.getParcelableArrayList("tracks");
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
        b.putParcelableArrayList("tracks", (ArrayList<Track>) tracks);
        dest.writeBundle(b);
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", title='" + title + "'" +
                ", permalink_url='" + permalink_url + "'" +
                ", duration=" + duration +
                ", user=" + user +
                ", track_count=" + (tracks != null ? tracks.size() : "0") +
                ", tracks_uri='" + tracks_uri + '\'' +
                '}';
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.PLAYLISTS.uri;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
    }

    @Override @JsonIgnore
    public Track getTrack() {
        return null;
    }

    @Override
    public Date getCreatedAt() {
        return null;
    }

    @Override
    public String getArtwork() {
        return null;
    }

    @Override
    public long getRefreshableId() {
        return 0;
    }

    @Override
    public ScResource getRefreshableResource() {
        return null;
    }

    @Override
    public boolean isStale() {
        return false;
    }


}
