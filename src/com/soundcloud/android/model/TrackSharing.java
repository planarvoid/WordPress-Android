package com.soundcloud.android.model;

import com.soundcloud.android.json.Views;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonView;

import java.util.Date;

public class TrackSharing implements Origin {
    @JsonView(Views.Mini.class) public Track track;
    @JsonView(Views.Mini.class) public SharingNote sharing_note;

    static class SharingNote {
        public String text;
        public Date created_at;
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public User getUser() {
        return track.user;
    }
}
