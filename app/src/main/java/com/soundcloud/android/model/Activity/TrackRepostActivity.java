package com.soundcloud.android.model.Activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class TrackRepostActivity extends Activity {
    public Track track;
    public User user;

    // for deserialization
    public TrackRepostActivity() { }

    public TrackRepostActivity(Cursor cursor) {
        super(cursor);
    }

    @Override
    public Type getType() {
        return Type.TRACK_REPOST;
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override @JsonIgnore
    public void setCachedTrack(Track track) {
        this.track = track;
    }

    @Override @JsonIgnore
    public void setCachedUser(User user) {
        this.user = user;
    }
}
