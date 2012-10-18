package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SharingNote;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class PlaylistActivity extends Activity {
    @JsonProperty public Playlist playlist;
    @JsonProperty public SharingNote sharingNote;
    @JsonProperty public User user;

    // for deserialization
    public PlaylistActivity() {
        super();
    }

    public PlaylistActivity(Cursor c) {
        super(c);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST;
    }

    @Override
    public Track getTrack() {
        return null;
    }

    @Override
    public User getUser() {
        return null;
    }

    @Override
    public Playlist getPlaylist() {
        return playlist;
    }

    @Override
    public void setCachedTrack(Track track) {
    }

    @Override
    public void setCachedUser(User user) {
    }


}
