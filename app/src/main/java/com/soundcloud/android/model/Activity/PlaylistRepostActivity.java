package com.soundcloud.android.model.Activity;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

public class PlaylistRepostActivity extends Activity{

    @Override
    public Type getType() {
        return Type.PLAYLIST_REPOST;
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
