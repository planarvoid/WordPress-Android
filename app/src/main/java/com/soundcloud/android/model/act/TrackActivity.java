package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class TrackActivity extends Activity implements Playable {
    @JsonProperty public Track track;

    // for deserialization
    public TrackActivity() {
        super();
    }

    public TrackActivity(Cursor c) {
        super(c);
    }


    @Override
    public Type getType() {
        return Type.TRACK;
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public User getUser() {
        return track.user;
    }

    @Override
    public Playlist getPlaylist() {
        return null;
    }

    @JsonIgnore @Override
    public void setCachedTrack(Track track) {
        this.track = track;
    }

    @JsonIgnore @Override
    public void setCachedUser(User user) {
        // nop
    }

    @Override
    public ScResource getRefreshableResource() {
        return track;
    }

    @Override
    public boolean isIncomplete() {
        return track == null || track.isIncomplete();
    }
}
