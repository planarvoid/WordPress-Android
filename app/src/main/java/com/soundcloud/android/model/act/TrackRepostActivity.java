package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.RepostActivity;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import org.jetbrains.annotations.NotNull;

import android.database.Cursor;

public class TrackRepostActivity extends TrackActivity implements PlayableHolder, RepostActivity {
    @JsonProperty public User user;

    // for deserialization
    public TrackRepostActivity() {
        super();
    }

    public TrackRepostActivity(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.MODEL_MANAGER.getUserFromActivityCursor(cursor);
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
