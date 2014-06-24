package com.soundcloud.android.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ActivityProperty;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.behavior.Repost;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.database.Cursor;

public class TrackRepostActivity extends TrackActivity implements PlayableHolder, Repost {
    @JsonProperty public User user;

    // for deserialization
    public TrackRepostActivity() {
        super();
    }

    public TrackRepostActivity(Cursor cursor) {
        super(cursor);
        user = SoundCloudApplication.sModelManager.getCachedUserFromActivityCursor(cursor);
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
        this.user = SoundCloudApplication.sModelManager.cache(user, ScResource.CacheUpdateMode.MINI);
    }

    @NotNull
    @Override
    public User getReposter() {
        return user;
    }

    @Override
    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(ActivityProperty.TYPE, ActivityProperty.TYPE_REPOST)
                .put(ActivityProperty.SOUND_TITLE, track.getTitle());
    }
}
