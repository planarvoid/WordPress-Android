package com.soundcloud.android.model.Activity;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

public class PlaylistActivity extends Activity {
    public Playlist playlist;
    public User user;

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
