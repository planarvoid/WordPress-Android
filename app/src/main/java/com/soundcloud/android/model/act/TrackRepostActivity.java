package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class TrackRepostActivity extends Activity implements Playable {
    @JsonProperty public Track track;
    @JsonProperty public User user;

    // for deserialization
    public TrackRepostActivity() {
        super();
    }

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

    @Override
    public ScResource getRefreshableResource() {
        return track;
    }
}
