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

public class TrackRepostActivity extends TrackActivity implements PlayableHolder {
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
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }
}
