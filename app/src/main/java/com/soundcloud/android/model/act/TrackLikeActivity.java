package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class TrackLikeActivity extends TrackActivity implements PlayableHolder {
    @JsonProperty public User user;

    // for deserialization
    public TrackLikeActivity() {
        super();
    }

    public TrackLikeActivity(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.MODEL_MANAGER.getUserFromActivityCursor(cursor);
    }

    @Override
    public Type getType() {
        return Type.TRACK_LIKE;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void cacheDependencies() {
        super.cacheDependencies();
        this.user = SoundCloudApplication.MODEL_MANAGER.cache(user, ScResource.CacheUpdateMode.MINI);
    }
}
