package com.soundcloud.android.model;

import com.soundcloud.android.json.Views;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Favoriting implements Origin {
    @JsonProperty @JsonView(Views.Mini.class) public Track track;
    @JsonProperty @JsonView(Views.Mini.class) public User user;

    @Override @JsonIgnore
    public Track getTrack() {
        return track;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
    }
}
