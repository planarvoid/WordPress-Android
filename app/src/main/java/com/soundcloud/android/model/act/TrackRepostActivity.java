package com.soundcloud.android.model.Activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

import java.util.List;

public class TrackRepostActivity extends Activity implements Playable {
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
        return track.user;
    }

    @Override
    public Playlist getPlaylist() {
        return null;
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
