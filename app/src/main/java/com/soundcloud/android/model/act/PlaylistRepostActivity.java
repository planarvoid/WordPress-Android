package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.behavior.Repost;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import org.jetbrains.annotations.NotNull;

import android.database.Cursor;

public class PlaylistRepostActivity extends PlaylistActivity implements PlayableHolder, Repost {
    @JsonProperty public User user;

    // for deserialization
    public PlaylistRepostActivity() {
        super();
    }

    public PlaylistRepostActivity(Cursor c) {
        super(c);
        user = SoundCloudApplication.MODEL_MANAGER.getCachedUserFromActivityCursor(c);
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
    public void cacheDependencies() {
        super.cacheDependencies();
        this.user = SoundCloudApplication.MODEL_MANAGER.cache(user, ScResource.CacheUpdateMode.MINI);
    }

    @NotNull
    @Override
    public User getReposter() {
        return user;
    }
}
