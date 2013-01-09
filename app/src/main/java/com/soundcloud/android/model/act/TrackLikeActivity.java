package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class TrackLikeActivity extends Activity implements PlayableHolder {
    @JsonProperty public User user;
    @JsonProperty public Track track;

    // for deserialization
    public TrackLikeActivity() {
        super();
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

    @Override
    public ScResource getRefreshableResource() {
        return track;
    }
}
