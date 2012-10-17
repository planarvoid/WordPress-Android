package com.soundcloud.android.model.Activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

import java.util.List;

public class TrackLikeActivity extends Activity implements Playable {
    public User user;
    public Track track;

    // for deserialization
    public TrackLikeActivity() {
    }

    public TrackLikeActivity(Cursor cursor) {
        super(cursor);
    }

    @Override
    public Type getType() {
        return Type.TRACK_LIKE;
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Playlist getPlaylist() {
        return null;
    }

    @JsonIgnore
    @Override
    public void setCachedTrack(Track track) {
        this.track = track;
    }

    @JsonIgnore
    @Override
    public void setCachedUser(User user) {
        this.user = user;
    }
}
