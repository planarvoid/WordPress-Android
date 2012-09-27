package com.soundcloud.android.model.Activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class TrackActivity extends Activity {
    public Track track;

    // for deserialization
    public TrackActivity() { }

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

    @JsonIgnore @Override
    public void setCachedTrack(Track track) {
        this.track = track;
    }

    @JsonIgnore @Override
    public void setCachedUser(User user) {
        // nop
    }
}
