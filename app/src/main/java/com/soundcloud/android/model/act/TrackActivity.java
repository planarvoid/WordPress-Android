package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;
import android.os.Parcel;

public class TrackActivity extends Activity implements PlayableHolder {
    @JsonProperty public Track track;

    // for deserialization
    public TrackActivity() {
        super();
    }

    public TrackActivity(Cursor c) {
        super(c);
    }

    public TrackActivity(Parcel in) {
        super(in);
        track = in.readParcelable(Track.class.getClassLoader());
    }

    @Override
    public Type getType() {
        return Type.TRACK;
    }

    @Override
    public Playable getPlayable() {
        return track;
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
    public void setTrack(Track track) {
        this.track = track;
    }

    @Override
    public void setPlaylist(Playlist playlist) {
        // nop
    }

    @JsonIgnore @Override
    public void setUser(User user) {
        // nop
    }

    @Override
    public ScResource getRefreshableResource() {
        return track;
    }
}
