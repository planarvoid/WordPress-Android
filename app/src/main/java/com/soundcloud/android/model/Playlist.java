package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;

import android.net.Uri;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Playlist extends ScResource {
    @JsonView(Views.Mini.class) public User user;

    @Override
    public Uri getBulkInsertUri() {
        return Content.PLAYLISTS.uri;
    }

    @Override @JsonIgnore
    public User getUser() {
        return null;
    }

    @Override @JsonIgnore
    public Track getTrack() {
        return null;
    }
}
