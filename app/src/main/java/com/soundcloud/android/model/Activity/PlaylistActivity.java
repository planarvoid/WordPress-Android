package com.soundcloud.android.model.Activity;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SharingNote;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class PlaylistActivity extends Activity {
    public Playlist playlist;
    public SharingNote sharingNote;
    public User user;

    // for deserialization
    public PlaylistActivity(){

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
    public void setCachedTrack(Track track) {
    }

    @Override
    public void setCachedUser(User user) {
    }

}
