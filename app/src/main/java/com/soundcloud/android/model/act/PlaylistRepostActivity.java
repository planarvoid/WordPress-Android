package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class PlaylistRepostActivity extends PlaylistActivity implements PlayableHolder {
    @JsonProperty public User user;

    // for deserialization
    public PlaylistRepostActivity() {
        super();
    }

    public PlaylistRepostActivity(Cursor c) {
        super(c);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST_REPOST;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }
}
