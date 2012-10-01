package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import java.util.Date;
import java.util.List;

public class Playlist extends Track {
    @JsonView(Views.Full.class) public String tracks_uri;
    @JsonView(Views.Full.class) public List<Track> tracks;

    /*
    @JsonView(Views.Mini.class) public String title;
    @JsonView(Views.Mini.class) public User user;
    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) public long user_id;
    @JsonView(Views.Mini.class) public String artwork_url;
    @JsonView(Views.Mini.class) public String permalink;
    @JsonView(Views.Mini.class) public String permalink_url;

    @JsonView(Views.Full.class) public int duration = NOT_SET;
    @JsonView(Views.Full.class) public Date created_at;

    @JsonView(Views.Full.class) public boolean streamable;
    @JsonView(Views.Full.class) public boolean downloadable;
    @JsonView(Views.Full.class) public String license;
    @JsonView(Views.Full.class) public Integer label_id;
    @JsonView(Views.Full.class) public String purchase_url;
    @JsonView(Views.Full.class) public String label_name;
    @JsonView(Views.Full.class) public String ean;
    @JsonView(Views.Full.class) public String release;
    @JsonView(Views.Full.class) public String description;

    @JsonView(Views.Full.class) public String genre;
    @JsonView(Views.Full.class) public String playlist_type;
    @JsonView(Views.Full.class) public String type;

    @JsonView(Views.Full.class) public Integer release_day;
    @JsonView(Views.Full.class) public Integer release_year;
    @JsonView(Views.Full.class) public Integer release_month;

    @JsonView(Views.Full.class) public String purchase_title;
    @JsonView(Views.Full.class) public String embeddable_by;

    @JsonView(Views.Full.class) public int likes_count;
    @JsonView(Views.Full.class) public int reposts_count;
    @JsonView(Views.Full.class) public String tag_list;
    @JsonView(Views.Full.class) public Sharing sharing;  //  public | private
    */

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", title='" + title + "'" +
                ", permalink_url='" + permalink_url + "'" +
                ", duration=" + duration +
                ", state=" + state +
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
}
