package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class PlaylistLikeActivity extends PlaylistActivity implements PlayableHolder {
    @JsonProperty public User user;

    public PlaylistLikeActivity() {
        super();
    }

    public PlaylistLikeActivity(Cursor cursor) {
        super(cursor);
        user = User.fromActivityView(cursor);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST_LIKE;
    }

    @Override
    public User getUser() {
        return user;
    }
}
