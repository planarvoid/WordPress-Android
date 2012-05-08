package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Playlist extends ScModel implements Origin {
    @JsonView(Views.Mini.class) public User user;

    @Override @JsonIgnore
    public Track getTrack() {
        return null;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
    }
}
