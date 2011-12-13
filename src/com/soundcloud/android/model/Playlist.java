package com.soundcloud.android.model;

import com.soundcloud.android.json.Views;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonView;

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
